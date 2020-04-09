/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import m from "mithril";
import Stream from "mithril/stream";
import {FetchTaskAttributes} from "models/pipeline_configs/task";
import {Configurations} from "models/shared/configuration";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {ArtifactPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import {ExternalFetchArtifactView} from "views/pages/clicky_pipeline_config/tabs/job/tasks/fetch/external";
import {TestHelper} from "views/pages/spec/test_helper";

describe("External Fetch Artifact Task", () => {
  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  let attributes: FetchTaskAttributes;
  beforeEach(() => {
    attributes = new FetchTaskAttributes("gocd",
                                         "pipeline",
                                         "stage",
                                         "job",
                                         false,
                                         undefined,
                                         undefined,
                                         "id1",
                                         new Configurations([]),
                                         [],
                                         undefined);
  });

  it("should render external fetch task", () => {
    mount();
    expect(helper.byTestId("external-fetch-artifact-view")).toBeInDOM();
  });

  it("should render pipeline input", () => {
    mount();

    const pipelineHelpText = "The name of direct upstream pipeline or ancestor pipeline of one of the upstream pipelines on which the pipeline of the job depends on. The pipeline should be a dependency material or should be reachable as an ancestor(of the form fetch-from-pipeline/path/to/upstream-pipeline) of at-least one dependency material. Defaults to current pipeline if not specified.";

    expect(helper.byTestId("form-field-label-pipeline")).toContainText("Pipeline");
    expect(helper.byTestId("form-field-input-pipeline")).toBeInDOM();
    expect(helper.qa("span")).toContainText(pipelineHelpText);
  });

  it("should bind pipeline input to model", () => {
    mount();

    expect(attributes.pipeline()).toBe("pipeline");
    expect(helper.byTestId("form-field-input-pipeline")).toHaveValue("pipeline");

    helper.oninput(`[data-test-id="form-field-input-pipeline"]`, "new-pipeline");

    expect(attributes.pipeline()).toBe("new-pipeline");
    expect(helper.byTestId("form-field-input-pipeline")).toHaveValue("new-pipeline");
  });

  it("should render stage input", () => {
    mount();
    const stageHelpText = "The name of the stage to fetch artifacts from.";

    expect(helper.byTestId("form-field-label-stage")).toContainText("Stage");
    expect(helper.byTestId("form-field-input-stage")).toBeInDOM();
    expect(helper.qa("span")).toContainText(stageHelpText);
  });

  it("should bind stage input to model", () => {
    mount();

    expect(attributes.stage()).toBe("stage");
    expect(helper.byTestId("form-field-input-stage")).toHaveValue("stage");

    helper.oninput(`[data-test-id="form-field-input-stage"]`, "new-stage");

    expect(attributes.stage()).toBe("new-stage");
    expect(helper.byTestId("form-field-input-stage")).toHaveValue("new-stage");
  });

  it("should render job input", () => {
    mount();
    const jobHelpText = "The name of the job to fetch artifacts from.";

    expect(helper.byTestId("form-field-label-job")).toContainText("Job");
    expect(helper.byTestId("form-field-input-job")).toBeInDOM();
    expect(helper.qa("span")).toContainText(jobHelpText);
  });

  it("should bind job input to model", () => {
    mount();

    expect(attributes.job()).toBe("job");
    expect(helper.byTestId("form-field-input-job")).toHaveValue("job");

    helper.oninput(`[data-test-id="form-field-input-job"]`, "new-job");

    expect(attributes.job()).toBe("new-job");
    expect(helper.byTestId("form-field-input-job")).toHaveValue("new-job");
  });

  it("should render artifact id input", () => {
    mount();
    const artifactIdHelpText = "The id of the external artifact uploaded by the upstream job.";

    expect(helper.byTestId("form-field-label-artifact-id")).toContainText("Artifact Id");
    expect(helper.byTestId("form-field-input-artifact-id")).toBeInDOM();
    expect(helper.qa("span")).toContainText(artifactIdHelpText);
  });

  it("should bind artifact id input to model", () => {
    mount();

    expect(attributes.artifactId()).toBe("id1");
    expect(helper.byTestId("form-field-input-artifact-id")).toHaveValue("id1");

    helper.oninput(`[data-test-id="form-field-input-artifact-id"]`, "new-id1");

    expect(attributes.artifactId()).toBe("new-id1");
    expect(helper.byTestId("form-field-input-artifact-id")).toHaveValue("new-id1");
  });

  it("should render plugin id dropdown only when artifact id is configured", () => {
    mount();

    expect(attributes.artifactId()).toBeTruthy();

    expect(helper.byTestId("form-field-label-plugin-id")).toContainText("Plugin Id");
    expect(helper.byTestId("form-field-input-plugin-id")).toBeInDOM();
  });

  it("should not render plugin id dropdown when artifact id is not configured", () => {
    attributes.artifactId(undefined);
    mount();

    expect(attributes.artifactId()).toBeFalsy();

    expect(helper.byTestId("form-field-input-plugin-id")).not.toBeInDOM();
  });

  it("should auto select plugin id based on selected artifact id", () => {
    mount();

    expect(attributes.pipeline()).toEqual("pipeline");
    expect(attributes.stage()).toEqual("stage");
    expect(attributes.job()).toEqual("job");
    expect(attributes.artifactId()).toEqual("id1");

    const pluginIdDropdown = (helper.byTestId("form-field-input-plugin-id") as HTMLInputElement);
    expect(pluginIdDropdown.value).toEqual("cd.go.artifact.docker.registry");
    expect(pluginIdDropdown).toBeDisabled();
  });

  it("should show plugin select error when can not auto detect plugin", () => {
    mount();

    attributes.artifactId("some-non-existing-plugin");
    m.redraw.sync();

    const pluginAutoDetectError = "The plugin with which the artifact is associated cannot be determined because: the pipeline, stage, job or artifact id is a parameter or is non-existent. Please choose a plugin to configure the plugin properties.";
    const pluginIdDropdown      = (helper.byTestId("form-field-input-plugin-id") as HTMLInputElement);
    expect(pluginIdDropdown).not.toBeDisabled();

    expect(helper.qa("span")).toContainText(pluginAutoDetectError);
  });

  function mount(fetchTaskAttributes: FetchTaskAttributes = attributes, pluginInfos?: PluginInfos) {
    if (!pluginInfos) {
      pluginInfos = new PluginInfos(PluginInfo.fromJSON(ArtifactPluginInfo.docker()));
    }

    const autoSuggestions = {
      pipeline: {
        stage: {
          job: {
            id1: "cd.go.artifact.docker.registry"
          }
        }
      }
    };

    return helper.mount(() => <ExternalFetchArtifactView autoSuggestions={Stream(autoSuggestions)}
                                                         artifactPluginInfos={pluginInfos!}
                                                         attributes={fetchTaskAttributes}/>);
  }
});
