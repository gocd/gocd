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

package com.thoughtworks.go.apiv1.templateauthorization.representers

import com.thoughtworks.go.apiv1.internalagent.representers.GetWorkRequestRepresenter
import com.thoughtworks.go.apiv1.internalagent.representers.WorkRepresenter
import com.thoughtworks.go.config.Agent
import com.thoughtworks.go.domain.AgentRuntimeStatus
import com.thoughtworks.go.remote.work.DeniedAgentWork
import com.thoughtworks.go.server.service.AgentRuntimeInfo
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo
import org.apache.commons.lang.builder.EqualsBuilder
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory
import static org.assertj.core.api.Assertions.assertThat

class GetWorkRequestRepresenterTest {
    @Test
    void 'should deserialize request from json'() {
        def requestJSON = "{\n" +
                "  \"agentRuntimeInfo\": {\n" +
                "    \"type\": \"AgentRuntimeInfo\",\n" +
                "    \"identifier\": {\n" +
                "      \"hostName\": \"localhost\",\n" +
                "      \"ipAddress\": \"176.19.4.1\",\n" +
                "      \"uuid\": \"uuid\"\n" +
                "    },\n" +
                "    \"runtimeStatus\": \"Idle\",\n" +
                "    \"buildingInfo\": {\n" +
                "      \"buildingInfo\": \"\",\n" +
                "      \"buildLocator\": \"\"\n" +
                "    },\n" +
                "    \"location\": \"/some/random/location\",\n" +
                "    \"usableSpace\": 10,\n" +
                "    \"operatingSystemName\": \"Mac OS X\",\n" +
                "    \"agentBootstrapperVersion\": \"20.1.0\",\n" +
                "    \"agentVersion\": \"20.9.0\"\n" +
                "  }\n" +
                "}"

        def agent = new Agent("uuid", "localhost", "176.19.4.1")
        def expectedRuntimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
                "20.1.0", "20.9.0")
        expectedRuntimeInfo.setUsableSpace(10L)
        expectedRuntimeInfo.setLocation("/some/random/location")
        expectedRuntimeInfo.setOperatingSystem("Mac OS X")

        def request = GetWorkRequestRepresenter.fromJSON(requestJSON)

        assertThat(request.getAgentRuntimeInfo()).isEqualTo(expectedRuntimeInfo)
    }

    @Test
    void 'should deserialize json representing runtime info for elastic agents'() {
        def requestJSON = "{\n" +
                "  \"agentRuntimeInfo\": {\n" +
                "    \"type\": \"ElasticAgentRuntimeInfo\",\n" +
                "    \"elasticAgentId\": \"elastic_agent_id\",\n" +
                "    \"elasticPluginId\": \"plugin_id\",\n" +
                "    \"identifier\": {\n" +
                "      \"hostName\": \"localhost\",\n" +
                "      \"ipAddress\": \"176.19.4.1\",\n" +
                "      \"uuid\": \"uuid\"\n" +
                "    },\n" +
                "    \"runtimeStatus\": \"Idle\",\n" +
                "    \"buildingInfo\": {\n" +
                "      \"buildingInfo\": \"\",\n" +
                "      \"buildLocator\": \"\"\n" +
                "    },\n" +
                "    \"location\": \"/some/random/location\",\n" +
                "    \"usableSpace\": 10,\n" +
                "    \"operatingSystemName\": \"Mac OS X\",\n" +
                "    \"agentBootstrapperVersion\": \"20.1.0\",\n" +
                "    \"agentVersion\": \"20.9.0\"\n" +
                "  }\n" +
                "}"
        def agent = new Agent("uuid", "localhost", "176.19.4.1")
        def expectedRuntimeInfo = ElasticAgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
                "elastic_agent_id", "plugin_id", "20.1.0", "20.9.0")
        expectedRuntimeInfo.setUsableSpace(10L)
        expectedRuntimeInfo.setLocation("/some/random/location")
        expectedRuntimeInfo.setOperatingSystem("Mac OS X")

        def request = GetWorkRequestRepresenter.fromJSON(requestJSON)

        assertThat(request.getAgentRuntimeInfo()).isEqualTo(expectedRuntimeInfo)
    }
}
