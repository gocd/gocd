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
package com.thoughtworks.go.apiv2.secretconfigs.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.rules.Allow
import com.thoughtworks.go.config.rules.Deny
import com.thoughtworks.go.config.rules.Rules
import com.thoughtworks.go.config.SecretConfig
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.security.GoCipher
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import spark.HaltException

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.apiv2.secretconfigs.representers.SecretConfigRepresenter.fromJSON
import static com.thoughtworks.go.apiv2.secretconfigs.representers.SecretConfigRepresenter.toJSON
import static org.assertj.core.api.Assertions.assertThat
import static org.junit.jupiter.api.Assertions.assertThrows

class SecretConfigRepresenterTest {

  @Nested
  class toJSON {
    SecretConfig secretConfig

    @BeforeEach
    void setup() {
      secretConfig = new SecretConfig("id", "plugin-id")
      secretConfig.getConfiguration().add(ConfigurationPropertyMother.create("key1", false, "value1"))
      secretConfig.getConfiguration().add(ConfigurationPropertyMother.create("key2", "secret", new GoCipher().encrypt("secret")))

      secretConfig.setRules(new Rules([
        new Allow("refer", "PipelineGroup", "DeployPipelines"),
        new Allow("view", "Environment", "DeployEnvironment"),
        new Deny("refer", "PipelineGroup", "TestPipelines"),
        new Deny("view", "Environment", "TestEnvironment")
      ]))
    }

    @Test
    void shouldSerializeToJSON() {
      def json = toObjectString({ toJSON(it, secretConfig) })

      JsonFluentAssert.assertThatJson(json).isEqualTo([
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
            "key"            : "key2",
            "encrypted_value": new GoCipher().encrypt("secret")
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
      ])
    }

    @Test
    void shouldSerializeEmptyObject() {
      secretConfig = null
      def json = toObjectString({ toJSON(it, secretConfig) })

      JsonFluentAssert.assertThatJson(json).isEqualTo("{}")
    }

    @Test
    void shouldNotRenderRulesIfEmpty() {
      secretConfig.setRules(new Rules())
      def json = toObjectString({ toJSON(it, secretConfig) })

      JsonFluentAssert.assertThatJson(json).isEqualTo([
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
            "key"            : "key2",
            "encrypted_value": new GoCipher().encrypt("secret")
          ]
        ]
      ])
    }
  }

  @Nested
  class fromJSON {
    @Test
    void shouldDeserializeFromJSON() {
      def json = [
        "id"        : "ForDeploy",
        "plugin_id" : "go.secret.file",
        "properties": [
          [
            "key"  : "key1",
            "value": "value1"
          ],
          [
            "key"            : "key2",
            "encrypted_value": new GoCipher().encrypt("secret")
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

      def secretConfig = fromJSON(GsonTransformer.instance.jsonReaderFrom(json))

      assertThat(secretConfig.id).isEqualTo("ForDeploy")
      assertThat(secretConfig.pluginId).isEqualTo("go.secret.file")
      assertThat(secretConfig.description).isNull()

      assertThat(secretConfig.getConfiguration())
        .hasSize(2)
        .contains(
        ConfigurationPropertyMother.create("key1", false, "value1"),
        ConfigurationPropertyMother.create("key2", true, "secret")
      )
      assertThat(secretConfig.rules)
        .hasSize(4)
        .contains(new Allow("refer", "PipelineGroup", "DeployPipelines"),
        new Allow("view", "Environment", "DeployEnvironment"),
        new Deny("refer", "PipelineGroup", "TestPipelines"),
        new Deny("view", "Environment", "TestEnvironment"))
    }

    @Test
    void shouldErrorOutIfIdNotProvided() {

      HaltException exception = assertThrows(HaltException.class, {
        fromJSON(GsonTransformer.instance.jsonReaderFrom("{}"))
      })

      JsonFluentAssert.assertThatJson(exception.body()).isEqualTo("{\"message\" : \"Json `{}` does not contain property 'id'\"}")
    }

    @Test
    void shouldErrorOutIfPluginIdNotProvided() {

      HaltException exception = assertThrows(HaltException.class, {
        fromJSON(GsonTransformer.instance.jsonReaderFrom([id: "foobar"]))
      })

      JsonFluentAssert.assertThatJson(exception.body()).isEqualTo("{\"message\":\"Json `{\\\"id\\\":\\\"foobar\\\"}` does not contain property 'plugin_id'\"}")
    }

    @Test
    void shouldDeserializeWithOnlyMandatoryFields() {
      def secretConfig = fromJSON(GsonTransformer.instance.jsonReaderFrom([id: "foobar", plugin_id: "barfoo"]))

      assertThat(secretConfig.id).isEqualTo("foobar")
      assertThat(secretConfig.pluginId).isEqualTo("barfoo")
      assertThat(secretConfig.description).isNull()
    }
  }
}
