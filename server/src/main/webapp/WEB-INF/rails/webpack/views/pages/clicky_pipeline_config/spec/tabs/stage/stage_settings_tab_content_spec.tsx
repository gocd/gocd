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
import Stream from "mithril/stream";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {StageSettingsTabContent} from "views/pages/clicky_pipeline_config/tabs/stage/stage_settings_tab_content";
import {OperationState} from "views/pages/page_operations";
import {TestHelper} from "views/pages/spec/test_helper";

describe("StageSettingsTab", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render stage name", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));

    mount(stage);

    expect(helper.byTestId("stage-name-input")).toBeInDOM();
    expect(helper.byTestId("stage-name-input")).toHaveValue("Test");
  });

  it("should change the stage name when textfield value is changed", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    mount(stage);

    expect(stage.name()).toEqual("Test");

    helper.oninput(helper.byTestId("stage-name-input"), "IntegrationTest");

    expect(stage.name()).toEqual("IntegrationTest");
  });

  it("should allow to change approval type", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));
    mount(stage);

    expect(stage.approval().typeAsString()).toEqual("manual");

    helper.clickByTestId("approval-checkbox");

    expect(stage.approval().typeAsString()).toEqual("success");
  });

  it("should render allow only in success", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));
    mount(stage);
    expect(stage.approval().allowOnlyOnSuccess()).toBeFalse();

    helper.clickByTestId("allow-only-on-success-checkbox");

    expect(stage.approval().allowOnlyOnSuccess()).toBeTrue();
  });

  it("should render fetch material", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));
    mount(stage);

    expect(stage.fetchMaterials()).toBeFalsy();

    helper.clickByTestId("fetch-materials-checkbox");

    expect(stage.fetchMaterials()).toBeTrue();
  });

  it("should render never cleanup artifacts directory", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));
    mount(stage);

    expect(stage.neverCleanupArtifacts()).toBeFalsy();

    helper.clickByTestId("never-cleanup-artifacts-checkbox");

    expect(stage.neverCleanupArtifacts()).toBeTrue();
  });

  it("should render clean working directory", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));
    mount(stage);

    expect(stage.cleanWorkingDirectory()).toBeFalsy();

    helper.clickByTestId("clean-working-directory-checkbox");

    expect(stage.cleanWorkingDirectory()).toBeTrue();
  });

  function mount(stage: Stage) {
    const pipelineConfig = new PipelineConfig();
    pipelineConfig.stages().add(stage);
    const routeParams    = {stage_name: stage.name()} as PipelineConfigRouteParams;
    const templateConfig = new TemplateConfig("foo", []);
    helper.mount(() => new StageSettingsTabContent().content(pipelineConfig,
                                                             templateConfig,
                                                             routeParams,
                                                             true,
                                                             Stream<OperationState>(OperationState.UNKNOWN)));
  }
});
