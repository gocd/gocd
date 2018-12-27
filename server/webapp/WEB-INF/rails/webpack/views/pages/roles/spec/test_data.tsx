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

export class RolesTestData {

  public static GetAllRoles(): any {
    const testData = [this.GoCDRoleJSON(), this.LdapPluginRoleJSON()];
    return {
      _links: {
        self: {
          href: "https://ci.example.com/go/api/admin/security/roles"
        },
        doc: {
          href: "https://api.gocd.org/#roles"
        },
        find: {
          href: "https://ci.example.com/go/api/admin/security/roles/:role_name"
        }
      },
      _embedded: {
        roles: testData
      }
    };
  }

  public static GoCDRoleJSON(): any {
    return {
      _links: {
        self: {
          href: "https://ci.example.com/go/api/admin/security/roles/spacetiger"
        },
        doc: {
          href: "https://api.gocd.org/#roles"
        },
        find: {
          href: "https://ci.example.com/go/api/admin/security/roles/:role_name"
        }
      },
      name: "spacetiger",
      type: "gocd",
      attributes: {
        users: ["alice", "bob", "robin"]
      }
    };
  }

  public static EmptyGoCDRoleJSON(): any {
    return {
      _links: {
        self: {
          href: "https://ci.example.com/go/api/admin/security/roles/spacetiger"
        },
        doc: {
          href: "https://api.gocd.org/#roles"
        },
        find: {
          href: "https://ci.example.com/go/api/admin/security/roles/:role_name"
        }
      },
      name: "spacetiger",
      type: "gocd",
      attributes: {
        users: []
      }
    };
  }

  public static LdapPluginRoleJSON(): any {
    return {
      _links: {
        self: {
          href: "https://ci.example.com/go/api/admin/security/roles/blackbird"
        },
        doc: {
          href: "https://api.gocd.org/#roles"
        },
        find: {
          href: "https://ci.example.com/go/api/admin/security/roles/:role_name"
        }
      },
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
    };
  }

  public static GitHubPluginRoleJSON(): any {
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
    };
  }
}
