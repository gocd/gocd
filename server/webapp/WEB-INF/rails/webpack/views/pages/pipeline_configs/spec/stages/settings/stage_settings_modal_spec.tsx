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

    expect(modal.title()).toEqual(modalTitle);
    expect(helper.findIn($("body"), "modal-title")).toContainText(modalTitle);
  });

  it("should render tab headings", () => {
    const body = $("body");

    expect(helper.findIn(body, "tab-header-0")).toContainText("Stage Settings");
    expect(helper.findIn(body, "tab-header-1")).toContainText("Environment Variables");
    expect(helper.findIn(body, "tab-header-2")).toContainText("Permissions");
  });

  it("should render stage settings widget by default", () => {
    const body = $("body");

    expect(helper.findIn(body, "tab-header-0")).toContainText("Stage Settings");
    expect(helper.findIn(body, "stage-settings-tab")).toBeInDOM();
  });

});
