/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.secretconfigs.representers

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.Allow
import com.thoughtworks.go.config.Deny
import com.thoughtworks.go.config.Rules
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat
import static org.junit.jupiter.api.Assertions.assertThrows

class RulesRepresenterTest {

  @Nested
  class toJSON {
    @Test
    void shouldSerializeRules() {
      Rules rules = new Rules()
      rules.add(new Allow("refer", "PipelineGroup", "DeployPipelines"))
      rules.add(new Allow("view", "Environment", "DeployEnvironment"))
      rules.add(new Deny("refer", "PipelineGroup", "TestPipelines"))
      rules.add(new Deny("view", "Environment", "TestEnvironment"))

      def json = toArrayString({ RulesRepresenter.toJSON(it, rules) })

      assertThatJson(json).isEqualTo(
        [
          [
            directive: "allow",
            action   : "refer",
            type     : "PipelineGroup",
            resource : "DeployPipelines"
          ],
          [
            directive: "allow",
            action   : "view",
            resource : "DeployEnvironment",
            type     : "Environment"
          ],
          [
            directive: "deny",
            action   : "refer",
            type     : "PipelineGroup",
            resource : "TestPipelines"
          ],
          [
            directive: "deny",
            action   : "view",
            resource : "TestEnvironment",
            type     : "Environment"
          ]
        ]
      )
    }

    @Test
    void shouldSerializeEmptyRules() {
      def json = toArrayString({ RulesRepresenter.toJSON(it, new Rules()) })

      assertThatJson(json).isEqualTo("[]")
    }

  }

  @Nested
  class fromJSON {
    @Test
    void shouldDeSerializeRules() {
      def request = [
        "rules": [
          [
            directive: "allow",
            action   : "refer",
            type     : "PipelineGroup",
            resource : "DeployPipelines"
          ],
          [
            directive: "allow",
            action   : "view",
            resource : "DeployEnvironment",
            type     : "Environment"
          ],
          [
            directive: "deny",
            action   : "refer",
            type     : "PipelineGroup",
            resource : "TestPipelines"
          ],
          [
            directive: "deny",
            action   : "view",
            resource : "TestEnvironment",
            type     : "Environment"
          ]
        ]
      ]


      def jsonRequest = new Gson().toJson(request).toString()
      def jsonObject = GsonTransformer.instance.jsonReaderFrom(jsonRequest)
      def rules = RulesRepresenter.fromJSON(jsonObject.optJsonArray("rules").get())

      assertThat(rules).hasSize(4)

      assertThat(rules).contains(
        new Allow("view", "Environment", "DeployEnvironment"),
        new Allow("view", "Environment", "DeployEnvironment"),
        new Deny("view", "Environment", "TestEnvironment"),
        new Deny("view", "Environment", "TestEnvironment")
      )
    }

    @Test
    void shouldReturn422WhenInvalidDirectiveIsSpecifiedInJson() {
      def request = [
        "rules": [
          [
            directive: "foobar",
            action   : "refer",
            type     : "PipelineGroup",
            resource : "DeployPipelines"
          ]
        ]
      ]

      def jsonRequest = new Gson().toJson(request).toString()
      def jsonObject = GsonTransformer.instance.jsonReaderFrom(jsonRequest)

      def error = (spark.HaltException) assertThrows(spark.HaltException.class, {
        RulesRepresenter.fromJSON(jsonObject.optJsonArray("rules").get())
      })

      assertThatJson(error.body()).isEqualTo([
        "message" : "Invalid rule directive 'foobar' in JSON payload '{\"directive\":\"foobar\",\"action\":\"refer\",\"type\":\"PipelineGroup\",\"resource\":\"DeployPipelines\"}'."
      ])
    }

    @Test
    void shouldDeSerializeEmptyRules() {
      def rules = RulesRepresenter.fromJSON(new JsonArray())

      assertThat(rules).hasSize(0)
    }
  }
}