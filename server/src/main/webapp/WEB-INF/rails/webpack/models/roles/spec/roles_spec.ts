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

import {GoCDRole, PluginRole, Role, RolesWithSuggestions, RolesWithSuggestionsJSON, RoleType} from "models/roles/roles";
import {RolesTestData} from "views/pages/roles/spec/test_data";

describe("RoleModel", () => {
  it("should deserialize gocd role json", () => {
    const goCDRoleJSON = RolesTestData.GoCDRoleJSON();
    const goCDRole     = Role.fromJSON(goCDRoleJSON) as GoCDRole;

    expect(goCDRole.name()).toEqual("spacetiger");
    expect(goCDRole.type()).toEqual(RoleType.gocd);
    expect(goCDRole.attributes().users).toHaveLength(3);
    expect(goCDRole.attributes().users[0]).toEqual("alice");
    expect(goCDRole.policy()).toHaveLength(1);
    expect(goCDRole.policy()[0]().permission()).toEqual("allow");
    expect(goCDRole.policy()[0]().action()).toEqual("view");
    expect(goCDRole.policy()[0]().type()).toEqual("environment");
    expect(goCDRole.policy()[0]().resource()).toEqual("*");
  });

  it("should deserialize plugin role json", () => {
    const pluginRoleJSON = RolesTestData.LdapPluginRoleJSON();
    const pluginRole     = Role.fromJSON(pluginRoleJSON) as PluginRole;

    expect(pluginRole.name()).toEqual("blackbird");
    expect(pluginRole.type()).toEqual(RoleType.plugin);
    expect(pluginRole.attributes().authConfigId).toEqual("ldap");
    expect(pluginRole.attributes().properties().allConfigurations()).toHaveLength(2);
    expect(pluginRole.attributes().properties().allConfigurations()[0].key).toEqual("UserGroupMembershipAttribute");
    expect(pluginRole.attributes().properties().allConfigurations()[0].displayValue()).toEqual("memberOf");
    expect(pluginRole.policy()).toHaveLength(1);
    expect(pluginRole.policy()[0]().permission()).toEqual("deny");
    expect(pluginRole.policy()[0]().action()).toEqual("view");
    expect(pluginRole.policy()[0]().type()).toEqual("environment");
    expect(pluginRole.policy()[0]().resource()).toEqual("*");
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

    const serializedGoCDRole = JSON.parse(JSON.stringify(Role.fromJSON(goCDRoleJSON)));

    expect(serializedGoCDRole).toEqual(goCDRoleJSON);
  });

  it("should serialize plugin role to json", () => {
    const pluginRoleJSON       = RolesTestData.LdapPluginRoleJSON();
    const serializedPluginRole = JSON.parse(JSON.stringify(Role.fromJSON(pluginRoleJSON)));

    expect(serializedPluginRole).toEqual(pluginRoleJSON);
  });

  it('should validate policy for presence of permissions', () => {
    const goCDRoleJSON = RolesTestData.GoCDRoleJSON();
    delete goCDRoleJSON.policy[0].permission;
    delete goCDRoleJSON.policy[0].action;
    delete goCDRoleJSON.policy[0].type;
    delete goCDRoleJSON.policy[0].resource;
    const goCDRole = Role.fromJSON(goCDRoleJSON) as GoCDRole;

    const isValid = goCDRole.isValid();
    expect(isValid).toBe(false);

    const policyErrors = goCDRole.policy()[0]().errors();
    expect(policyErrors.count()).toEqual(4);
    expect(policyErrors.errorsForDisplay("permission")).toEqual("Permission must be present.");
    expect(policyErrors.errorsForDisplay("action")).toEqual("Action must be present.");
    expect(policyErrors.errorsForDisplay("type")).toEqual("Type must be present.");
    expect(policyErrors.errorsForDisplay("resource")).toEqual("Resource must be present.");
  });
});

describe('RolesWithSuggestions', () => {
  it('should deserialize roles with auto suggestions', () => {
    const goCDRoleJSON    = RolesTestData.GoCDRoleJSON();
    const inputJson       = {
      _embedded:       {
        roles: [goCDRoleJSON]
      },
      auto_completion: [
        {
          key:   "key1",
          value: ["val1", "val2"]
        }
      ]
    } as RolesWithSuggestionsJSON;
    const withSuggestions = RolesWithSuggestions.fromJSON(inputJson);

    expect(withSuggestions.roles.length).toEqual(1);
    expect(withSuggestions.roles[0].name()).toEqual("spacetiger");

    expect(withSuggestions.autoCompletion.length).toEqual(1);
    expect(withSuggestions.autoCompletion[0].key).toEqual("key1");
    expect(withSuggestions.autoCompletion[0].value).toEqual(["val1", "val2"]);
  });
});
