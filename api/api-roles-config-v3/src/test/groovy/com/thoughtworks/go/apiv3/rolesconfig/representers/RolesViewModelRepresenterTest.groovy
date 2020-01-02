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
package com.thoughtworks.go.apiv3.rolesconfig.representers

import com.thoughtworks.go.apiv3.rolesconfig.models.RolesViewModel
import com.thoughtworks.go.config.*
import com.thoughtworks.go.domain.config.ConfigurationKey
import com.thoughtworks.go.domain.config.ConfigurationProperty
import com.thoughtworks.go.domain.config.ConfigurationValue
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static java.util.Arrays.asList
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class RolesViewModelRepresenterTest {
  private final PluginRoleConfig pluginRoleConfig = new PluginRoleConfig("blackbird", "ldap", new ConfigurationProperty(new ConfigurationKey("abc"), new ConfigurationValue("def")))

  private final RoleConfig goCDRoleConfig = new RoleConfig(new CaseInsensitiveString("admins"), new RoleUser("bob"))

  @Test
  void 'should serialize into json'() {
    def rolesConfig = new RolesConfig(goCDRoleConfig, pluginRoleConfig)
    def autoSuggestions = new HashMap<String, List<String>>()
    autoSuggestions.put("key1", asList("val1", "val2"))
    autoSuggestions.put("key2", asList("val3", "val3"))
    def rolesViewModel = new RolesViewModel().setRolesConfig(rolesConfig).setAutoSuggestions(autoSuggestions)

    def actualJson = toObjectString({ RolesViewModelRepresenter.toJSON(it, rolesViewModel) })

    def expectedJson = [
      "_embedded"      : [
        "roles": rolesConfig.collect { role -> toObject({ RoleRepresenter.toJSON(it, role) }) }
      ],
      "auto_completion": [
        [
          "key"  : "key1",
          "value": [
            "val1",
            "val2"
          ]
        ],
        [
          "key"  : "key2",
          "value": [
            "val3",
            "val3"
          ]
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
