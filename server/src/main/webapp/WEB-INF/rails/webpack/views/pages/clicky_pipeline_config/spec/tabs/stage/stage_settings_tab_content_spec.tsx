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
import {Origin, OriginType} from "models/origin";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {StageSettingsTabContent, StageSettingsWidget} from "views/pages/clicky_pipeline_config/tabs/stage/stage_settings_tab_content";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
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

  it("should render elastic profile id", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.elasticProfileId("some-value");
    mount(stage);

    expect(helper.byTestId("elastic-agent-id-input")).toBeInDOM();
    expect(helper.byTestId("elastic-agent-id-input")).toHaveValue("some-value");

    helper.oninput(helper.byTestId("elastic-agent-id-input"), "some-other-value");
    expect(stage.elasticProfileId()).toEqual("some-other-value");
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

    const labelElement    = helper.allByTestId("switch-label")[0];
    const helpTextElement = helper.qa("#switch-btn-help-text")[0];
    const label           = "Trigger on completion of previous stage";
    const helpText        = StageSettingsWidget.APPROVAL_TYPE_HELP;

    expect(labelElement).toContainText(label);
    expect(helpTextElement).toContainText(helpText);

    expect(stage.approval().typeAsString()).toEqual("manual");

    helper.clickByTestId("approval-checkbox");

    expect(stage.approval().typeAsString()).toEqual("success");
  });

  it("should render allow only in success", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));
    mount(stage);

    const labelElement    = helper.allByTestId("switch-label")[1];
    const helpTextElement = helper.qa("#switch-btn-help-text")[1];
    const label           = "Allow only on success of previous stage";
    const helpText        = StageSettingsWidget.ALLOW_ONLY_ON_SUCCESS_HELP;

    expect(labelElement).toContainText(label);
    expect(helpTextElement).toContainText(helpText);

    expect(stage.approval().allowOnlyOnSuccess()).toBeFalse();

    helper.clickByTestId("allow-only-on-success-checkbox");

    expect(stage.approval().allowOnlyOnSuccess()).toBeTrue();
  });

  it("should render fetch material", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));
    mount(stage);

    const labelElement    = helper.allByTestId("switch-label")[2];
    const helpTextElement = helper.qa("#switch-btn-help-text")[2];
    const label           = "Fetch materials";
    const helpText        = "Perform material updates or checkouts.";

    expect(labelElement).toContainText(label);
    expect(helpTextElement).toContainText(helpText);

    expect(stage.fetchMaterials()).toBeFalsy();

    helper.clickByTestId("fetch-materials-checkbox");

    expect(stage.fetchMaterials()).toBeTrue();
  });

  it("should render never cleanup artifacts directory", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));
    mount(stage);

    const labelElement    = helper.allByTestId("switch-label")[3];
    const helpTextElement = helper.qa("#switch-btn-help-text")[3];
    const label           = "Never cleanup artifacts";
    const helpText        = "Never cleanup artifacts for this stage, if purging artifacts is configured at the Server Level.";

    expect(labelElement).toContainText(label);
    expect(helpTextElement).toContainText(helpText);

    expect(stage.neverCleanupArtifacts()).toBeFalsy();

    helper.clickByTestId("never-cleanup-artifacts-checkbox");

    expect(stage.neverCleanupArtifacts()).toBeTrue();
  });

  it("should render clean working directory", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));
    mount(stage);

    const labelElement    = helper.allByTestId("switch-label")[4];
    const helpTextElement = helper.qa("#switch-btn-help-text")[4];
    const label           = "Clean Working Directory";
    const helpText        = "Remove all files/directories in the working directory on the agent.";

    expect(labelElement).toContainText(label);
    expect(helpTextElement).toContainText(helpText);

    expect(stage.cleanWorkingDirectory()).toBeFalsy();

    helper.clickByTestId("clean-working-directory-checkbox");

    expect(stage.cleanWorkingDirectory()).toBeTrue();
  });

  describe("Read Only", () => {
    beforeEach(() => {
      const stage          = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));
      const pipelineOrigin = new Origin(OriginType.ConfigRepo, "repo1");
      mount(stage, pipelineOrigin);
    });

    it("should render disabled stage name", () => {
      expect(helper.byTestId("stage-name-input")).toBeDisabled();
    });

    it("should render disabled approval checkbox", () => {
      expect(helper.byTestId("approval-checkbox")).toBeDisabled();
    });

    it("should render disabled allow only on success checkbox", () => {
      expect(helper.byTestId("allow-only-on-success-checkbox")).toBeDisabled();
    });

    it("should render disabled fetch materials checkbox", () => {
      expect(helper.byTestId("fetch-materials-checkbox")).toBeDisabled();
    });

    it("should render disabled never cleanup artifacts checkbox", () => {
      expect(helper.byTestId("never-cleanup-artifacts-checkbox")).toBeDisabled();
    });

    it("should render disabled clean working directory checkbox", () => {
      expect(helper.byTestId("clean-working-directory-checkbox")).toBeDisabled();
    });

    it("should render disabled elastic profile id", () => {
      expect(helper.byTestId("elastic-agent-id-input")).toBeDisabled();
    });
  });

  describe("Stage Settings For Add a New Stage", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));

    beforeEach(() => {
      helper.mount(() => {
        return <StageSettingsWidget stage={stage} readonly={false} isForAddStagePopup={true}/>;
      });
    });

    it("should render stage name", () => {
      expect(helper.byTestId("stage-name-input")).toBeInDOM();
      expect(helper.byTestId("stage-name-input")).toHaveValue("Test");
    });

    it("should allow to change approval type", () => {
      expect(stage.approval().typeAsString()).toEqual("manual");
      expect(helper.byTestId("approval-checkbox")).toBeInDOM();
    });

    it("should render allow only in success", () => {
      expect(stage.approval().allowOnlyOnSuccess()).toBeFalse();
      expect(helper.byTestId("allow-only-on-success-checkbox")).toBeInDOM();
    });

    it("should not render fetch material", () => {
      expect(helper.byTestId("fetch-materials-checkbox")).not.toBeInDOM();
    });

    it("should not render never cleanup artifacts directory", () => {
      expect(helper.byTestId("never-cleanup-artifacts-checkbox")).not.toBeInDOM();
    });

    it("should not render clean working directory", () => {
      expect(helper.byTestId("clean-working-directory-checkbox")).not.toBeInDOM();
    });

  });

  function mount(stage: Stage, pipelineOrigin: Origin = new Origin(OriginType.GoCD)) {
    document.body.setAttribute("data-meta", JSON.stringify({pipelineName: "pipeline1"}));
    const pipelineConfig = new PipelineConfig();
    pipelineConfig.origin(pipelineOrigin);
    pipelineConfig.stages().add(stage);
    const routeParams    = {stage_name: stage.name()} as PipelineConfigRouteParams;
    const templateConfig = new TemplateConfig("foo", []);
    helper.mount(() => new StageSettingsTabContent().content(pipelineConfig,
                                                             templateConfig,
                                                             routeParams,
                                                             Stream<OperationState>(OperationState.UNKNOWN),
                                                             new FlashMessageModelWithTimeout(),
                                                             jasmine.createSpy(),
                                                             jasmine.createSpy()));
  }
});
