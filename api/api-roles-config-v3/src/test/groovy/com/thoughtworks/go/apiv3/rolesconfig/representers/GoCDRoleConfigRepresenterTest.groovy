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
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.RoleConfig
import com.thoughtworks.go.config.RoleUser
import com.thoughtworks.go.config.policy.Allow
import com.thoughtworks.go.config.policy.Policy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

class GoCDRoleConfigRepresenterTest {
  private LinkedHashMap<String, Object> map
  private RoleConfig roleConfig

  @BeforeEach
  void setUp() {
    map = [
      _links    : [
        doc : [href: apiDocsUrl('#roles')],
        self: [href: 'http://test.host/go/api/admin/security/roles/admins'],
        find: [href: 'http://test.host/go/api/admin/security/roles/:role_name']
      ],
      name      : 'admins',
      type      : 'gocd',
      policy    : [[
                     permission: "allow",
                     action    : "view",
                     type      : "environment",
                     resource  : "foo*"
                   ]],
      attributes: [
        users: ['bob', 'alice']
      ]
    ]
    roleConfig = new RoleConfig(new CaseInsensitiveString("admins"), new RoleUser("bob"), new RoleUser("alice"))
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
    assertThat(roleConfig).isEqualTo(this.roleConfig)
  }
}
