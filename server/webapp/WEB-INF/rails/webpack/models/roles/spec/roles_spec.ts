/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import {
  GoCDRole,
  PluginRole,
  Role, RoleType
} from "models/roles/roles_new";
import {RolesTestData} from "views/pages/roles/spec/test_data";

describe("RoleModel", () => {
  it("should deserialize gocd role json", () => {
    const goCDRoleJSON = RolesTestData.GoCDRoleJSON();
    const goCDRole     = Role.fromJSON(goCDRoleJSON) as GoCDRole;

    expect(goCDRole.name()).toEqual("spacetiger");
    expect(goCDRole.type()).toEqual(RoleType.gocd);
    expect(goCDRole.attributes().users).toHaveLength(3);
    expect(goCDRole.attributes().users[0]).toEqual("alice");
  });

  it("should deserialize plugin role json", () => {
    const pluginRoleJSON = RolesTestData.LdapPluginRoleJSON();
    const pluginRole     = Role.fromJSON(pluginRoleJSON) as PluginRole;

    expect(pluginRole.name()).toEqual("blackbird");
    expect(pluginRole.type()).toEqual(RoleType.plugin);
    expect(pluginRole.attributes().authConfigId).toEqual("ldap");
    expect(pluginRole.attributes().properties()).toHaveLength(2);
    expect(pluginRole.attributes().properties()[0].key).toEqual("UserGroupMembershipAttribute");
    expect(pluginRole.attributes().properties()[0].displayValue()).toEqual("memberOf");
  });

  it("should validate the presence of name", () => {
    const goCDRoleJSON = RolesTestData.GoCDRoleJSON();
    delete goCDRoleJSON.name;
    const goCDRole = Role.fromJSON(goCDRoleJSON);

    const isValid = goCDRole.isValid();
    expect(isValid).toBe(false);
    expect(goCDRole.errors().count()).toEqual(1);
    expect(goCDRole.errors().keys()).toEqual(["name"]);
  });

  it("should validate pattern for name", () => {
    const goCDRoleJSON = RolesTestData.GoCDRoleJSON();
    goCDRoleJSON.name  = "&%$Not-allowed";
    const goCDRole     = Role.fromJSON(goCDRoleJSON);

    const isValid = goCDRole.isValid();

    expect(isValid).toBe(false);
    expect(goCDRole.errors().count()).toEqual(1);
    expect(goCDRole.errors().keys()).toEqual(["name"]);
  });

  it("should validate length for name", () => {
    const goCDRoleJSON = RolesTestData.GoCDRoleJSON();
    goCDRoleJSON.name  = "This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters";
    const goCDRole     = Role.fromJSON(goCDRoleJSON);

    const isValid = goCDRole.isValid();

    expect(isValid).toBe(false);
    expect(goCDRole.errors().count()).toEqual(1);
    expect(goCDRole.errors().keys()).toEqual(["name"]);
  });

  it("should serialize gocd role to json", () => {
    const goCDRoleJSON = RolesTestData.GoCDRoleJSON();


    const serializedGoCDRole = Role.fromJSON(goCDRoleJSON).toJSON();

    delete goCDRoleJSON._links;
    expect(serializedGoCDRole).toEqual(goCDRoleJSON);
  });

  it("should serialize plugin role to json", () => {
    const pluginRoleJSON       = RolesTestData.LdapPluginRoleJSON();
    const serializedPluginRole = Role.fromJSON(pluginRoleJSON).toJSON();

    delete pluginRoleJSON._links;
    expect(serializedPluginRole).toEqual(pluginRoleJSON);
  });

});
