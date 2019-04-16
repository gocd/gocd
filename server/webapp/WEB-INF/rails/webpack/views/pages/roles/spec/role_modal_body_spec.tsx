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

import * as m from "mithril";
import * as stream from "mithril/stream";
import {AuthConfigs} from "models/auth_configs/auth_configs";
import {TestData} from "models/auth_configs/spec/test_data";
import {Role, RoleType} from "models/roles/roles";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {AuthorizationPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import * as simulateEvent from "simulate-event";
import {TestHelper} from "views/pages/spec/test_helper";
import {Action, RoleModalBody} from "views/pages/roles/role_modal_body";
import {RolesTestData} from "views/pages/roles/spec/test_data";

describe("RoleModalBody", () => {
  const gocdRole       = Role.fromJSON(RolesTestData.GoCDRoleJSON());
  const pluginInfos    = [PluginInfo.fromJSON(AuthorizationPluginInfo.ldap())];
  const authConfigs    = AuthConfigs.fromJSON(TestData.authConfigList(TestData.ldapAuthConfig()));
  const changeRoleType = jasmine.createSpy("changeRoleType");

  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  describe("ErrorMessage", () => {
    it("should show error message", () => {
      helper.mount(() => <RoleModalBody role={stream(gocdRole)}
                                        message="Something went wrong contact your admin!!!"
                                        pluginInfos={pluginInfos}
                                        authConfigs={authConfigs}
                                        action={Action.NEW}
                                        changeRoleType={changeRoleType}/>);
      expect(helper.findByDataTestId("flash-message-alert").text()).toEqual("Something went wrong contact your admin!!!");
    });

    it("should not show error message when one is not present", () => {
      helper.mount(() => <RoleModalBody role={stream(gocdRole)}
                                        pluginInfos={pluginInfos}
                                        authConfigs={authConfigs}
                                        action={Action.NEW}
                                        changeRoleType={changeRoleType}/>);

      expect(helper.findByDataTestId("flash-message-alert").get(0)).not.toBeInDOM();
    });
  });

  describe("RoleTypeSelector", () => {
    it("should show role type selector", () => {
      helper.mount(() => <RoleModalBody role={stream(gocdRole)}
                                        action={Action.NEW}
                                        pluginInfos={pluginInfos}
                                        authConfigs={authConfigs}
                                        changeRoleType={changeRoleType}/>);

      expect(helper.findByDataTestId("role-type-selector").get(0)).toBeInDOM();
    });

    it("should not show role type selector for clone action", () => {
      helper.mount(() => <RoleModalBody role={stream(gocdRole)}
                                        pluginInfos={pluginInfos}
                                        authConfigs={authConfigs}
                                        action={Action.CLONE}
                                        changeRoleType={changeRoleType}/>);

      expect(helper.findByDataTestId("role-type-selector").get(0)).not.toBeInDOM();
    });

    it("should not show role type selector for edit action", () => {
      helper.mount(() => <RoleModalBody role={stream(gocdRole)}
                                        pluginInfos={pluginInfos}
                                        authConfigs={authConfigs}
                                        action={Action.EDIT}
                                        changeRoleType={changeRoleType}/>);

      expect(helper.findByDataTestId("role-type-selector").get(0)).not.toBeInDOM();
    });

  });

  describe("DisablePluginRoleType", () => {
    it("should disable when no auth configs defined", () => {
      helper.mount(() => <RoleModalBody role={stream(gocdRole)}
                                        action={Action.NEW}
                                        pluginInfos={pluginInfos}
                                        authConfigs={new AuthConfigs()}
                                        changeRoleType={changeRoleType}/>);
      expect(helper.find("#plugin-role").get(0)).toBeInDOM();
      expect(helper.find("#plugin-role").get(0)).toBeDisabled();
    });

    it("should enable when auth configs are defined", () => {
      helper.mount(() => <RoleModalBody role={stream(gocdRole)}
                                        action={Action.NEW}
                                        pluginInfos={pluginInfos}
                                        authConfigs={authConfigs}
                                        changeRoleType={changeRoleType}/>);
      expect(helper.find("#plugin-role").get(0)).toBeInDOM();
      expect(helper.find("#plugin-role").get(0)).not.toBeDisabled();
    });

  });

  describe("ChangeRoleType", () => {
    it("should change role type on click of radio button", () => {
      helper.mount(() => <RoleModalBody role={stream(gocdRole)}
                                        action={Action.NEW}
                                        pluginInfos={pluginInfos}
                                        authConfigs={authConfigs}
                                        changeRoleType={changeRoleType}/>);

      expect(helper.find("#plugin-role").get(0)).toBeInDOM();
      expect(helper.find("#plugin-role").get(0)).not.toBeDisabled();

      simulateEvent.simulate(helper.find("#plugin-role").get(0), "click");
      expect(changeRoleType).toHaveBeenCalledWith(RoleType.plugin, jasmine.any(Event));

      simulateEvent.simulate(helper.find("#core-role").get(0), "click");
      expect(changeRoleType).toHaveBeenCalledWith(RoleType.gocd, jasmine.any(Event));
    });
  });

});
