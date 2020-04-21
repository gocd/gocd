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
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {AddStageModal} from "views/pages/clicky_pipeline_config/tabs/pipeline/stage/add_stage_modal";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Add Stage Modal", () => {
  const helper = new TestHelper();
  let modal: AddStageModal;

  beforeEach(mount);
  afterEach(helper.unmount.bind(helper));

  it("should render modal title", () => {
    expect(modal.title()).toBe("Add new Stage");
  });

  it("should render stage settings tab for defining a new stage", () => {
    expect(helper.byTestId("add-stage-modal")).toBeInDOM();
    expect(helper.byTestId("stage-settings")).toBeInDOM();
    expect(helper.byTestId("additional-stage-settings")).not.toBeInDOM();
  });

  it("should render job name for defining a new stage", () => {
    expect(helper.byTestId("add-stage-modal")).toBeInDOM();
    expect(helper.byTestId("form-field-input-job-name")).toBeInDOM();
  });

  it("should allow users to define a job and task as part of new stage definition", () => {
    const helpText = "You can add more jobs and tasks to this stage once the stage has been created.";

    expect(helper.byTestId("add-stage-modal")).toBeInDOM();
    expect(helper.byTestId("initial-job-and-task-header")).toContainText("Initial Job and Task");
    expect(helper.byTestId("initial-job-and-task-header-help-text")).toContainText(helpText);
  });

  it("should render task type dropdown", () => {
    const dropdown = helper.q("select", helper.byTestId("add-job-modal"));
    const options  = helper.qa("option", helper.byTestId("add-job-modal"));

    expect(dropdown).toBeInDOM();
    expect(dropdown).toHaveLength(4);

    expect(options[0]).toHaveValue("Ant");
    expect(options[1]).toHaveValue("NAnt");
    expect(options[2]).toHaveValue("Rake");
    expect(options[3]).toHaveValue("Custom Command");
  });

  it("should render ant task view by default", () => {
    expect(helper.byTestId("ant-task-modal")).toBeInDOM();
  });

  it("should render nant task view", () => {
    expect(helper.byTestId("ant-task-modal")).toBeInDOM();

    helper.onchange("select", "NAnt");

    expect(helper.byTestId("nant-task-modal")).toBeInDOM();
  });

  it("should render rake task view", () => {
    expect(helper.byTestId("ant-task-modal")).toBeInDOM();

    helper.onchange("select", "Rake");

    expect(helper.byTestId("rake-task-modal")).toBeInDOM();
  });

  it("should render custom task view", () => {
    expect(helper.byTestId("ant-task-modal")).toBeInDOM();

    helper.onchange("select", "Custom Command");

    expect(helper.byTestId("exec-task-modal")).toBeInDOM();
  });

  it("should show error message when conflicting stage name is specified", function () {
    const conflictingStageName = "StageOne";

    const stageNameInput = helper.byTestId("stage-name-input");
    expect(stageNameInput).toBeInDOM();
    helper.oninput(stageNameInput, conflictingStageName);
    modal.onbeforeupdate({} as m.VnodeDOM<any, any>);

    m.redraw.sync();

    const expectedErrorMsg = "Another stage with the same name already exists!";
    expect(helper.q(`#${stageNameInput.id}-error-text`)).toBeInDOM();
    expect(helper.q(`#${stageNameInput.id}-error-text`)).toContainText(expectedErrorMsg);
  });

  it("should remove conflicting name message when conflicting stage name is fixed", function () {
    const uniqueStageName      = "my-new-stage-name";
    const conflictingStageName = "StageOne";

    expect(helper.byTestId("stage-name-input")).toBeInDOM();

    helper.oninput(helper.byTestId("stage-name-input"), conflictingStageName);
    modal.onbeforeupdate({} as m.VnodeDOM<any, any>);

    m.redraw.sync();

    const expectedErrorMsg = "Another stage with the same name already exists!";
    expect(helper.q(`#${helper.byTestId("stage-name-input").id}-error-text`)).toBeInDOM();
    expect(helper.q(`#${helper.byTestId("stage-name-input").id}-error-text`)).toContainText(expectedErrorMsg);

    helper.oninput(helper.byTestId("stage-name-input"), uniqueStageName);
    modal.onbeforeupdate({} as m.VnodeDOM<any, any>);

    m.redraw.sync();

    expect(helper.q(`#${helper.byTestId("stage-name-input").id}-error-text`)).not.toBeInDOM();
  });

  function mount() {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());

    modal = new AddStageModal(pipelineConfig.stages(), jasmine.createSpy());
    helper.mount(() => modal.body());
  }
});
