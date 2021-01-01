/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {AuthConfigs} from "models/auth_configs/auth_configs";
import {TestData} from "models/auth_configs/spec/test_data";
import {GoCDRole, PluginRole, Role, Roles} from "models/roles/roles";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {AuthorizationPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import s from "underscore.string";
import {RolesWidget} from "views/pages/roles/roles_widget";
import {RolesTestData} from "views/pages/roles/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";

describe("RolesWidgetSpec", () => {
  const onEdit   = jasmine.createSpy("onEdit");
  const onClone  = jasmine.createSpy("onClone");
  const onDelete = jasmine.createSpy("onDelete");

  const roles       = Roles.fromJSON(RolesTestData.GetAllRoles());
  const authConfigs = AuthConfigs.fromJSON(TestData.authConfigList(TestData.ldapAuthConfig()));
  const pluginInfos = new PluginInfos(PluginInfo.fromJSON(AuthorizationPluginInfo.ldap()));
  const helper      = new TestHelper();

  afterEach((done) => helper.unmount(done));

  function mount(roles: Array<GoCDRole | PluginRole>,
                 authConfigs: AuthConfigs,
                 pluginInfos: PluginInfos) {
    helper.mount(() => <RolesWidget roles={roles}
                                    authConfigs={authConfigs}
                                    pluginInfos={pluginInfos}
                                    onEdit={onEdit}
                                    onClone={onClone}
                                    onDelete={onDelete}/>);
  }

  it("should render role info when no roles are configured", () => {
    mount(new Roles(), authConfigs, pluginInfos);

    const infoElements = helper.qa("li", helper.byTestId("role-config-info"));

    expect(helper.byTestId("role-widget")).toBeFalsy();
    expect(helper.byTestId("role-config-info")).toBeInDOM();
    expect(infoElements[0].textContent).toEqual("Click on \"Add\" to add new role configuration.");
    expect(infoElements[1].textContent).toEqual("A role configuration is used to define a group of users, along with the access permissions, who perform similar tasks. You can read more about roles based access control in GoCD from here.");
  });

  it("should show that no user is present and action buttons for gocd role with no users", () => {
    mount(new Roles(Role.fromJSON(RolesTestData.EmptyGoCDRoleJSON())), authConfigs, pluginInfos);

    expect(helper.textByTestId("key-value-key-name")).toContain("Name");
    expect(helper.textByTestId("key-value-value-name")).toContain("spacetiger");
    expect(helper.textByTestId("no-users-message")).toBe("No users in this role.");
    expect(helper.byTestId("role-edit")).toBeInDOM();
    expect(helper.byTestId("role-clone")).toBeInDOM();
    expect(helper.byTestId("role-delete")).toBeInDOM();

    expect(helper.byTestId("role-edit")).not.toBeDisabled();
    expect(helper.byTestId("role-clone")).not.toBeDisabled();
    expect(helper.byTestId("role-delete")).not.toBeDisabled();
  });

  it("should render user info and action buttons for gocd role", () => {
    mount(new Roles(Role.fromJSON(RolesTestData.GoCDRoleJSON())), authConfigs, pluginInfos);

    expect(helper.textByTestId("key-value-key-name")).toContain("Name");
    expect(helper.textByTestId("key-value-value-name")).toContain("spacetiger");
    expect(helper.textByTestId("collapse-body")).toContain("alice");
    expect(helper.textByTestId("collapse-body")).toContain("bob");
    expect(helper.textByTestId("collapse-body")).toContain("robin");
    expect(helper.byTestId("role-edit")).toBeInDOM();
    expect(helper.byTestId("role-clone")).toBeInDOM();
    expect(helper.byTestId("role-delete")).toBeInDOM();

    expect(helper.byTestId("role-edit")).not.toBeDisabled();
    expect(helper.byTestId("role-clone")).not.toBeDisabled();
    expect(helper.byTestId("role-delete")).not.toBeDisabled();
  });

  it("should render user info and action buttons for plugin role", () => {
    mount(new Roles(Role.fromJSON(RolesTestData.LdapPluginRoleJSON())), authConfigs, pluginInfos);

    expect(helper.textByTestId("key-value-key-name")).toContain("Name");
    expect(helper.textByTestId("key-value-value-name")).toContain("blackbird");
    expect(helper.textByTestId("key-value-key-auth-config-id")).toContain("Auth Config Id");
    expect(helper.textByTestId("key-value-value-auth-config-id")).toContain("ldap");
    expect(helper.textByTestId("key-value-key-plugin")).toContain("Plugin");
    expect(helper.textByTestId("key-value-value-plugin")).toContain("LDAP Authorization Plugin for GoCD");
    expect(helper.textByTestId("key-value-key-usergroupmembershipattribute")).toContain("UserGroupMembershipAttribute");
    expect(helper.textByTestId("key-value-value-usergroupmembershipattribute")).toContain("memberOf");
    expect(helper.textByTestId("key-value-key-groupidentifiers")).toContain("GroupIdentifiers");
    expect(helper.textByTestId("key-value-value-groupidentifiers")).toContain("ou=admins,ou=groups,ou=system,dc=example,dc=com");

    expect(helper.byTestId("role-edit")).toBeInDOM();
    expect(helper.byTestId("role-clone")).toBeInDOM();
    expect(helper.byTestId("role-delete")).toBeInDOM();

    expect(helper.byTestId("role-edit")).not.toBeDisabled();
    expect(helper.byTestId("role-clone")).not.toBeDisabled();
    expect(helper.byTestId("role-delete")).not.toBeDisabled();
  });

  it("should callback the edit function when edit button is clicked", () => {
    mount(roles, authConfigs, pluginInfos);

    helper.clickByTestId("role-edit");

    expect(onEdit).toHaveBeenCalledWith(roles[0], jasmine.any(Event));
  });

  it("should callback the clone function when clone button is clicked", () => {
    mount(roles, authConfigs, pluginInfos);

    helper.clickByTestId("role-clone");

    expect(onClone).toHaveBeenCalledWith(roles[0], jasmine.any(Event));
  });

  it("should callback the delete function when delete button is clicked", () => {
    mount(roles, authConfigs, pluginInfos);

    helper.clickByTestId("role-delete");

    expect(onDelete).toHaveBeenCalledWith(roles[0], jasmine.any(Event));
  });

  it("should disable edit & clone button when no authorization plugin installed.", () => {
    mount(new Roles(Role.fromJSON(RolesTestData.LdapPluginRoleJSON())), authConfigs, new PluginInfos());
    expect(helper.byTestId("role-edit")).toBeDisabled();
    expect(helper.byTestId("role-clone")).toBeDisabled();
    expect(helper.byTestId("role-delete")).not.toBeDisabled();
  });

  it("should only disable edit & clone button of auth config for which plugin is not installed", () => {
    const authConfigJson = TestData.authConfigList(TestData.gitHubAuthConfig(), TestData.ldapAuthConfig());
    const authConfigs    = AuthConfigs.fromJSON(authConfigJson);

    const githubRole = Role.fromJSON(RolesTestData.GitHubPluginRoleJSON());
    const ldapRole   = Role.fromJSON(RolesTestData.LdapPluginRoleJSON());
    mount(new Roles(githubRole, ldapRole), authConfigs, pluginInfos);

    const ldapRolePanel = helper.byTestId(`role-${s.slugify(ldapRole.name())}`);
    expect(helper.byTestId("role-edit", ldapRolePanel)).not.toBeDisabled();
    expect(helper.byTestId("role-clone", ldapRolePanel)).not.toBeDisabled();
    expect(helper.byTestId("role-delete", ldapRolePanel)).not.toBeDisabled();

    const githubRolePanel = helper.byTestId(`role-${s.slugify(githubRole.name())}`);
    expect(helper.byTestId("role-edit", githubRolePanel)).toBeDisabled();
    expect(helper.byTestId("role-clone", githubRolePanel)).toBeDisabled();
    expect(helper.byTestId("role-delete", githubRolePanel)).not.toBeDisabled();
  });

  it('should render policy info for gocd role', () => {
    mount(new Roles(Role.fromJSON(RolesTestData.GoCDRoleJSON())), authConfigs, pluginInfos);

    expect(helper.byTestId("policy-info")).toBeInDOM();
    const table = helper.byTestId("policy-table");

    const headerRow = helper.qa("th", helper.byTestId("table-header", table));
    expect(headerRow.length).toBe(4);
    expect(headerRow.item(0)).toContainText("Permission");
    expect(headerRow.item(1)).toContainText("Action");
    expect(headerRow.item(2)).toContainText("Type");
    expect(headerRow.item(3)).toContainText("Resource");

    const ruleBodyRow = helper.qa("td", helper.byTestId("table-body", table));
    expect(ruleBodyRow.length).toBe(4);
    expect(ruleBodyRow.item(0)).toContainText("allow");
    expect(ruleBodyRow.item(1)).toContainText("view");
    expect(ruleBodyRow.item(2)).toContainText("environment");
    expect(ruleBodyRow.item(3)).toContainText("*");
  });

  it('should render policy info for plugin role', () => {
    mount(new Roles(Role.fromJSON(RolesTestData.LdapPluginRoleJSON())), authConfigs, pluginInfos);

    expect(helper.byTestId("policy-info")).toBeInDOM();
    const table = helper.byTestId("policy-table");

    const headerRow = helper.qa("th", helper.byTestId("table-header", table));
    expect(headerRow.length).toBe(4);
    expect(headerRow.item(0)).toContainText("Permission");
    expect(headerRow.item(1)).toContainText("Action");
    expect(headerRow.item(2)).toContainText("Type");
    expect(headerRow.item(3)).toContainText("Resource");

    const ruleBodyRow = helper.qa("td", helper.byTestId("table-body", table));
    expect(ruleBodyRow.length).toBe(4);
    expect(ruleBodyRow.item(0)).toContainText("deny");
    expect(ruleBodyRow.item(1)).toContainText("view");
    expect(ruleBodyRow.item(2)).toContainText("environment");
    expect(ruleBodyRow.item(3)).toContainText("*");
  });
});
