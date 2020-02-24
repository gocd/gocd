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

import {ArtifactType, GoCDArtifact} from "models/pipeline_configs/artifact";
import {Job} from "models/pipeline_configs/job";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {JobTestData, PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import * as simulateEvent from "simulate-event";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {ArtifactsTabContent} from "views/pages/clicky_pipeline_config/tabs/job/artifacts_tab_content";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Artifacts Tab", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render no artifacts configured message", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(job.artifacts()).toHaveLength(0);
    const expectedMsg = "No Artifacts Configured. Click Add to configure artifacts.";
    expect(helper.byTestId("flash-message-info")).toContainText(expectedMsg);
  });

  it("should render build artifact", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    job.artifacts().push(new GoCDArtifact(ArtifactType.build, "source", "destination"));
    mount(job);

    const typeDescription        = "There are 3 types of artifacts - build, test and external. When 'Test Artifact' is selected, Go will use this artifact to generate a test report. Test information is placed in the Failures and Test sub-tabs. Test results from multiple jobs are aggregated on the stage detail pages. This allows you to see the results of tests from both functional and unit tests even if they are run in different jobs. When artifact type external is selected, you can configure the external artifact store to which you can push an artifact.";
    const sourceDescription      = "The file or folders to publish to the server. Go will only upload files that are in the working directory of the job. You can use wildcards to specify the files and folders to upload (** means any path, * means any file or folder name).";
    const destinationDescription = "The destination is relative to the artifacts folder of the current instance on the server side. If it is not specified, the artifact will be stored in the root of the artifacts directory";

    expect(helper.byTestId("type-header")).toContainText("Type");
    expect(helper.allByTestId("tooltip-wrapper")[0]).toContainText(typeDescription);

    expect(helper.byTestId("source-header")).toContainText("Source");
    expect(helper.allByTestId("tooltip-wrapper")[1]).toContainText(sourceDescription);

    expect(helper.byTestId("destination-header")).toContainText("Destination");
    expect(helper.allByTestId("tooltip-wrapper")[2]).toContainText(destinationDescription);

    expect(job.artifacts()).toHaveLength(1);
    expect(helper.allByTestId("build-artifact-view")).toHaveLength(1);
    expect(helper.allByTestId("test-artifact-view")).toHaveLength(0);

    expect(helper.byTestId("artifact-type")).toHaveText("Build Artifact");
    expect(helper.byTestId("artifact-source-source")).toHaveValue("source");
    expect(helper.byTestId("artifact-destination-destination")).toHaveValue("destination");
  });

  it("should render test artifact", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    job.artifacts().push(new GoCDArtifact(ArtifactType.test, "source", "destination"));
    mount(job);

    const typeDescription        = "There are 3 types of artifacts - build, test and external. When 'Test Artifact' is selected, Go will use this artifact to generate a test report. Test information is placed in the Failures and Test sub-tabs. Test results from multiple jobs are aggregated on the stage detail pages. This allows you to see the results of tests from both functional and unit tests even if they are run in different jobs. When artifact type external is selected, you can configure the external artifact store to which you can push an artifact.";
    const sourceDescription      = "The file or folders to publish to the server. Go will only upload files that are in the working directory of the job. You can use wildcards to specify the files and folders to upload (** means any path, * means any file or folder name).";
    const destinationDescription = "The destination is relative to the artifacts folder of the current instance on the server side. If it is not specified, the artifact will be stored in the root of the artifacts directory";

    expect(helper.byTestId("type-header")).toContainText("Type");
    expect(helper.allByTestId("tooltip-wrapper")[0]).toContainText(typeDescription);

    expect(helper.byTestId("source-header")).toContainText("Source");
    expect(helper.allByTestId("tooltip-wrapper")[1]).toContainText(sourceDescription);

    expect(helper.byTestId("destination-header")).toContainText("Destination");
    expect(helper.allByTestId("tooltip-wrapper")[2]).toContainText(destinationDescription);

    expect(job.artifacts()).toHaveLength(1);
    expect(helper.allByTestId("build-artifact-view")).toHaveLength(0);
    expect(helper.allByTestId("test-artifact-view")).toHaveLength(1);

    expect(helper.byTestId("artifact-type")).toHaveText("Test Artifact");
    expect(helper.byTestId("artifact-source-source")).toHaveValue("source");
    expect(helper.byTestId("artifact-destination-destination")).toHaveValue("destination");
  });

  it("should remove artifact", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    job.artifacts().push(
      new GoCDArtifact(ArtifactType.build, "/path/to/source", "/dest"),
      new GoCDArtifact(ArtifactType.test, "/path/to/test", "/test")
    );
    mount(job);

    expect(job.artifacts()).toHaveLength(2);
    expect(helper.allByTestId("build-artifact-view")).toHaveLength(1);
    expect(helper.allByTestId("test-artifact-view")).toHaveLength(1);

    helper.click(`[data-test-id="remove-artifact"]`);

    expect(job.artifacts()).toHaveLength(1);
    expect(helper.allByTestId("build-artifact-view")).toHaveLength(0);
    expect(helper.allByTestId("test-artifact-view")).toHaveLength(1);

    helper.click(`[data-test-id="remove-artifact"]`);

    expect(job.artifacts()).toHaveLength(0);
    expect(helper.allByTestId("build-artifact-view")).toHaveLength(0);
    expect(helper.allByTestId("test-artifact-view")).toHaveLength(0);
  });

  it("should add a build artifact", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(job.artifacts()).toHaveLength(0);
    expect(helper.allByTestId("build-artifact-view")).toHaveLength(0);
    expect(helper.allByTestId("test-artifact-view")).toHaveLength(0);

    helper.click(`button`);

    expect(helper.allByTestId("build-artifact-view")).toHaveLength(1);

    helper.oninput(`[data-test-id="artifact-source-"]`, "/path/to/source");
    helper.oninput(`[data-test-id="artifact-destination-"]`, "/path/to/dest");

    expect(job.artifacts()).toHaveLength(1);
    expect(job.artifacts()[0].type()).toEqual(ArtifactType.build);
    expect((job.artifacts()[0] as GoCDArtifact).source()).toEqual("/path/to/source");
    expect((job.artifacts()[0] as GoCDArtifact).destination()).toEqual("/path/to/dest");
  });

  it("should add a test artifact", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(job.artifacts()).toHaveLength(0);
    expect(helper.allByTestId("build-artifact-view")).toHaveLength(0);
    expect(helper.allByTestId("test-artifact-view")).toHaveLength(0);

    const input = helper.q("select");

    (input as HTMLSelectElement).value = "test";
    simulateEvent.simulate(input, "change");

    helper.click(`button`);

    expect(helper.allByTestId("test-artifact-view")).toHaveLength(1);

    helper.oninput(`[data-test-id="artifact-source-"]`, "/path/to/source");
    helper.oninput(`[data-test-id="artifact-destination-"]`, "/path/to/dest");

    expect(job.artifacts()).toHaveLength(1);
    expect(job.artifacts()[0].type()).toEqual(ArtifactType.test);
    expect((job.artifacts()[0] as GoCDArtifact).source()).toEqual("/path/to/source");
    expect((job.artifacts()[0] as GoCDArtifact).destination()).toEqual("/path/to/dest");
  });

  function mount(job: Job) {
    const pipelineConfig = new PipelineConfig();

    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.jobs(new NameableSet([job]));
    pipelineConfig.stages().add(stage);

    const routeParams = {
      stage_name: stage.name(),
      job_name: job.name()
    } as PipelineConfigRouteParams;

    const templateConfig = new TemplateConfig("foo", []);
    helper.mount(() => new ArtifactsTabContent().content(pipelineConfig, templateConfig, routeParams, true));
  }
});
