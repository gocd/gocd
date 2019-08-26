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
import {StagePermissionsTab} from "views/pages/pipeline_configs/stages/settings/tabs/permissions";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Pipeline Config - Stage Settings Modal - Stage Permissions Tab", () => {
  const helper = new TestHelper();

  let stageConfig: StageConfig;
  beforeEach(() => {
    stageConfig = new StageConfig("my_test_stage");

    helper.mount(() => {
      return <StagePermissionsTab stageConfig={Stream(stageConfig)}/>;
    });
  });

  it("should render stage permissions tab", () => {
    const body = $("body");
    expect(helper.findIn(body, "stage-permissions-tab")).toBeInDOM();
  });

  it("should render the default permission message", () => {
    const msg      = "All System Administrators and Pipeline Group Administrators can operate on this pipeline.";
    const helpText = "(This permission can not be overridden!)";

    expect(helper.findByDataTestId("default-permission-message-body")).toContainText(msg);
    expect(helper.findByDataTestId("default-permission-help-message")).toContainText(helpText);
  });

  it("should render Inherit permissions from Pipeline Group checkbox", () => {
    expect(helper.findByDataTestId("switch-wrapper")).toBeInDOM();
    expect(helper.findByDataTestId("switch-label")).toContainText("Inherit permissions from Pipeline Group:");

    expect((helper.findByDataTestId("switch-checkbox")[0] as HTMLInputElement).checked).toEqual(true);
  });

  it("should allow toggling Inherit permissions from Pipeline Group checkbox", () => {
    expect(helper.findByDataTestId("switch-wrapper")).toBeInDOM();
    expect(helper.findByDataTestId("switch-label")).toContainText("Inherit permissions from Pipeline Group:");

    expect((helper.findByDataTestId("switch-checkbox")[0] as HTMLInputElement).checked).toEqual(true);
    expect(stageConfig.approval().inheritFromPipelineGroup()).toEqual(true);

    simulateEvent.simulate(helper.findByDataTestId("switch-checkbox")[0], "click");
    m.redraw.sync();

    expect((helper.findByDataTestId("switch-checkbox")[0] as HTMLInputElement).checked).toEqual(false);
    expect(stageConfig.approval().inheritFromPipelineGroup()).toEqual(false);
  });

  it("should not render Specify Permissions locally when permissions are inherited from pipeline group", () => {
    expect((helper.findByDataTestId("switch-checkbox")[0] as HTMLInputElement).checked).toEqual(true);
    expect(helper.findByDataTestId("specify-stage-permissions-locally")).not.toBeInDOM();
  });

  it("should render Specify Permissions locally when permissions are not inherited from pipeline group", () => {
    stageConfig.approval().inheritFromPipelineGroup(false);
    m.redraw.sync();

    expect((helper.findByDataTestId("switch-checkbox")[0] as HTMLInputElement).checked).toEqual(false);
    expect(helper.findByDataTestId("specify-stage-permissions-locally")).toBeInDOM();
  });

  describe("Specify Stage Permissions Locally", () => {
    beforeEach(() => {
      stageConfig.approval().inheritFromPipelineGroup(false);
      m.redraw.sync();
    });

    it("should render specify locally message", () => {
      expect(helper.findByDataTestId("specify-stage-permissions-locally")).toBeInDOM();
      expect(helper.findByDataTestId("specify-stage-permissions-locally")).toContainText("Specify locally:");
    });

    describe("Users", () => {
      it("should render user input field", () => {
        expect(helper.findByDataTestId("User-permissions")).toBeInDOM();
        expect(helper.findByDataTestId("User-permissions")).toContainText("Users");
        expect(helper.findByDataTestId("User-input")).toBeInDOM();
      });

      it("should show existing users on the UI", () => {
        stageConfig.approval().authorization()!.users.add("Bob");
        stageConfig.approval().authorization()!.users.add("John");
        m.redraw.sync();

        expect(helper.findByDataTestId("show-User-Bob")).toBeInDOM();
        expect(helper.findByDataTestId("show-User-John")).toBeInDOM();
      });

      it("should allow adding users", () => {
        const username = "Bob";
        expect(stageConfig.approval().authorization()!.users.list()).toEqual([]);

        const inputSelector = helper.findByDataTestId("User-input").find("input");
        inputSelector.val(username);

        simulateEvent.simulate(inputSelector[0], "input");
        m.redraw.sync();

        const addBtnSelector = helper.findByDataTestId("User-input").find("button");
        simulateEvent.simulate(addBtnSelector[0], "click");
        m.redraw.sync();

        expect(stageConfig.approval().authorization()!.users.list()).toEqual([username]);
      });

      it("should allow removing user", () => {
        stageConfig.approval().authorization()!.users.add("Bob");
        stageConfig.approval().authorization()!.users.add("John");
        m.redraw.sync();

        expect(helper.findByDataTestId("show-User-Bob")).toBeInDOM();
        expect(helper.findByDataTestId("show-User-John")).toBeInDOM();

        simulateEvent.simulate(helper.findByDataTestId("Close-icon")[0], "click");
        m.redraw.sync();

        expect(helper.findByDataTestId("show-User-Bob")).not.toBeInDOM();
        expect(helper.findByDataTestId("show-User-John")).toBeInDOM();
      });
    });

    describe("Roles", () => {
      it("should render role input field", () => {
        expect(helper.findByDataTestId("Role-permissions")).toBeInDOM();
        expect(helper.findByDataTestId("Role-permissions")).toContainText("Roles");
        expect(helper.findByDataTestId("User-input")).toBeInDOM();
      });

      it("should show existing roles on the UI", () => {
        stageConfig.approval().authorization()!.roles.add("Admin");
        stageConfig.approval().authorization()!.roles.add("Dev");
        m.redraw.sync();

        expect(helper.findByDataTestId("show-Role-Admin")).toBeInDOM();
        expect(helper.findByDataTestId("show-Role-Dev")).toBeInDOM();
      });

      it("should allow removing roles", () => {
        const role = "Admin";
        expect(stageConfig.approval().authorization()!.roles.list()).toEqual([]);

        const inputSelector = helper.findByDataTestId("Role-input").find("input");
        inputSelector.val(role);

        simulateEvent.simulate(inputSelector[0], "input");
        m.redraw.sync();

        const addBtnSelector = helper.findByDataTestId("Role-input").find("button");
        simulateEvent.simulate(addBtnSelector[0], "click");
        m.redraw.sync();

        expect(stageConfig.approval().authorization()!.roles.list()).toEqual([role]);
      });

      it("should allow removing role", () => {
        stageConfig.approval().authorization()!.roles.add("Admin");
        stageConfig.approval().authorization()!.roles.add("Dev");
        m.redraw.sync();

        expect(helper.findByDataTestId("show-Role-Admin")).toBeInDOM();
        expect(helper.findByDataTestId("show-Role-Dev")).toBeInDOM();

        simulateEvent.simulate(helper.findByDataTestId("Close-icon")[0], "click");
        m.redraw.sync();

        expect(helper.findByDataTestId("show-Role-Admin")).not.toBeInDOM();
        expect(helper.findByDataTestId("show-Role-Dev")).toBeInDOM();
      });
    });
  });

  afterEach(() => {
    helper.unmount();
  });
});
