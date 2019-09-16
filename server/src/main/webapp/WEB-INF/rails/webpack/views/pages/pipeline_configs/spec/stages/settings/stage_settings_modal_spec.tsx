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
import {StageSettingsModal} from "views/pages/pipeline_configs/stages/settings/stage_settings_modal";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Pipeline Config - Stage Settings Modal", () => {
  const helper = new TestHelper();
  const body = document.body;

  let stage: StageConfig;
  let modal: StageSettingsModal;

  beforeEach(() => {
    stage = new StageConfig("up42_stage");
    modal = new StageSettingsModal(Stream(stage));
    modal.render();
    m.redraw.sync();
  });

  afterEach(() => {
    modal.close();
  });

  it("should render modal title", () => {
    const modalTitle = "Stage Settings";

    expect(modal.title()).toBe(modalTitle);
    expect(helper.textByTestId("modal-title", body)).toContain(modalTitle);
  });

  it("should render tab headings", () => {

    expect(helper.textByTestId("tab-header-0", body)).toContain("Stage Settings");
    expect(helper.textByTestId("tab-header-1", body)).toContain("Environment Variables");
    expect(helper.textByTestId("tab-header-2", body)).toContain("Permissions");
  });

  it("should render stage settings widget by default", () => {
    expect(helper.textByTestId("tab-header-0", body)).toContain("Stage Settings");
    expect(helper.byTestId("stage-settings-tab", body)).toBeInDOM();

    expect(isHidden("tab-content-0")).toBe(false);
    expect(isHidden("tab-content-1")).toBe(true);
    expect(isHidden("tab-content-2")).toBe(true);
  });

  it("should render stage settings permission widget on click", () => {
    expect(helper.textByTestId("tab-header-0", body)).toContain("Stage Settings");
    expect(helper.byTestId("stage-settings-tab", body)).toBeInDOM();

    expect(helper.textByTestId("tab-header-2", body)).toContain("Permissions");
    expect(helper.byTestId("stage-permissions-tab", body)).toBeInDOM();

    expect(isHidden("tab-content-0")).toBe(false);
    expect(isHidden("tab-content-1")).toBe(true);
    expect(isHidden("tab-content-2")).toBe(true);

    helper.clickByTestId("tab-header-2", body);

    expect(isHidden("tab-content-0")).toBe(true);
    expect(isHidden("tab-content-1")).toBe(true);
    expect(isHidden("tab-content-2")).toBe(false);
  });

  function isHidden(tab: string) {
    const classList = helper.byTestId(tab, body).classList;
    return Array.from(classList).some((c) => c.indexOf("hide") !== -1);
  }
});
