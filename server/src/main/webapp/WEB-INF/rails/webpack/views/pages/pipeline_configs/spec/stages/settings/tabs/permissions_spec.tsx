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
    expect(helper.byTestId("stage-permissions-tab", document.body)).toBeInDOM();
  });

  it("should render the default permission message", () => {
    const msg      = "All System Administrators and Pipeline Group Administrators can operate on this pipeline.";
    const helpText = "(This permission can not be overridden!)";

    expect(helper.textByTestId("default-permission-message-body")).toContain(msg);
    expect(helper.textByTestId("default-permission-help-message")).toContain(helpText);
  });

  it("should render Inherit permissions from Pipeline Group checkbox", () => {
    expect(helper.byTestId("switch-wrapper")).toBeInDOM();
    expect(helper.textByTestId("switch-label")).toContain("Inherit permissions from Pipeline Group:");

    expect(checked("switch-checkbox")).toBe(true);
  });

  it("should allow toggling Inherit permissions from Pipeline Group checkbox", () => {
    expect(helper.byTestId("switch-wrapper")).toBeInDOM();
    expect(helper.textByTestId("switch-label")).toContain("Inherit permissions from Pipeline Group:");

    expect(checked("switch-checkbox")).toBe(true);
    expect(stageConfig.approval().inheritFromPipelineGroup()).toEqual(true);

    click("switch-checkbox");

    expect(checked("switch-checkbox")).toBe(false);
    expect(stageConfig.approval().inheritFromPipelineGroup()).toEqual(false);
  });

  it("should not render Specify Permissions locally when permissions are inherited from pipeline group", () => {
    expect(checked("switch-checkbox")).toBe(true);
    expect(helper.byTestId("specify-stage-permissions-locally")).toBeFalsy();
  });

  it("should render Specify Permissions locally when permissions are not inherited from pipeline group", () => {
    stageConfig.approval().inheritFromPipelineGroup(false);
    m.redraw.sync();

    expect(checked("switch-checkbox")).toBe(false);
    expect(helper.byTestId("specify-stage-permissions-locally")).toBeInDOM();
  });

  describe("Specify Stage Permissions Locally", () => {
    beforeEach(() => {
      stageConfig.approval().inheritFromPipelineGroup(false);
      m.redraw.sync();
    });

    it("should render specify locally message", () => {
      expect(helper.byTestId("specify-stage-permissions-locally")).toBeInDOM();
      expect(helper.textByTestId("specify-stage-permissions-locally")).toContain("Specify locally:");
    });

    describe("Users", () => {
      it("should render user input field", () => {
        expect(helper.byTestId("User-permissions")).toBeInDOM();
        expect(helper.textByTestId("User-permissions")).toContain("Users");
        expect(helper.byTestId("User-input")).toBeInDOM();
      });

      it("should show existing users on the UI", () => {
        stageConfig.approval().authorization()!.users.add("Bob");
        stageConfig.approval().authorization()!.users.add("John");
        m.redraw.sync();

        expect(helper.byTestId("show-User-Bob")).toBeInDOM();
        expect(helper.byTestId("show-User-John")).toBeInDOM();
      });

      it("should allow adding users", () => {
        const username = "Bob";
        expect(stageConfig.approval().authorization()!.users.list()).toEqual([]);

        helper.oninput("input", username, helper.byTestId("User-input"));
        helper.click("button", helper.byTestId("User-input"));

        expect(stageConfig.approval().authorization()!.users.list()).toEqual([username]);
      });

      it("should allow removing user", () => {
        stageConfig.approval().authorization()!.users.add("Bob");
        stageConfig.approval().authorization()!.users.add("John");
        m.redraw.sync();

        expect(helper.byTestId("show-User-Bob")).toBeInDOM();
        expect(helper.byTestId("show-User-John")).toBeInDOM();

        helper.clickByTestId("Close-icon");

        expect(helper.byTestId("show-User-Bob")).toBeFalsy();
        expect(helper.byTestId("show-User-John")).toBeInDOM();
      });
    });

    describe("Roles", () => {
      it("should render role input field", () => {
        expect(helper.byTestId("Role-permissions")).toBeInDOM();
        expect(helper.textByTestId("Role-permissions")).toContain("Roles");
        expect(helper.byTestId("User-input")).toBeInDOM();
      });

      it("should show existing roles on the UI", () => {
        stageConfig.approval().authorization()!.roles.add("Admin");
        stageConfig.approval().authorization()!.roles.add("Dev");
        m.redraw.sync();

        expect(helper.byTestId("show-Role-Admin")).toBeInDOM();
        expect(helper.byTestId("show-Role-Dev")).toBeInDOM();
      });

      it("should allow removing roles", () => {
        const role = "Admin";
        expect(stageConfig.approval().authorization()!.roles.list()).toEqual([]);

        helper.oninput("input", role, helper.byTestId("Role-input"));
        helper.click("button", helper.byTestId("Role-input"));

        expect(stageConfig.approval().authorization()!.roles.list()).toEqual([role]);
      });

      it("should allow removing role", () => {
        stageConfig.approval().authorization()!.roles.add("Admin");
        stageConfig.approval().authorization()!.roles.add("Dev");
        m.redraw.sync();

        expect(helper.byTestId("show-Role-Admin")).toBeInDOM();
        expect(helper.byTestId("show-Role-Dev")).toBeInDOM();

        helper.clickByTestId("Close-icon");

        expect(helper.byTestId("show-Role-Admin")).toBeFalsy();
        expect(helper.byTestId("show-Role-Dev")).toBeInDOM();
      });
    });
  });

  afterEach(() => {
    helper.unmount();
  });

  function click(id: string) {
    (helper.byTestId(id) as HTMLElement).click();
    m.redraw.sync();
  }

  function checked(id: string) {
    return (helper.byTestId(id) as HTMLInputElement).checked;
  }
});
