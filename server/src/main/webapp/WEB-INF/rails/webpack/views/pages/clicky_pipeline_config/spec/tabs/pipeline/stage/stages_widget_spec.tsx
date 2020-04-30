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
import {DependentPipeline} from "models/internal_pipeline_structure/pipeline_structure";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {Stage} from "models/pipeline_configs/stage";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {EntityReOrderHandler} from "views/pages/clicky_pipeline_config/tabs/common/re_order_entity_widget";
import {StagesWidget} from "views/pages/clicky_pipeline_config/tabs/pipeline/stage/stages_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Stages Widget", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));
  it("should not render when using template", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTemplate());

    mount(pipelineConfig.stages, pipelineConfig.isUsingTemplate());

    expect(helper.byTestId("stages-container")).not.toBeInDOM();
  });

  it("should render stages when defined", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());

    mount(pipelineConfig.stages, pipelineConfig.isUsingTemplate());

    const stagesContainer = helper.byTestId("stages-container");
    expect(stagesContainer).toBeInDOM();

    const allRows = helper.allByTestId("table-row", stagesContainer);
    expect(allRows).toHaveLength(2);
    expect(helper.qa("td", allRows[0])[1]).toHaveText("StageOne");
    expect(helper.qa("td", allRows[0])[2]).toHaveText("manual");
    expect(helper.qa("td", allRows[0])[3]).toHaveText("1");

    expect(helper.qa("td", allRows[1])[1]).toHaveText("StageTwo");
    expect(helper.qa("td", allRows[1])[2]).toHaveText("manual");
    expect(helper.qa("td", allRows[1])[3]).toHaveText("3");
  });

  it("should render each stage as a link to the stage settings page", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
    mount(pipelineConfig.stages, pipelineConfig.isUsingTemplate());

    const stagesContainer = helper.byTestId("stages-container");
    expect(stagesContainer).toBeInDOM();

    const allRows = helper.allByTestId("table-row", stagesContainer);

    const expectedHref = "StageOne/stage_settings";
    const stageName    = (helper.q("td a", allRows[0]) as HTMLAnchorElement);

    expect(stageName).toHaveText("StageOne");
    expect(stageName.href).toContain(expectedHref);
  });

  it("should render delete stage icon", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());

    mount(pipelineConfig.stages, pipelineConfig.isUsingTemplate());

    expect(helper.byTestId("stageone-delete-icon")).toBeInDOM();
    expect(helper.byTestId("stagetwo-delete-icon")).toBeInDOM();
  });

  it("should disable delete stage when there is only one stage", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
    pipelineConfig.stages().delete(pipelineConfig.firstStage());

    mount(pipelineConfig.stages, pipelineConfig.isUsingTemplate());

    expect(helper.byTestId("stagetwo-delete-icon")).toBeInDOM();
    const expectedMsg = "Can not delete the only stage from the pipeline.";
    expect(helper.byTestId("stagetwo-delete-icon").title).toEqual(expectedMsg);
    expect(helper.byTestId("stagetwo-delete-icon")).toBeDisabled();
  });

  it("should not disable delete stages when there are more than one stage", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());

    mount(pipelineConfig.stages, pipelineConfig.isUsingTemplate());

    expect(helper.byTestId("stageone-delete-icon")).toBeInDOM();
    expect(helper.byTestId("stagetwo-delete-icon")).not.toBeDisabled();

    expect(helper.byTestId("stagetwo-delete-icon")).toBeInDOM();
    expect(helper.byTestId("stagetwo-delete-icon")).not.toBeDisabled();
  });

  it("should disable delete stage when it has dependent pipelines", () => {
    const pipelineConfig     = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
    const dependentPipelines = [
      {
        dependent_pipeline_name: "downstream",
        depends_on_stage: "StageTwo"
      }
    ] as DependentPipeline[];
    mount(pipelineConfig.stages, pipelineConfig.isUsingTemplate(), true, dependentPipelines);

    expect(helper.byTestId("stagetwo-delete-icon")).toBeInDOM();
    const expectedMsg = "Can not delete stage 'StageTwo' as pipeline(s) 'downstream' depends on it.";
    expect(helper.byTestId("stagetwo-delete-icon").title).toEqual(expectedMsg);
    expect(helper.byTestId("stagetwo-delete-icon")).toBeDisabled();
  });

  it("should delete stage on click of delete icon", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
    mount(pipelineConfig.stages, pipelineConfig.isUsingTemplate());
    expect(pipelineConfig.stages()).toHaveLength(2);

    helper.clickByTestId("stageone-delete-icon");

    const body = document.body;
    expect(helper.byTestId("modal-title", body)).toContainText("Delete Stage");
    expect(helper.byTestId("modal-body", body)).toContainText("Do you want to delete the stage 'StageOne'?");

    helper.clickByTestId("primary-action-button", body);

    expect(pipelineConfig.stages()).toHaveLength(1);
  });

  it("should render add stage button", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());

    mount(pipelineConfig.stages, pipelineConfig.isUsingTemplate());

    expect(helper.byTestId("add-stage-button")).toBeInDOM();
    expect(helper.byTestId("add-stage-button")).toHaveText("Add new stage");
  });

  it("should render render add stage  modal on click of add new stage button", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());

    mount(pipelineConfig.stages, pipelineConfig.isUsingTemplate());

    helper.clickByTestId("add-stage-button");

    const modal = helper.modal();
    expect(modal).toBeInDOM();
    helper.closeModal();
  });

  describe("Pipeline defined in config repo", () => {
    it("should not allow to change order of the stages", () => {
      const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());

      mount(pipelineConfig.stages, pipelineConfig.isUsingTemplate(), false);

      expect(helper.textAllByTestId("table-row-drag-icon")).toHaveLength(0);
    });

    it("should not render delete icon", () => {
      const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());

      mount(pipelineConfig.stages, pipelineConfig.isUsingTemplate(), false);

      expect(helper.byTestId("stageone-delete-icon")).not.toBeInDOM();
      expect(helper.byTestId("stagetwo-delete-icon")).not.toBeInDOM();
    });
  });

  function mount(stages: Stream<NameableSet<Stage>>,
                 isUsingTemplate                         = false,
                 isEditable                              = true,
                 dependentPipelines: DependentPipeline[] = []) {

    const onSave               = jasmine.createSpy().and.returnValue(Promise.resolve());
    const onReset              = jasmine.createSpy().and.returnValue(Promise.resolve());
    const flashMessage         = new FlashMessageModelWithTimeout();
    const entityReOrderHandler = new EntityReOrderHandler("", flashMessage, onSave, onReset, () => false);

    helper.mount(() => <StagesWidget stages={stages}
                                     entityReOrderHandler={entityReOrderHandler}
                                     pipelineConfigSave={onSave}
                                     pipelineConfigReset={onReset}
                                     dependentPipelines={Stream(dependentPipelines)}
                                     flashMessage={flashMessage}
                                     isUsingTemplate={isUsingTemplate}
                                     isEditable={isEditable}/>);
  }
});
