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
package com.thoughtworks.go.apiv3.rolesconfig.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.PluginRoleConfig
import com.thoughtworks.go.config.policy.Allow
import com.thoughtworks.go.config.policy.Policy
import com.thoughtworks.go.domain.config.ConfigurationKey
import com.thoughtworks.go.domain.config.ConfigurationProperty
import com.thoughtworks.go.domain.config.ConfigurationValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PluginRoleConfigRepresenterTest {
  private LinkedHashMap<String, Object> map
  private PluginRoleConfig roleConfig

  @BeforeEach
  void setUp() {
    map = [
      _links    : [
        doc : [href: apiDocsUrl('#roles')],
        self: [href: 'http://test.host/go/api/admin/security/roles/blackbird'],
        find: [href: 'http://test.host/go/api/admin/security/roles/:role_name']
      ],
      name      : 'blackbird',
      type      : 'plugin',
      policy    : [[
                     permission: "allow",
                     action    : "view",
                     type      : "environment",
                     resource  : "foo*"
                   ]],
      attributes: [
        auth_config_id: "ldap",
        properties    : [
          [
            key  : "UserGroupMembershipAttribute",
            value: "memberOf"
          ],
          [
            key  : "GroupIdentifiers",
            value: "ou=admins,ou=groups,ou=system,dc=example,dc=com"
          ]
        ]
      ]
    ]

    roleConfig = new PluginRoleConfig("blackbird", "ldap",
      new ConfigurationProperty(new ConfigurationKey("UserGroupMembershipAttribute"), new ConfigurationValue("memberOf")),
      new ConfigurationProperty(new ConfigurationKey("GroupIdentifiers"), new ConfigurationValue("ou=admins,ou=groups,ou=system,dc=example,dc=com")))

    def directives = new Policy()
    directives.add(new Allow("view", "environment", "foo*"))
    roleConfig.setPolicy(directives)
  }

  @Test
  void shouldGenerateJSON() {
    def actualJson = toObjectString({ RoleRepresenter.toJSON(it, roleConfig) })

    assertThatJson(actualJson).isEqualTo(this.map)
  }

  @Test
  void shouldBuildObjectFromJson() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(map)
    def roleConfig = RoleRepresenter.fromJSON(jsonReader)
    assertThatJson(roleConfig).isEqualTo(this.roleConfig)
  }
}
