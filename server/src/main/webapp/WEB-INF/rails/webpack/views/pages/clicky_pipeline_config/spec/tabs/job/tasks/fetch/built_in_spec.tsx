/*!
 * Copyright 2021 ThoughtWorks, Inc.
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
import {BuiltInFetchArtifactView} from "views/pages/clicky_pipeline_config/tabs/job/tasks/fetch/built_in";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Built In Fetch Artifact Task", () => {
  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  let attributes: FetchTaskAttributes;
  beforeEach(() => {
    attributes = new FetchTaskAttributes("gocd",
                                         "pipeline",
                                         "stage",
                                         "job",
                                         false,
                                         "source",
                                         "destination",
                                         undefined,
                                         new Configurations([]),
                                         [],
                                         undefined);
  });

  it("should render built in fetch task", () => {
    mount();
    expect(helper.byTestId("built-in-fetch-artifact-view")).toBeInDOM();
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

  it("should render source input", () => {
    mount();
    const sourceHelpText = "The path of the artifact directory or file of a specific job, relative to the sandbox directory. If the directory or file does not exist, the job is failed.";

    expect(helper.byTestId("form-field-label-source")).toContainText("Source");
    expect(helper.byTestId("form-field-input-source")).toBeInDOM();
    expect(helper.qa("span")).toContainText(sourceHelpText);
  });

  it("should bind source input to model", () => {
    mount();

    expect(attributes.source()).toBe("source");
    expect(helper.byTestId("form-field-input-source")).toHaveValue("source");

    helper.oninput(`[data-test-id="form-field-input-source"]`, "new-source");

    expect(attributes.source()).toBe("new-source");
    expect(helper.byTestId("form-field-input-source")).toHaveValue("new-source");
  });

  it("should render is source a file checkbox", () => {
    mount();

    const label = "Source is a file(Not a directory)";
    expect(helper.byTestId("form-field-label-source-is-a-file-not-a-directory")).toContainText(label);
    expect(helper.byTestId("form-field-input-source-is-a-file-not-a-directory")).not.toBeChecked();
  });

  it("should bind is source a file checkbox to model", () => {
    mount();

    expect(attributes.isSourceAFile()).toBeFalse();
    expect(helper.byTestId("form-field-input-source-is-a-file-not-a-directory")).not.toBeChecked();

    helper.clickByTestId("form-field-input-source-is-a-file-not-a-directory");

    expect(attributes.isSourceAFile()).toBeTrue();
    expect(helper.byTestId("form-field-input-source-is-a-file-not-a-directory")).toBeChecked();
  });

  it("should render destination input", () => {
    mount();
    const destinationHelpText = "The path of the directory where the artifact is fetched to. The directory is overwritten if it already exists. The directory path is relative to the pipeline working directory.";

    expect(helper.byTestId("form-field-label-destination")).toContainText("Destination");
    expect(helper.byTestId("form-field-input-destination")).toBeInDOM();
    expect(helper.qa("span")).toContainText(destinationHelpText);
  });

  it("should bind destination input to model", () => {
    mount();

    expect(attributes.destination()).toBe("destination");
    expect(helper.byTestId("form-field-input-destination")).toHaveValue("destination");

    helper.oninput(`[data-test-id="form-field-input-destination"]`, "new-destination");

    expect(attributes.destination()).toBe("new-destination");
    expect(helper.byTestId("form-field-input-destination")).toHaveValue("new-destination");
  });

  describe("Read Only", () => {
    beforeEach(() => {
      mount(undefined, true);
    });

    it("should render disabled pipeline input", () => {
      expect(helper.byTestId("form-field-input-pipeline")).toBeDisabled();
    });

    it("should render disabled stage input", () => {
      expect(helper.byTestId("form-field-input-stage")).toBeDisabled();
    });

    it("should render disabled job input", () => {
      expect(helper.byTestId("form-field-input-job")).toBeDisabled();
    });

    it("should render disabled source input", () => {
      expect(helper.byTestId("form-field-input-source")).toBeDisabled();
    });

    it("should render disabled destination input", () => {
      expect(helper.byTestId("form-field-input-destination")).toBeDisabled();
    });

    it("should render disabled is source a file checkbox", () => {
      expect(helper.byTestId("form-field-input-source-is-a-file-not-a-directory")).toBeDisabled();
    });
  });

  function mount(fetchTaskAttributes: FetchTaskAttributes = attributes, readonly: boolean = false) {
    return helper.mount(() => <BuiltInFetchArtifactView autoSuggestions={Stream({})}
                                                        readonly={readonly}
                                                        attributes={fetchTaskAttributes}/>);
  }
});
