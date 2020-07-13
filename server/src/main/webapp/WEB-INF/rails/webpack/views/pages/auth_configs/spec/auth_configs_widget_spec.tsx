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

import {docsUrl} from "gen/gocd_version";
import m from "mithril";
import Stream from "mithril/stream";
import {AuthConfigs} from "models/auth_configs/auth_configs";
import {TestData} from "models/auth_configs/spec/test_data";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {AuthorizationPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import {AuthConfigsWidget} from "views/pages/auth_configs/auth_configs_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("AuthorizationConfigurationWidget", () => {
  const onEdit   = jasmine.createSpy("onEdit");
  const onClone  = jasmine.createSpy("onClone");
  const onDelete = jasmine.createSpy("onDelete");

  const authConfigs = AuthConfigs.fromJSON(TestData.authConfigList(TestData.ldapAuthConfig()));
  const pluginInfos = new PluginInfos(PluginInfo.fromJSON(AuthorizationPluginInfo.ldap()));

  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  it("should render info message when authorization plugins are not installed on the server", () => {
    mount(new AuthConfigs(), new PluginInfos());
    expect(helper.textByTestId("flash-message-info")).toContain("No authorization plugin installed.");
  });

  it("should render information when no auth config has been configured", () => {
    mount(new AuthConfigs(), pluginInfos);

    expect(helper.byTestId("auth-config-widget")).toBeFalsy();
    const helpElement = helper.byTestId("auth-config-info");
    expect(helpElement).toBeInDOM();
    expect(helpElement.textContent).toBe('Click on "Add" to add new authorization configuration."Authorization configuration" is the term used in GoCD for the configuration which allows a GoCD administrator to configure the kind of authentication and authorization used by it.GoCD can be setup to use multiple authorization configurations at the same time.An auth configuration can be used to setup user authorization. You can read more about authorization in GoCD from here.');
    expect(helper.q('a', helpElement)).toHaveAttr('href', docsUrl("configuration/dev_authentication.html"));
  });

  it("should render id info and action buttons", () => {
    mount(authConfigs, pluginInfos);
    expect(helper.textByTestId("key-value-key-id")).toContain("Id");
    expect(helper.textByTestId("key-value-value-id")).toContain("ldap");
    expect(helper.textByTestId("key-value-key-plugin-id")).toContain("Plugin Id");
    expect(helper.textByTestId("key-value-value-plugin-id")).toContain("cd.go.authorization.ldap");
    expect(helper.byTestId("auth-config-edit")).toBeInDOM();
    expect(helper.byTestId("auth-config-clone")).toBeInDOM();
    expect(helper.byTestId("auth-config-delete")).toBeInDOM();

    expect(helper.byTestId("auth-config-edit")).not.toBeDisabled();
    expect(helper.byTestId("auth-config-clone")).not.toBeDisabled();
    expect(helper.byTestId("auth-config-delete")).not.toBeDisabled();
  });

  it("should disable edit & clone button when plugin is not installed", () => {
    mount(authConfigs, new PluginInfos());
    expect(helper.byTestId("auth-config-edit")).toBeDisabled();
    expect(helper.byTestId("auth-config-clone")).toBeDisabled();
    expect(helper.byTestId("auth-config-delete")).not.toBeDisabled();
  });

  it("should render auth config properties", () => {
    mount(authConfigs, pluginInfos);
    expect(helper.textByTestId("key-value-key-url")).toContain("Url");
    expect(helper.textByTestId("key-value-key-managerdn")).toContain("ManagerDN");
    expect(helper.textByTestId("key-value-key-password")).toContain("Password");
    expect(helper.textByTestId("key-value-value-url")).toContain("ldap://ldap.server.url");
    expect(helper.textByTestId("key-value-value-managerdn")).toContain("uid=admin,ou=system");
    expect(helper.textByTestId("key-value-value-password")).toContain("************");
  });

  it("should callback the edit function when edit button is clicked", () => {
    mount(authConfigs, pluginInfos);
    helper.clickByTestId("auth-config-edit");

    expect(onEdit).toHaveBeenCalledWith(authConfigs[0], jasmine.any(Event));
  });

  it("should callback the clone function when clone button is clicked", () => {
    mount(authConfigs, pluginInfos);
    helper.clickByTestId("auth-config-clone");

    expect(onClone).toHaveBeenCalledWith(authConfigs[0], jasmine.any(Event));
  });

  it("should callback the delete function when delete button is clicked", () => {
    mount(authConfigs, pluginInfos);
    helper.clickByTestId("auth-config-delete");

    expect(onDelete).toHaveBeenCalledWith(authConfigs[0], jasmine.any(Event));
  });

  function mount(authConfigs: AuthConfigs, pluginInfos: PluginInfos) {
    helper.mount(() => <AuthConfigsWidget authConfigs={authConfigs}
                                          pluginInfos={Stream(pluginInfos)}
                                          onEdit={onEdit}
                                          onClone={onClone}
                                          onDelete={onDelete}/>);
  }
});
