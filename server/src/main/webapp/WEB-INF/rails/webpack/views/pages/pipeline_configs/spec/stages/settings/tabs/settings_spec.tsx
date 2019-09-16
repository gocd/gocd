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
    expect(helper.byTestId("form-field-label-stage-name")).toContainText("Stage Name");
    expect(helper.byTestId("form-field-input-stage-name")).toBeInDOM();

    expect((helper.byTestId("form-field-input-stage-name") as HTMLInputElement).value)
      .toEqual(stageConfig.name());
  });

  it("should update stage name input field when stage name model property is updated", () => {
    const newStageName = "new_stage_name";
    stageConfig.name(newStageName);
    m.redraw.sync();

    expect(stageConfig.name()).toEqual(newStageName);
    expect((helper.byTestId("form-field-input-stage-name") as HTMLInputElement).value).toEqual(newStageName);
  });

  it("should update stage name model property when stage name input field is updated", () => {
    const newStageName = "another_new_stage_name";
    (helper.byTestId("form-field-input-stage-name") as HTMLInputElement).value = newStageName;
    helper.oninput(helper.byTestId("form-field-input-stage-name"), newStageName);

    expect(stageConfig.name()).toEqual(newStageName);
    expect((helper.byTestId("form-field-input-stage-name") as HTMLInputElement).value).toEqual(newStageName);
  });

  it("should render approval type switch", () => {
    expect(helper.byTestId("switch-wrapper")).toBeInDOM();
    expect(helper.textByTestId("switch-label")).toContain("Automatically run this stage on upstream changes:");
    expect(checked("switch-checkbox")).toBe(true);
  });

  it("should allow toggling approval switch", () => {
    expect(helper.byTestId("switch-wrapper")).toBeInDOM();

    expect(stageConfig.approval().state()).toEqual(true);
    expect(checked("switch-checkbox")).toBe(true);

    click("switch-checkbox");

    expect(stageConfig.approval().state()).toBe(false);
    expect(checked("switch-checkbox")).toBe(false);
  });

  it("should render fetch materials checkbox", () => {
    expect(helper.byTestId("form-field-input-fetch-materials")).toBeInDOM();
    expect(helper.byTestId("form-field-label-fetch-materials")).toBeInDOM();

    expect(checked("form-field-input-fetch-materials")).toBe(false);
    expect(helper.textByTestId("form-field-label-fetch-materials")).toContain("Fetch Materials");
  });

  it("should allow toggling fetch materials checkbox", () => {
    expect(checked("form-field-input-fetch-materials")).toBe(false);

    click("form-field-input-fetch-materials");
    expect(checked("form-field-input-fetch-materials")).toBe(true);
  });

  it("should render Never Cleanup Artifacts checkbox", () => {
    expect(helper.byTestId("form-field-input-never-cleanup-artifacts")).toBeInDOM();
    expect(helper.byTestId("form-field-label-never-cleanup-artifacts")).toBeInDOM();

    expect(checked("form-field-input-never-cleanup-artifacts")).toBe(false);
    expect(helper.textByTestId("form-field-label-never-cleanup-artifacts")).toContain("Never Cleanup Artifacts");
  });

  it("should allow toggling Never Cleanup Artifacts checkbox", () => {
    expect(checked("form-field-input-never-cleanup-artifacts")).toBe(false);

    click("form-field-input-never-cleanup-artifacts");
    expect(checked("form-field-input-never-cleanup-artifacts")).toBe(true);
  });

  it("should render Clean Working Directory checkbox", () => {
    expect(helper.byTestId("form-field-input-clean-working-directory")).toBeInDOM();
    expect(helper.byTestId("form-field-label-clean-working-directory")).toBeInDOM();

    expect(checked("form-field-input-clean-working-directory")).toBe(false);
    expect(helper.textByTestId("form-field-label-clean-working-directory")).toContain("Clean Working Directory");
  });

  it("should allow toggling Clean Working Directory checkbox", () => {
    expect(checked("form-field-input-clean-working-directory")).toBe(false);

    click("form-field-input-clean-working-directory");
    expect(checked("form-field-input-clean-working-directory")).toBe(true);
  });

  function click(id: string) {
    (helper.byTestId(id) as HTMLElement).click();
    m.redraw.sync();
  }

  function checked(id: string) {
    return (helper.byTestId(id) as HTMLInputElement).checked;
  }
});
