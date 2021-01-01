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

import {AuthConfigsJSON} from "models/auth_configs/auth_configs";

export class TestData {
  static authConfigList(...objects: any[]) {
    return {
      _links: {
        self: {
          href: "https://ci.example.com/go/api/admin/security/auth_configs"
        },
        doc: {
          href: "https://api.gocd.org/#authorization-configuration"
        },
        find: {
          href: "https://ci.example.com/go/api/admin/security/auth_configs/:auth_config_id"
        }
      },
      _embedded: {
        auth_configs: objects
      }
    } as AuthConfigsJSON;
  }

  static ldapAuthConfig() {
    return {
      id: "ldap",
      plugin_id: "cd.go.authorization.ldap",
      allow_only_known_users_to_login: true,
      properties: [
        {
          key: "Url",
          value: "ldap://ldap.server.url"
        },
        {
          key: "ManagerDN",
          value: "uid=admin,ou=system"
        },
        {
          key: "Password",
          encrypted_value: "gGx7G+4+BAQ="
        }
      ]
    };
  }

  static gitHubAuthConfig() {
    return {
      id: "github",
      plugin_id: "cd.go.authorization.github",
      allow_only_known_users_to_login: false,
      properties: [
        {
          key: "Url",
          value: "https://foo.github.com"
        },
        {
          key: "ClientKey",
          value: "some-key"
        },
        {
          key: "ClientSecret",
          encrypted_value: "gGx7G+4+BAQ="
        }
      ]
    };
  }
}
