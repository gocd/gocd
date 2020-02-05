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
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {Stage} from "models/pipeline_configs/stage";
import {StagesWidget} from "views/pages/clicky_pipeline_config/widgets/stages_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("StagesWidget", () => {
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

  it("should render delete stage icon", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());

    mount(pipelineConfig.stages, pipelineConfig.isUsingTemplate());

    expect(helper.byTestId("stageone-delete-icon")).toBeInDOM();
    expect(helper.byTestId("stagetwo-delete-icon")).toBeInDOM();
  });

  it("should delete stage on click of delete icon", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
    mount(pipelineConfig.stages, pipelineConfig.isUsingTemplate());
    expect(pipelineConfig.stages()).toHaveLength(2);

    helper.clickByTestId("stageone-delete-icon");

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

  function mount(stages: Stream<NameableSet<Stage>>, isUsingTemplate = Stream<boolean>(false), isEditable = true) {
    helper.mount(() => <StagesWidget stages={stages} isUsingTemplate={isUsingTemplate} isEditable={isEditable}/>);
  }
});
