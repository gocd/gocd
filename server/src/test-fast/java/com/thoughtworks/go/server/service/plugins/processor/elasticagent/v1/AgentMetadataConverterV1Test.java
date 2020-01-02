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
package com.thoughtworks.go.server.service.plugins.processor.elasticagent.v1;

import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AgentMetadataConverterV1Test {

    @Test
    public void fromDTO_shouldConvertToAgentMetadataFromAgentMetadataDTO() {
        final AgentMetadataDTO agentMetadataDTO = new AgentMetadataDTO("agent-id", "Idle", "Building", "Enabled");

        final AgentMetadata agentMetadata = new AgentMetadataConverterV1().fromDTO(agentMetadataDTO);

        assertThat(agentMetadata.elasticAgentId(), is("agent-id"));
        assertThat(agentMetadata.agentState(), is("Idle"));
        assertThat(agentMetadata.buildState(), is("Building"));
        assertThat(agentMetadata.configState(), is("Enabled"));
    }

    @Test
    public void fromDTO_shouldConvertToAgentMetadataDTOFromAgentMetadata() {
        final AgentMetadata agentMetadata = new AgentMetadata("agent-id", "Idle", "Building", "Enabled");

        final AgentMetadataDTO agentMetadataDTO = new AgentMetadataConverterV1().toDTO(agentMetadata);

        assertThat(agentMetadataDTO.elasticAgentId(), is("agent-id"));
        assertThat(agentMetadataDTO.agentState(), is("Idle"));
        assertThat(agentMetadataDTO.buildState(), is("Building"));
        assertThat(agentMetadataDTO.configState(), is("Enabled"));
    }
}