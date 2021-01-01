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
package com.thoughtworks.go.apiv2.secretconfigs.representers

import com.thoughtworks.go.config.*
import com.thoughtworks.go.config.rules.Allow
import com.thoughtworks.go.config.rules.Deny
import com.thoughtworks.go.config.rules.Rules
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.apiv2.secretconfigs.representers.SecretConfigsRepresenter.toJSON
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class SecretConfigsRepresenterTest {

  @Test
  void shouldSerializeSecretConfigs() {
    SecretConfig secretConfig = new SecretConfig("id", "plugin-id");
    secretConfig.getConfiguration().add(ConfigurationPropertyMother.create("key1", false, "value1"))
    secretConfig.getConfiguration().add(ConfigurationPropertyMother.create("key2", "secret", "AES:lzcCuNSe4vUx+CsWgN11Uw==:YotExzWbFv5w/7/HmpYp3g=="))

    secretConfig.setRules(new Rules([
      new Allow("refer", "PipelineGroup", "DeployPipelines"),
      new Allow("view", "Environment", "DeployEnvironment"),
      new Deny("refer", "PipelineGroup", "TestPipelines"),
      new Deny("view", "Environment", "TestEnvironment")
    ]))

    SecretConfigs configs = new SecretConfigs(secretConfig)

    def json = toObjectString({ toJSON(it, configs) })

    assertThatJson(json).isEqualTo([
      "_embedded": [
        "secret_configs": [
          [
            "_links"    : [
              "doc" : [
                "href": apiDocsUrl('#secret-configs')
              ],
              "find": [
                "href": "http://test.host/go/api/admin/secret_configs/:config_id"
              ],
              "self": [
                "href": "http://test.host/go/api/admin/secret_configs/id"
              ]
            ],
            "id"        : "id",
            "plugin_id" : "plugin-id",
            "properties": [
              [
                "key"  : "key1",
                "value": "value1"
              ],
              [
                "encrypted_value": "AES:lzcCuNSe4vUx+CsWgN11Uw==:YotExzWbFv5w/7/HmpYp3g==",
                "key"            : "key2"
              ]
            ],
            "rules"     : [

              [
                "directive": "allow",
                "action"   : "refer",
                "resource" : "DeployPipelines",
                "type"     : "PipelineGroup"
              ],
              [
                "directive": "allow",
                "action"   : "view",
                "resource" : "DeployEnvironment",
                "type"     : "Environment"
              ],
              [
                "directive": "deny",
                "action"   : "refer",
                "resource" : "TestPipelines",
                "type"     : "PipelineGroup"
              ],
              [
                "directive": "deny",
                "action"   : "view",
                "resource" : "TestEnvironment",
                "type"     : "Environment"
              ]
            ]
          ]
        ]
      ],
      "_links"   : [
        "doc" : [
          "href": apiDocsUrl('#secret-configs')
        ],
        "find": [
          "href": "http://test.host/go/api/admin/secret_configs/:config_id"
        ],
        "self": [
          "href": "http://test.host/go/api/admin/secret_configs"
        ]
      ]
    ])

  }

  @Test
  void shouldSerializeSecretConfigsWhenEmpty() {
    SecretConfigs configs = new SecretConfigs()

    def json = toObjectString({ toJSON(it, configs) })

    assertThatJson(json).isEqualTo([
      "_links"   : [
        "doc" : [
          "href": apiDocsUrl('#secret-configs')
        ],
        "find": [
          "href": "http://test.host/go/api/admin/secret_configs/:config_id"
        ],
        "self": [
          "href": "http://test.host/go/api/admin/secret_configs"
        ]
      ],
      "_embedded": [
        "secret_configs": []
      ]
    ])
  }
}
