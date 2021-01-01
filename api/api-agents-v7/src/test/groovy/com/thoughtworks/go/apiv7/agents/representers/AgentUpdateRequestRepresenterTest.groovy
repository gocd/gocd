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
package com.thoughtworks.go.apiv7.agents.representers

import com.thoughtworks.go.apiv7.agents.model.AgentUpdateRequest
import com.thoughtworks.go.util.TriState
import groovy.json.JsonBuilder
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

import static org.assertj.core.api.Assertions.assertThat

class AgentUpdateRequestRepresenterTest {
  @Test
  void 'should convert request body to agent info'() {
    def jsonString = new JsonBuilder(["hostname"          : "agent02.example.com",
                                      "agent_config_state": "Enabled",
                                      "resources"         : ["java", "psql"],
                                      "environments"      : ["env1"]
    ]).toString()

    AgentUpdateRequest agentInfo = AgentUpdateRequestRepresenter.fromJSON(jsonString)

    Assertions.assertThat(agentInfo.getHostname()).isEqualTo("agent02.example.com")
    assertThat(agentInfo.getAgentConfigState()).isEqualTo(TriState.TRUE)
    Assertions.assertThat(agentInfo.getResources()).isEqualTo("java,psql")
    Assertions.assertThat(agentInfo.getEnvironments()).isEqualTo("env1")
  }

  @Test
  void 'should create agent update request when request body does not contains hostname'() {
    def jsonString = new JsonBuilder(["agent_config_state": "Enabled",
                                      "resources"         : ["java", "psql"],
                                      "environments"      : ["env1"]
    ]).toString()

    AgentUpdateRequest agentInfo = AgentUpdateRequestRepresenter.fromJSON(jsonString)

    Assertions.assertThat(agentInfo.getHostname()).isEqualTo(null)
    assertThat(agentInfo.getAgentConfigState()).isEqualTo(TriState.TRUE)
    Assertions.assertThat(agentInfo.getResources()).isEqualTo("java,psql")
    Assertions.assertThat(agentInfo.getEnvironments()).isEqualTo("env1")
  }

  @Nested
  class Resources {
    @Test
    void 'should accept comma separated string as resources value'() {
      def jsonString = new JsonBuilder(["hostname"          : "agent02.example.com",
                                        "agent_config_state": "Enabled",
                                        "resources"         : "firefox,chrome,safari"
      ]).toString()

      AgentUpdateRequest agentInfo = AgentUpdateRequestRepresenter.fromJSON(jsonString)

      Assertions.assertThat(agentInfo.getResources()).isEqualTo("firefox,chrome,safari")
    }

    @Test
    void 'should return empty string when empty resources specified in request body'() {
      def jsonString = new JsonBuilder(["hostname"          : "agent02.example.com",
                                        "agent_config_state": "Enabled",
                                        "resources"         : [],
                                        "environments"      : ["env1"]
      ]).toString()

      AgentUpdateRequest agentInfo = AgentUpdateRequestRepresenter.fromJSON(jsonString)

      Assertions.assertThat(agentInfo.getResources()).isEmpty()
    }

    @Test
    void 'should return null string when resources are not specified'() {
      def jsonString = new JsonBuilder(["hostname"          : "agent02.example.com",
                                        "agent_config_state": "Enabled",
                                        "environments"      : []
      ]).toString()

      AgentUpdateRequest agentInfo = AgentUpdateRequestRepresenter.fromJSON(jsonString)

      Assertions.assertThat(agentInfo.getResources()).isNull()
    }
  }

  @Nested
  class Environments {
    @Test
    void 'should accept comma separated string as environments value'() {
      def jsonString = new JsonBuilder(["hostname"          : "agent02.example.com",
                                        "agent_config_state": "Enabled",
                                        "environments"      : "linux,java,psql"
      ]).toString()

      AgentUpdateRequest agentInfo = AgentUpdateRequestRepresenter.fromJSON(jsonString)

      Assertions.assertThat(agentInfo.getEnvironments()).isEqualTo("linux,java,psql")
    }

    @Test
    void 'should return empty string when empty environments specified in request'() {
      def jsonString = new JsonBuilder(["hostname"          : "agent02.example.com",
                                        "agent_config_state": "Enabled",
                                        "resources"         : ["Foo"],
                                        "environments"      : []
      ]).toString()

      AgentUpdateRequest agentInfo = AgentUpdateRequestRepresenter.fromJSON(jsonString)

      Assertions.assertThat(agentInfo.getEnvironments()).isEmpty()
    }

    @Test
    void 'should return null string when environments are not specified'() {
      def jsonString = new JsonBuilder(["hostname"          : "agent02.example.com",
                                        "agent_config_state": "Enabled",
                                        "resources"         : []
      ]).toString()

      AgentUpdateRequest agentInfo = AgentUpdateRequestRepresenter.fromJSON(jsonString)

      Assertions.assertThat(agentInfo.getEnvironments()).isNull()
    }
  }

  @ParameterizedTest
  @MethodSource("agentConfigToTriState")
  void 'should convert agent config state to tristate'(String agentconfigState, TriState triState) {
    def jsonString = new JsonBuilder(["hostname"          : "agent02.example.com",
                                      "agent_config_state": agentconfigState,
                                      "environments"      : []
    ]).toString()

    AgentUpdateRequest agentInfo = AgentUpdateRequestRepresenter.fromJSON(jsonString)

    assertThat(agentInfo.getAgentConfigState()).isEqualTo(triState)
  }

  private static Stream<Arguments> agentConfigToTriState() {
    return Stream.of(
      Arguments.of(null, TriState.UNSET),
      Arguments.of("", TriState.UNSET),
      Arguments.of("enabled", TriState.TRUE),
      Arguments.of("disabled", TriState.FALSE),
    )
  }
}
