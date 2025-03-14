/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AgentRuntimeStatusTest {

    @Test
    public void shouldConvertToBuildState() {
        List<AgentRuntimeStatus> agentRuntimeStatuses = List.of(AgentRuntimeStatus.Idle, AgentRuntimeStatus.Building, AgentRuntimeStatus.Cancelled);

        for (AgentRuntimeStatus status : AgentRuntimeStatus.values()) {
            if (agentRuntimeStatuses.contains(status)) {
                assertEquals(status.buildState(), status);
            } else {
                assertEquals(status.buildState(), AgentRuntimeStatus.Unknown);
            }
        }
    }

    @Test
    public void shouldConvertToAgentState() {
        List<AgentRuntimeStatus> agentRuntimeStatuses = List.of(AgentRuntimeStatus.Idle, AgentRuntimeStatus.Building, AgentRuntimeStatus.LostContact, AgentRuntimeStatus.Missing);

        for (AgentRuntimeStatus status : AgentRuntimeStatus.values()) {
            if (agentRuntimeStatuses.contains(status)) {
                assertEquals(status.agentState(), status);
            } else if (status == AgentRuntimeStatus.Cancelled) {
                assertEquals(status.agentState(), AgentRuntimeStatus.Building);
            } else {
                assertEquals(status.agentState(), AgentRuntimeStatus.Unknown);
            }
        }
    }
}
