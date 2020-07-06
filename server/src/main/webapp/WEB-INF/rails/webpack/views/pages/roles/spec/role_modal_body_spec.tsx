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
import {AuthConfigs} from "models/auth_configs/auth_configs";
import {TestData} from "models/auth_configs/spec/test_data";
import {Role, RoleType} from "models/roles/roles";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {AuthorizationPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import {Action, RoleModalBody} from "views/pages/roles/role_modal_body";
import {RolesTestData} from "views/pages/roles/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";

describe("RoleModalBody", () => {
  const gocdRole       = Role.fromJSON(RolesTestData.GoCDRoleJSON());
  const pluginInfos    = new PluginInfos(PluginInfo.fromJSON(AuthorizationPluginInfo.ldap()));
  const authConfigs    = AuthConfigs.fromJSON(TestData.authConfigList(TestData.ldapAuthConfig()));
  const changeRoleType = jasmine.createSpy("changeRoleType");

  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  describe("ErrorMessage", () => {
    it("should show error message", () => {
      helper.mount(() => <RoleModalBody role={Stream(gocdRole)}
                                        message="Something went wrong contact your admin!!!"
                                        pluginInfos={pluginInfos}
                                        authConfigs={authConfigs}
                                        action={Action.NEW}
                                        changeRoleType={changeRoleType}
                                        resourceAutocompleteHelper={new Map()}/>);
      expect(helper.textByTestId("flash-message-alert")).toBe("Something went wrong contact your admin!!!");
    });

    it("should not show error message when one is not present", () => {
      helper.mount(() => <RoleModalBody role={Stream(gocdRole)}
                                        pluginInfos={pluginInfos}
                                        authConfigs={authConfigs}
                                        action={Action.NEW}
                                        changeRoleType={changeRoleType}
                                        resourceAutocompleteHelper={new Map()}/>);

      expect(helper.byTestId("flash-message-alert")).toBeFalsy();
    });
  });

  describe("RoleTypeSelector", () => {
    it("should show role type selector", () => {
      helper.mount(() => <RoleModalBody role={Stream(gocdRole)}
                                        action={Action.NEW}
                                        pluginInfos={pluginInfos}
                                        authConfigs={authConfigs}
                                        changeRoleType={changeRoleType}
                                        resourceAutocompleteHelper={new Map()}/>);

      expect(helper.byTestId("role-type-selector")).toBeInDOM();
    });

    it("should not show role type selector for clone action", () => {
      helper.mount(() => <RoleModalBody role={Stream(gocdRole)}
                                        pluginInfos={pluginInfos}
                                        authConfigs={authConfigs}
                                        action={Action.CLONE}
                                        changeRoleType={changeRoleType}
                                        resourceAutocompleteHelper={new Map()}/>);

      expect(helper.byTestId("role-type-selector")).toBeFalsy();
    });

    it("should not show role type selector for edit action", () => {
      helper.mount(() => <RoleModalBody role={Stream(gocdRole)}
                                        pluginInfos={pluginInfos}
                                        authConfigs={authConfigs}
                                        action={Action.EDIT}
                                        changeRoleType={changeRoleType}
                                        resourceAutocompleteHelper={new Map()}/>);

      expect(helper.byTestId("role-type-selector")).toBeFalsy();
    });

  });

  describe("DisablePluginRoleType", () => {
    it("should disable when no auth configs defined", () => {
      helper.mount(() => <RoleModalBody role={Stream(gocdRole)}
                                        action={Action.NEW}
                                        pluginInfos={pluginInfos}
                                        authConfigs={new AuthConfigs()}
                                        changeRoleType={changeRoleType}
                                        resourceAutocompleteHelper={new Map()}/>);
      expect(helper.q("#plugin-role")).toBeInDOM();
      expect(helper.q("#plugin-role")).toBeDisabled();
      expect(helper.byTestId("tooltip-wrapper")).toBeInDOM();
      expect(helper.textByTestId("tooltip-content")).toBe('Either no plugin has authorization capability or there are no authorization configs defined for the same.');
    });

    it("should enable when auth configs are defined", () => {
      helper.mount(() => <RoleModalBody role={Stream(gocdRole)}
                                        action={Action.NEW}
                                        pluginInfos={pluginInfos}
                                        authConfigs={authConfigs}
                                        changeRoleType={changeRoleType}
                                        resourceAutocompleteHelper={new Map()}/>);
      expect(helper.q("#plugin-role")).toBeInDOM();
      expect(helper.q("#plugin-role")).not.toBeDisabled();
    });

  });

  describe("ChangeRoleType", () => {
    it("should change role type on click of radio button", () => {
      helper.mount(() => <RoleModalBody role={Stream(gocdRole)}
                                        action={Action.NEW}
                                        pluginInfos={pluginInfos}
                                        authConfigs={authConfigs}
                                        changeRoleType={changeRoleType}
                                        resourceAutocompleteHelper={new Map()}/>);

      expect(helper.q("#plugin-role")).toBeInDOM();
      expect(helper.q("#plugin-role")).not.toBeDisabled();

      helper.click("#plugin-role");
      expect(changeRoleType).toHaveBeenCalledWith(RoleType.plugin, jasmine.any(Event));

      helper.click("#core-role");
      expect(changeRoleType).toHaveBeenCalledWith(RoleType.gocd, jasmine.any(Event));
    });
  });

  describe("Policy", () => {
    it("should callback the add function when add new permission is clicked", () => {
      helper.mount(() => <RoleModalBody role={Stream(gocdRole)}
                                        action={Action.NEW}
                                        pluginInfos={pluginInfos}
                                        authConfigs={authConfigs}
                                        changeRoleType={changeRoleType}
                                        resourceAutocompleteHelper={new Map()}/>);

      expect(helper.qa("tr", helper.byTestId("table-body")).length).toBe(1);

      helper.clickByTestId("add-permission-button");

      expect(helper.qa("tr", helper.byTestId("table-body")).length).toBe(2);
    });
  });

});
