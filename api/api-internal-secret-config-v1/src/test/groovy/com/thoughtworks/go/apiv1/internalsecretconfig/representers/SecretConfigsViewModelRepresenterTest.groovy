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

package com.thoughtworks.go.apiv1.internalsecretconfig.representers

import com.thoughtworks.go.apiv1.internalsecretconfig.models.SecretConfigsViewModel
import com.thoughtworks.go.config.SecretConfig
import com.thoughtworks.go.config.SecretConfigs
import com.thoughtworks.go.config.rules.Allow
import com.thoughtworks.go.config.rules.Deny
import com.thoughtworks.go.config.rules.Rules
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class SecretConfigsViewModelRepresenterTest {

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
    def model = new SecretConfigsViewModel().setSecretConfigs(configs)
    model.getAutoSuggestions().put("key1", ["value1", "value2"])
    model.getAutoSuggestions().put("key2", ["value3", "value4"])

    def actualJson = toObjectString({ SecretConfigsViewModelRepresenter.toJSON(it, model) })

    def expectedJson = [
      "_embedded"      : [
        "secret_configs": configs.collect { eachItem -> toObject({ SecretConfigRepresenter.toJSON(it, eachItem) }) }
      ],
      "auto_completion": [
        [
          "key"  : "key1",
          "value": ["value1", "value2"]
        ],
        [
          "key"  : "key2",
          "value": ["value3", "value4"]
        ]
      ]
    ]
    assertThatJson(actualJson).isEqualTo(expectedJson)

  }

  @Test
  void shouldSerializeSecretConfigsWhenEmpty() {
    SecretConfigs configs = new SecretConfigs()
    def model = new SecretConfigsViewModel().setSecretConfigs(configs)

    def actualJson = toObjectString({ SecretConfigsViewModelRepresenter.toJSON(it, model) })

    def expectedJson = [
      "_embedded"      : [
        "secret_configs": []
      ],
      "auto_completion": []
    ]
    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
