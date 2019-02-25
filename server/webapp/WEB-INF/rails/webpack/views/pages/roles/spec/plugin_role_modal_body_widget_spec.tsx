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
import {AuthConfigs} from "models/auth_configs/auth_configs_new";
import {TestData} from "models/auth_configs/spec/test_data";
import {Role} from "models/roles/roles";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {AuthorizationPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import * as simulateEvent from "simulate-event";
import {TestHelper} from "views/pages/artifact_stores/spec/test_helper";
import {PluginRoleModalBodyWidget} from "views/pages/roles/role_modal_body_widget";
import {RolesTestData} from "views/pages/roles/spec/test_data";

describe("PluginRoleModalBodyWidget", () => {
  const pluginInfos = [
    PluginInfo.fromJSON(AuthorizationPluginInfo.ldap()),
    PluginInfo.fromJSON(AuthorizationPluginInfo.github())
  ];
  const authConfigs = AuthConfigs.fromJSON(
    TestData.authConfigList(TestData.ldapAuthConfig(), TestData.gitHubAuthConfig())
  );

  const helper = new TestHelper();

  afterEach((done) => helper.unmount(done));

  it("should render plugin role view", () => {
    mount();

    expect(helper.findByDataTestId("form-field-input-role-name")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-input-role-name").prop("readonly")).toBeFalsy();

    expect(helper.findByDataTestId("form-field-input-auth-config-id")).toBeInDOM();
    expect(helper.find(".plugin-view")).toBeInDOM();
    expect(helper.find(".plugin-view")).toContainText("This is ldap role config view.");
  });

  it("should disable role name when isNameDisabled is set to true", () => {
    mount(true);

    expect(helper.findByDataTestId("form-field-input-role-name")).toBeInDOM();
    expect(helper.findByDataTestId("form-field-input-role-name").prop("readonly")).toBeTruthy();
  });

  it("should change role view on change of auth config id", () => {
    mount();

    expect(helper.find(".plugin-view")).toContainText("This is ldap role config view.");

    const authConfigDropDown = helper.findByDataTestId("form-field-input-auth-config-id");
    authConfigDropDown.val("github");
    simulateEvent.simulate(authConfigDropDown.get(0), "change");
    //Had to do this as m.redraw() was't working :(
    helper.remount();

    expect(helper.find(".plugin-view")).toContainText("This is github role config view.");
  });

  function mount(isNameDisabled = false) {
    const role = Role.fromJSON(RolesTestData.LdapPluginRoleJSON());
    helper.mount(<PluginRoleModalBodyWidget isNameDisabled={isNameDisabled}
                                            role={role}
                                            authConfigs={authConfigs}
                                            pluginInfos={pluginInfos}/>);
  }
});
