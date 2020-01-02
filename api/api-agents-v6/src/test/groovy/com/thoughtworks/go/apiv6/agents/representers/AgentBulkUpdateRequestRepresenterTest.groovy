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
package com.thoughtworks.go.apiv6.agents.representers

import com.thoughtworks.go.apiv6.agents.model.AgentBulkUpdateRequest
import com.thoughtworks.go.util.TriState
import groovy.json.JsonBuilder
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat

class AgentBulkUpdateRequestRepresenterTest {
  @Test
  void 'should convert json to agent bulk update request'() {
    def requestBody = new JsonBuilder([
      "uuids"             : [
        "adb9540a-b954-4571-9d9b-2f330739d4da",
        "adb528b2-b954-1234-9d9b-b27ag4h568e1"
      ],
      "operations"        : [
        "environments": [
          "add"   : ["Dev", "Test"],
          "remove": ["Production"]
        ],
        "resources"   : [
          "add"   : ["Linux", "Firefox"],
          "remove": ["Chrome"]
        ]
      ],
      "agent_config_state": "enabled"
    ]).toString()

    AgentBulkUpdateRequest request = AgentBulkUpdateRequestRepresenter.fromJSON(requestBody)

    assertThat(request.getAgentConfigState()).isEqualTo(TriState.TRUE)
    Assertions.assertThat(request.getUuids())
      .hasSize(2)
      .contains("adb9540a-b954-4571-9d9b-2f330739d4da", "adb528b2-b954-1234-9d9b-b27ag4h568e1")

    Assertions.assertThat(request.getOperations().getEnvironments().toAdd())
      .hasSize(2)
      .contains("Dev", "Test")
    Assertions.assertThat(request.getOperations().getEnvironments().toRemove())
      .hasSize(1)
      .contains("Production")

    Assertions.assertThat(request.getOperations().getResources().toAdd())
      .hasSize(2)
      .contains("Linux", "Firefox")
    Assertions.assertThat(request.getOperations().getResources().toRemove())
      .hasSize(1)
      .contains("Chrome")
  }

  @Test
  void 'should parse request body without environment'() {
    def requestBody = new JsonBuilder([
      "uuids"             : [
        "adb9540a-b954-4571-9d9b-2f330739d4da",
        "adb528b2-b954-1234-9d9b-b27ag4h568e1"
      ],
      "operations"        : [
        "resources": [
          "add"   : ["Linux", "Firefox"],
          "remove": ["Chrome"]
        ]
      ],
      "agent_config_state": "enabled"
    ]).toString()

    AgentBulkUpdateRequest request = AgentBulkUpdateRequestRepresenter.fromJSON(requestBody)

    assertThat(request.getAgentConfigState()).isEqualTo(TriState.TRUE)
    Assertions.assertThat(request.getUuids())
      .hasSize(2)
      .contains("adb9540a-b954-4571-9d9b-2f330739d4da", "adb528b2-b954-1234-9d9b-b27ag4h568e1")

    Assertions.assertThat(request.getOperations().getEnvironments().toAdd()).isEmpty()
    Assertions.assertThat(request.getOperations().getEnvironments().toRemove()).isEmpty()

    Assertions.assertThat(request.getOperations().getResources().toAdd())
      .hasSize(2)
      .contains("Linux", "Firefox")
    Assertions.assertThat(request.getOperations().getResources().toRemove())
      .hasSize(1)
      .contains("Chrome")
  }

  @Test
  void 'should parse request body without resources'() {
    def requestBody = new JsonBuilder([
      "uuids"             : [
        "adb9540a-b954-4571-9d9b-2f330739d4da",
        "adb528b2-b954-1234-9d9b-b27ag4h568e1"
      ],
      "operations"        : [
        "environments": [
          "add"   : ["Dev", "Test"],
          "remove": ["Production"]
        ]
      ],
      "agent_config_state": "enabled"
    ]).toString()

    AgentBulkUpdateRequest request = AgentBulkUpdateRequestRepresenter.fromJSON(requestBody)

    assertThat(request.getAgentConfigState()).isEqualTo(TriState.TRUE)
    Assertions.assertThat(request.getUuids())
      .hasSize(2)
      .contains("adb9540a-b954-4571-9d9b-2f330739d4da", "adb528b2-b954-1234-9d9b-b27ag4h568e1")

    Assertions.assertThat(request.getOperations().getEnvironments().toAdd())
      .hasSize(2)
      .contains("Dev", "Test")
    Assertions.assertThat(request.getOperations().getEnvironments().toRemove())
      .hasSize(1)
      .contains("Production")

    Assertions.assertThat(request.getOperations().getResources().toAdd()).isEmpty()
    Assertions.assertThat(request.getOperations().getResources().toRemove()).isEmpty()
  }
}
