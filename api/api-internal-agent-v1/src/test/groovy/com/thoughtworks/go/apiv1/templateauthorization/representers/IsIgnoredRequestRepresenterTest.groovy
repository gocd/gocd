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

package com.thoughtworks.go.apiv1.templateauthorization.representers

import com.thoughtworks.go.apiv1.internalagent.representers.GetCookieRequestRepresenter
import com.thoughtworks.go.apiv1.internalagent.representers.IsIgnoredRequestRepresenter
import com.thoughtworks.go.config.Agent
import com.thoughtworks.go.domain.AgentRuntimeStatus
import com.thoughtworks.go.domain.JobIdentifier
import com.thoughtworks.go.remote.request.IsIgnoredRequest
import com.thoughtworks.go.server.service.AgentRuntimeInfo
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo
import org.apache.commons.lang.builder.EqualsBuilder
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory
import static org.assertj.core.api.Assertions.assertThat

class IsIgnoredRequestRepresenterTest {
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
                "  },\n" +
                "  \"jobIdentifier\": {\n" +
                "    \"pipelineName\": \"up_42\",\n" +
                "    \"pipelineCounter\": 100,\n" +
                "    \"pipelineLabel\": \"100\",\n" +
                "    \"stageName\": \"up42_stage\",\n" +
                "    \"buildName\": \"some_job\",\n" +
                "    \"buildId\": 1111,\n" +
                "    \"stageCounter\": \"1\"\n" +
                "  }\n" +
                "}"

        def agent = new Agent("uuid", "localhost", "176.19.4.1")
        def expectedRuntimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
                "20.1.0", "20.9.0")
        expectedRuntimeInfo.setUsableSpace(10L)
        expectedRuntimeInfo.setLocation("/some/random/location")
        expectedRuntimeInfo.setOperatingSystem("Mac OS X")

        def expectedJobIdentifier = new JobIdentifier("up_42", 100, "100", "up42_stage",
                "1", "some_job", 1111L)

        def request = IsIgnoredRequestRepresenter.fromJSON(requestJSON)

        assertThat(request.getAgentRuntimeInfo()).isEqualTo(expectedRuntimeInfo)
        assertThat(request.getJobIdentifier()).isEqualTo(expectedJobIdentifier)
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
                "  },\n" +
                "  \"jobIdentifier\": {\n" +
                "    \"pipelineName\": \"up_42\",\n" +
                "    \"pipelineCounter\": 100,\n" +
                "    \"pipelineLabel\": \"100\",\n" +
                "    \"stageName\": \"up42_stage\",\n" +
                "    \"buildName\": \"some_job\",\n" +
                "    \"buildId\": 1111,\n" +
                "    \"stageCounter\": \"1\"\n" +
                "  }\n" +
                "}"

        def agent = new Agent("uuid", "localhost", "176.19.4.1")
        def expectedRuntimeInfo = ElasticAgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
                "elastic_agent_id", "plugin_id", "20.1.0", "20.9.0")
        expectedRuntimeInfo.setUsableSpace(10L)
        expectedRuntimeInfo.setLocation("/some/random/location")
        expectedRuntimeInfo.setOperatingSystem("Mac OS X")

        def expectedJobIdentifier = new JobIdentifier("up_42", 100, "100", "up42_stage",
                "1", "some_job", 1111L)

        def request = IsIgnoredRequestRepresenter.fromJSON(requestJSON)

        assertThat(request.getAgentRuntimeInfo()).isEqualTo(expectedRuntimeInfo)
        assertThat(request.getJobIdentifier()).isEqualTo(expectedJobIdentifier)
    }

    @Test
    void 'should ensure the serialized and deserialized objects are same'() {
        def agent = new Agent("uuid", "localhost", "176.19.4.1")
        def runtimeInfo = AgentRuntimeInfo.fromAgent(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(),
                "20.1.0", "20.9.0")
        runtimeInfo.setUsableSpace(10L)
        runtimeInfo.setLocation("/some/random/location")
        def jobIdentifier = new JobIdentifier("up_42", 100, "100", "up42_stage",
                "1", "some_job", 1111L)


        def requestJSON = IsIgnoredRequestRepresenter.toJSON(new IsIgnoredRequest(runtimeInfo, jobIdentifier))
        def requestFromAgent = IsIgnoredRequestRepresenter.fromJSON(requestJSON)

        assertThat(EqualsBuilder.reflectionEquals(requestFromAgent, new IsIgnoredRequest(runtimeInfo, jobIdentifier))).isTrue()
    }
}
