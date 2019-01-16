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

import {RoleJSON, RolesJSON} from "models/roles/roles_new";

export class RolesTestData {

  public static GetAllRoles() {
    const testData = [this.GoCDRoleJSON(), this.LdapPluginRoleJSON()];
    return {
      _embedded: {
        roles: testData
      }
    } as RolesJSON;
  }

  public static GoCDRoleJSON(roleName = "spacetiger", users = ["alice", "bob", "robin"]) {
    return {
      name: roleName,
      type: "gocd",
      attributes: {
        users
      }
    } as RoleJSON;
  }

  public static EmptyGoCDRoleJSON() {
    return {
      name: "spacetiger",
      type: "gocd",
      attributes: {
        users: []
      }
    } as RoleJSON;
  }

  public static LdapPluginRoleJSON() {
    return {
      name: "blackbird",
      type: "plugin",
      attributes: {
        auth_config_id: "ldap",
        properties: [
          {
            key: "UserGroupMembershipAttribute",
            value: "memberOf"
          },
          {
            key: "GroupIdentifiers",
            value: "ou=admins,ou=groups,ou=system,dc=example,dc=com"
          }
        ]
      }
    } as RoleJSON;
  }

  public static GitHubPluginRoleJSON() {
    return {
      name: "github-role",
      type: "plugin",
      attributes: {
        auth_config_id: "github",
        properties: [
          {
            key: "Organization",
            value: "gocd"
          }
        ]
      }
    } as RoleJSON;
  }
}
