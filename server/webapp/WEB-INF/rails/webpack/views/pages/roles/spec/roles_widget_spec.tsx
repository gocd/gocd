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
import {AuthConfigs} from "models/auth_configs/auth_configs";
import {TestData} from "models/auth_configs/spec/test_data";
import {GoCDRole, PluginRole, Role, Roles} from "models/roles/roles";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {AuthorizationPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import * as simulateEvent from "simulate-event";
import * as s from "underscore.string";
import {TestHelper} from "views/pages/artifact_stores/spec/test_helper";
import {RolesWidget} from "views/pages/roles/roles_widget";
import {RolesTestData} from "views/pages/roles/spec/test_data";

describe("RolesWidgetSpec", () => {
  const onEdit   = jasmine.createSpy("onEdit");
  const onClone  = jasmine.createSpy("onClone");
  const onDelete = jasmine.createSpy("onDelete");

  const roles       = Roles.fromJSON(RolesTestData.GetAllRoles());
  const authConfigs = AuthConfigs.fromJSON(TestData.authConfigList(TestData.ldapAuthConfig()));
  const pluginInfos = [PluginInfo.fromJSON(AuthorizationPluginInfo.ldap())];
  const helper      = new TestHelper();

  afterEach((done) => helper.unmount(done));

  function mount(roles: Array<GoCDRole | PluginRole>,
                 authConfigs: AuthConfigs,
                 pluginInfos: Array<PluginInfo<Extension>>) {
    helper.mount(() => <RolesWidget roles={roles}
                              authConfigs={authConfigs}
                              pluginInfos={pluginInfos}
                              onEdit={onEdit}
                              onClone={onClone}
                              onDelete={onDelete}/>);
  }

  it("should show that no user is present and action buttons for gocd role with no users", () => {
    mount(new Roles(Role.fromJSON(RolesTestData.EmptyGoCDRoleJSON())), authConfigs, pluginInfos);

    expect(helper.findByDataTestId("key-value-key-name")).toContainText("Name");
    expect(helper.findByDataTestId("key-value-value-name").get(0)).toContainText("spacetiger");
    expect(helper.findByDataTestId("no-users-message").text()).toBe("No users in this role.");
    expect(helper.findByDataTestId("role-edit")).toBeInDOM();
    expect(helper.findByDataTestId("role-clone")).toBeInDOM();
    expect(helper.findByDataTestId("role-delete")).toBeInDOM();

    expect(helper.findByDataTestId("role-edit")).not.toBeDisabled();
    expect(helper.findByDataTestId("role-clone")).not.toBeDisabled();
    expect(helper.findByDataTestId("role-delete")).not.toBeDisabled();
  });

  it("should render user info and action buttons for gocd role", () => {
    mount(new Roles(Role.fromJSON(RolesTestData.GoCDRoleJSON())), authConfigs, pluginInfos);

    expect(helper.findByDataTestId("key-value-key-name")).toContainText("Name");
    expect(helper.findByDataTestId("key-value-value-name").get(0)).toContainText("spacetiger");
    expect(helper.findByDataTestId("collapse-body").get(0)).toContainText("alice");
    expect(helper.findByDataTestId("collapse-body").get(0)).toContainText("bob");
    expect(helper.findByDataTestId("collapse-body").get(0)).toContainText("robin");
    expect(helper.findByDataTestId("role-edit")).toBeInDOM();
    expect(helper.findByDataTestId("role-clone")).toBeInDOM();
    expect(helper.findByDataTestId("role-delete")).toBeInDOM();

    expect(helper.findByDataTestId("role-edit")).not.toBeDisabled();
    expect(helper.findByDataTestId("role-clone")).not.toBeDisabled();
    expect(helper.findByDataTestId("role-delete")).not.toBeDisabled();
  });

  it("should render user info and action buttons for plugin role", () => {
    mount(new Roles(Role.fromJSON(RolesTestData.LdapPluginRoleJSON())), authConfigs, pluginInfos);

    expect(helper.findByDataTestId("key-value-key-name")).toContainText("Name");
    expect(helper.findByDataTestId("key-value-value-name").get(0)).toContainText("blackbird");
    expect(helper.findByDataTestId("key-value-key-auth-config-id")).toContainText("Auth Config Id");
    expect(helper.findByDataTestId("key-value-value-auth-config-id")).toContainText("ldap");
    expect(helper.findByDataTestId("key-value-key-plugin")).toContainText("Plugin");
    expect(helper.findByDataTestId("key-value-value-plugin")).toContainText("LDAP Authorization Plugin for GoCD");
    expect(helper.findByDataTestId("key-value-key-usergroupmembershipattribute"))
      .toContainText("UserGroupMembershipAttribute");
    expect(helper.findByDataTestId("key-value-value-usergroupmembershipattribute")).toContainText("memberOf");
    expect(helper.findByDataTestId("key-value-key-groupidentifiers")).toContainText("GroupIdentifiers");
    expect(helper.findByDataTestId("key-value-value-groupidentifiers"))
      .toContainText("ou=admins,ou=groups,ou=system,dc=example,dc=com");

    expect(helper.findByDataTestId("role-edit")).toBeInDOM();
    expect(helper.findByDataTestId("role-clone")).toBeInDOM();
    expect(helper.findByDataTestId("role-delete")).toBeInDOM();

    expect(helper.findByDataTestId("role-edit")).not.toBeDisabled();
    expect(helper.findByDataTestId("role-clone")).not.toBeDisabled();
    expect(helper.findByDataTestId("role-delete")).not.toBeDisabled();
  });

  it("should callback the edit function when edit button is clicked", () => {
    mount(roles, authConfigs, pluginInfos);

    simulateEvent.simulate(helper.findByDataTestId("role-edit").get(0), "click");

    expect(onEdit).toHaveBeenCalledWith(roles[0], jasmine.any(Event));
  });

  it("should callback the clone function when clone button is clicked", () => {
    mount(roles, authConfigs, pluginInfos);

    simulateEvent.simulate(helper.findByDataTestId("role-clone").get(0), "click");

    expect(onClone).toHaveBeenCalledWith(roles[0], jasmine.any(Event));
  });

  it("should callback the delete function when delete button is clicked", () => {
    mount(roles, authConfigs, pluginInfos);

    simulateEvent.simulate(helper.findByDataTestId("role-delete").get(0), "click");

    expect(onDelete).toHaveBeenCalledWith(roles[0], jasmine.any(Event));
  });

  it("should disable edit & clone button when no authorization plugin installed.", () => {
    mount(new Roles(Role.fromJSON(RolesTestData.LdapPluginRoleJSON())), authConfigs, []);
    expect(helper.findByDataTestId("role-edit")).toBeDisabled();
    expect(helper.findByDataTestId("role-clone")).toBeDisabled();
    expect(helper.findByDataTestId("role-delete")).not.toBeDisabled();
  });

  it("should only disable edit & clone button of auth config for which plugin is not installed", () => {
    const authConfigJson = TestData.authConfigList(TestData.gitHubAuthConfig(), TestData.ldapAuthConfig());
    const authConfigs    = AuthConfigs.fromJSON(authConfigJson);

    const githubRole = Role.fromJSON(RolesTestData.GitHubPluginRoleJSON());
    const ldapRole   = Role.fromJSON(RolesTestData.LdapPluginRoleJSON());
    mount(new Roles(githubRole, ldapRole), authConfigs, pluginInfos);

    const ldapRolePanel = helper.findByDataTestId(`role-${s.slugify(ldapRole.name())}`);
    expect(helper.findIn(ldapRolePanel, "role-edit")).not.toBeDisabled();
    expect(helper.findIn(ldapRolePanel, "role-clone")).not.toBeDisabled();
    expect(helper.findIn(ldapRolePanel, "role-delete")).not.toBeDisabled();

    const githubRolePanel = helper.findByDataTestId(`role-${s.slugify(githubRole.name())}`);
    expect(helper.findIn(githubRolePanel, "role-edit")).toBeDisabled();
    expect(helper.findIn(githubRolePanel, "role-clone")).toBeDisabled();
    expect(helper.findIn(githubRolePanel, "role-delete")).not.toBeDisabled();
  });
});
