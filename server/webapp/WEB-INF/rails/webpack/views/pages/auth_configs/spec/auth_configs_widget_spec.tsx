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
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {AuthorizationPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import * as simulateEvent from "simulate-event";
import {TestHelper} from "views/pages/spec/test_helper";
import {AuthConfigsWidget} from "views/pages/auth_configs/auth_configs_widget";

describe("AuthorizationConfigurationWidget", () => {
  const onEdit   = jasmine.createSpy("onEdit");
  const onClone  = jasmine.createSpy("onClone");
  const onDelete = jasmine.createSpy("onDelete");

  const authConfigs = AuthConfigs.fromJSON(TestData.authConfigList(TestData.ldapAuthConfig()));
  const pluginInfos = [PluginInfo.fromJSON(AuthorizationPluginInfo.ldap())];

  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  it("should render info message when authorization plugins are not installed on the server", () => {
    mount(new AuthConfigs(), []);
    expect(helper.findByDataTestId("flash-message-info")).toContainText("No authorization plugin installed.");
  });

  it("should render id info and action buttons", () => {
    mount(authConfigs, pluginInfos);
    expect(helper.findByDataTestId("key-value-key-id")).toContainText("Id");
    expect(helper.findByDataTestId("key-value-value-id")).toContainText("ldap");
    expect(helper.findByDataTestId("key-value-key-plugin-id")).toContainText("Plugin Id");
    expect(helper.findByDataTestId("key-value-value-plugin-id")).toContainText("cd.go.authorization.ldap");
    expect(helper.findByDataTestId("auth-config-edit")).toBeInDOM();
    expect(helper.findByDataTestId("auth-config-clone")).toBeInDOM();
    expect(helper.findByDataTestId("auth-config-delete")).toBeInDOM();

    expect(helper.findByDataTestId("auth-config-edit")).not.toBeDisabled();
    expect(helper.findByDataTestId("auth-config-clone")).not.toBeDisabled();
    expect(helper.findByDataTestId("auth-config-delete")).not.toBeDisabled();
  });

  it("should disable edit & clone button when plugin is not installed", () => {
    mount(authConfigs, []);
    expect(helper.findByDataTestId("auth-config-edit")).toBeDisabled();
    expect(helper.findByDataTestId("auth-config-clone")).toBeDisabled();
    expect(helper.findByDataTestId("auth-config-delete")).not.toBeDisabled();
  });

  it("should render auth config properties", () => {
    mount(authConfigs, pluginInfos);
    expect(helper.findByDataTestId("key-value-key-url")).toContainText("Url");
    expect(helper.findByDataTestId("key-value-key-managerdn")).toContainText("ManagerDN");
    expect(helper.findByDataTestId("key-value-key-password")).toContainText("Password");
    expect(helper.findByDataTestId("key-value-value-url")).toContainText("ldap://ldap.server.url");
    expect(helper.findByDataTestId("key-value-value-managerdn")).toContainText("uid=admin,ou=system");
    expect(helper.findByDataTestId("key-value-value-password")).toContainText("************");
  });

  it("should callback the edit function when edit button is clicked", () => {
    mount(authConfigs, pluginInfos);
    simulateEvent.simulate(helper.findByDataTestId("auth-config-edit").get(0), "click");

    expect(onEdit).toHaveBeenCalledWith(authConfigs[0], jasmine.any(Event));
  });

  it("should callback the clone function when clone button is clicked", () => {
    mount(authConfigs, pluginInfos);
    simulateEvent.simulate(helper.findByDataTestId("auth-config-clone").get(0), "click");

    expect(onClone).toHaveBeenCalledWith(authConfigs[0], jasmine.any(Event));
  });

  it("should callback the delete function when delete button is clicked", () => {
    mount(authConfigs, pluginInfos);
    simulateEvent.simulate(helper.findByDataTestId("auth-config-delete").get(0), "click");

    expect(onDelete).toHaveBeenCalledWith(authConfigs[0], jasmine.any(Event));
  });

  function mount(authConfigs: AuthConfigs, pluginInfos: Array<PluginInfo<any>>) {
    helper.mount(() => <AuthConfigsWidget authConfigs={authConfigs}
                                          pluginInfos={stream(pluginInfos)}
                                          onEdit={onEdit}
                                          onClone={onClone}
                                          onDelete={onDelete}/>);
  }
});
