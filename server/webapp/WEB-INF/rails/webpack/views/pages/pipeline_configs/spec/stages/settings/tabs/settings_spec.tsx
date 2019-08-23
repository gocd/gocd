/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import {StageConfig} from "models/new_pipeline_configs/stage_configuration";
import * as simulateEvent from "simulate-event";
import {StageSettingsTab} from "views/pages/pipeline_configs/stages/settings/tabs/settings";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Pipeline Config - Stage Settings Modal - Stage Settings Tab", () => {
  const helper = new TestHelper();

  let stageConfig: StageConfig;
  beforeEach(() => {
    stageConfig = new StageConfig("my_test_stage");

    helper.mount(() => {
      return <StageSettingsTab stageConfig={Stream(stageConfig)}/>;
    });
  });

  afterEach(() => {
    helper.unmount();
  });

  it("should render stage name input field", () => {
    expect(helper.findByDataTestId("form-field-label-stage-name")).toContainText("Stage Name");
    expect(helper.findByDataTestId("form-field-input-stage-name")).toBeInDOM();

    expect((helper.findByDataTestId("form-field-input-stage-name")[0] as HTMLInputElement).value)
      .toEqual(stageConfig.name());
  });

  it("should update stage name input field when stage name model property is updated", () => {
    const newStageName = "new_stage_name";
    stageConfig.name(newStageName);
    m.redraw.sync();

    expect(stageConfig.name()).toEqual(newStageName);
    expect((helper.findByDataTestId("form-field-input-stage-name")[0] as HTMLInputElement).value).toEqual(newStageName);
  });

  it("should update stage name model property when stage name input field is updated", () => {
    const newStageName = "another_new_stage_name";
    helper.findByDataTestId("form-field-input-stage-name").val(newStageName);
    simulateEvent.simulate(helper.findByDataTestId("form-field-input-stage-name")[0], "input");
    m.redraw.sync();

    expect(stageConfig.name()).toEqual(newStageName);
    expect((helper.findByDataTestId("form-field-input-stage-name")[0] as HTMLInputElement).value).toEqual(newStageName);
  });

  it("should render approval type switch", () => {
    expect(helper.findByDataTestId("switch-wrapper")).toBeInDOM();
    expect(helper.findByDataTestId("switch-label")).toContainText("Automatically run this stage on upstream changes:");
    expect((helper.findByDataTestId("switch-checkbox")[0] as HTMLInputElement).checked).toEqual(true);
  });

  it("should allow toggling approval switch", () => {
    expect(helper.findByDataTestId("switch-wrapper")).toBeInDOM();

    expect(stageConfig.approval().state()).toEqual(true);
    expect((helper.findByDataTestId("switch-checkbox")[0] as HTMLInputElement).checked).toEqual(true);

    helper.findByDataTestId("switch-checkbox").val("off");
    simulateEvent.simulate(helper.findByDataTestId("switch-checkbox")[0], "click");
    m.redraw.sync();

    expect(stageConfig.approval().state()).toEqual(false);
    expect((helper.findByDataTestId("switch-checkbox")[0] as HTMLInputElement).checked).toEqual(false);
  });

  it("should render fetch materials checkbox", () => {
    expect(helper.findByDataTestId("form-field-input-fetch-materials")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-label-fetch-materials")).toBeInDOM();

    expect((helper.findByDataTestId("form-field-input-fetch-materials")[0] as HTMLInputElement).checked).toEqual(false);
    expect(helper.findByDataTestId("form-field-label-fetch-materials")).toContainText("Fetch Materials");
  });

  it("should allow toggling fetch materials checkbox", () => {
    expect((helper.findByDataTestId("form-field-input-fetch-materials")[0] as HTMLInputElement).checked).toEqual(false);

    helper.findByDataTestId("form-field-input-fetch-materials").val("on");
    simulateEvent.simulate(helper.findByDataTestId("form-field-input-fetch-materials")[0], "click");

    expect((helper.findByDataTestId("form-field-input-fetch-materials")[0] as HTMLInputElement).checked).toEqual(true);
  });

  it("should render fetch materials checkbox", () => {
    expect(helper.findByDataTestId("form-field-input-fetch-materials")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-label-fetch-materials")).toBeInDOM();

    expect((helper.findByDataTestId("form-field-input-fetch-materials")[0] as HTMLInputElement).checked).toEqual(false);
    expect(helper.findByDataTestId("form-field-label-fetch-materials")).toContainText("Fetch Materials");
  });

  it("should allow toggling fetch materials checkbox", () => {
    expect((helper.findByDataTestId("form-field-input-fetch-materials")[0] as HTMLInputElement).checked).toEqual(false);

    helper.findByDataTestId("form-field-input-fetch-materials").val("on");
    simulateEvent.simulate(helper.findByDataTestId("form-field-input-fetch-materials")[0], "click");

    expect((helper.findByDataTestId("form-field-input-fetch-materials")[0] as HTMLInputElement).checked).toEqual(true);
  });

  it("should render Never Cleanup Artifacts checkbox", () => {
    expect(helper.findByDataTestId("form-field-input-never-cleanup-artifacts")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-label-never-cleanup-artifacts")).toBeInDOM();

    expect((helper.findByDataTestId("form-field-input-never-cleanup-artifacts")[0] as HTMLInputElement).checked).toEqual(false);
    expect(helper.findByDataTestId("form-field-label-never-cleanup-artifacts")).toContainText("Never Cleanup Artifacts");
  });

  it("should allow toggling Never Cleanup Artifacts checkbox", () => {
    expect((helper.findByDataTestId("form-field-input-never-cleanup-artifacts")[0] as HTMLInputElement).checked).toEqual(false);

    helper.findByDataTestId("form-field-input-never-cleanup-artifacts").val("on");
    simulateEvent.simulate(helper.findByDataTestId("form-field-input-never-cleanup-artifacts")[0], "click");

    expect((helper.findByDataTestId("form-field-input-never-cleanup-artifacts")[0] as HTMLInputElement).checked).toEqual(true);
  });

  it("should render Clean Working Directory checkbox", () => {
    expect(helper.findByDataTestId("form-field-input-clean-working-directory")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-label-clean-working-directory")).toBeInDOM();

    expect((helper.findByDataTestId("form-field-input-clean-working-directory")[0] as HTMLInputElement).checked).toEqual(false);
    expect(helper.findByDataTestId("form-field-label-clean-working-directory")).toContainText("Clean Working Directory");
  });

  it("should allow toggling Clean Working Directory checkbox", () => {
    expect((helper.findByDataTestId("form-field-input-clean-working-directory")[0] as HTMLInputElement).checked).toEqual(false);

    helper.findByDataTestId("form-field-input-clean-working-directory").val("on");
    simulateEvent.simulate(helper.findByDataTestId("form-field-input-clean-working-directory")[0], "click");

    expect((helper.findByDataTestId("form-field-input-clean-working-directory")[0] as HTMLInputElement).checked).toEqual(true);
  });

});
