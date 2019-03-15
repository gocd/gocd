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

import com.thoughtworks.go.Deny
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.Allow
import com.thoughtworks.go.config.Rules
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.assertj.core.api.Assertions.assertThat

class RulesRepresenterTest {

  @Nested
  class toJSON {
    @Test
    void shouldSerializeRules() {
      Rules rules = new Rules();
      rules.add(new Allow("refer", "PipelineGroup", "DeployPipelines"))
      rules.add(new Allow("view", "Environment", "DeployEnvironment"))
      rules.add(new Deny("refer", "PipelineGroup", "TestPipelines"))
      rules.add(new Deny("view", "Environment", "TestEnvironment"))

      def json = toObjectString({ RulesRepresenter.toJSON(it, rules) })

      JsonFluentAssert.assertThatJson(json).isEqualTo([
        "allow": [
          [
            "action"  : "refer",
            "resource": "DeployPipelines",
            "type"    : "PipelineGroup"
          ],
          [
            "action"  : "view",
            "resource": "DeployEnvironment",
            "type"    : "Environment"
          ]
        ],
        "deny" : [
          [
            "action"  : "refer",
            "resource": "TestPipelines",
            "type"    : "PipelineGroup"
          ],
          [
            "action"  : "view",
            "resource": "TestEnvironment",
            "type"    : "Environment"
          ]
        ]
      ])
    }

    @Test
    void shouldSerializeEmptyRules() {
      Rules rules = new Rules()

      def json = toObjectString({ RulesRepresenter.toJSON(it, rules) })

      JsonFluentAssert.assertThatJson(json).isEqualTo("{}")
    }

  }

  @Nested
  class fromJSON {
    @Test
    void shouldDeSerializeRules() {
      def request = [
        "allow": [
          [
            "action"  : "refer",
            "resource": "DeployPipelines",
            "type"    : "PipelineGroup"
          ],
          [
            "action"  : "view",
            "resource": "DeployEnvironment",
            "type"    : "Environment"
          ]
        ],
        "deny" : [
          [
            "action"  : "refer",
            "resource": "TestPipelines",
            "type"    : "PipelineGroup"
          ],
          [
            "action"  : "view",
            "resource": "TestEnvironment",
            "type"    : "Environment"
          ]
        ]
      ]

      def rules = RulesRepresenter.fromJSON(GsonTransformer.instance.jsonReaderFrom(request))

      assertThat(rules).hasSize(4)
      assertThat(rules.allowDirectives).hasSize(2)
      assertThat(rules.denyDirectives).hasSize(2)

      assertThat(rules.allowDirectives).contains(
        new Allow("view", "Environment", "DeployEnvironment"),
        new Allow("view", "Environment", "DeployEnvironment")
      )

      assertThat(rules.denyDirectives).contains(
        new Deny("view", "Environment", "TestEnvironment"),
        new Deny("view", "Environment", "TestEnvironment")
      )
    }

    @Test
    void shouldDeSerializeEmptyRules() {
      def rules = RulesRepresenter.fromJSON(GsonTransformer.instance.jsonReaderFrom("{}"))

      assertThat(rules).hasSize(0)
    }
  }
}