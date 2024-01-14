/*
 * Copyright 2024 Thoughtworks, Inc.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

public class ElasticAgentProcessorConverterV1Test {
    private ElasticAgentProcessorConverterV1 elasticAgentProcessorConverterV1;

    @BeforeEach
    public void setUp() throws Exception {
        elasticAgentProcessorConverterV1 = new ElasticAgentProcessorConverterV1();
    }

    @Test
    public void shouldJsonizeAgentMetadataListConvertAgentMetadataList() {
        final List<AgentMetadata> agentMetadataList = List.of(
                new AgentMetadata("foo-id", "Idle", "Idle", "Enabled"),
                new AgentMetadata("bar-id", "Idle", "Building", "Enabled")
        );

        final String responseBody = elasticAgentProcessorConverterV1.listAgentsResponseBody(agentMetadataList);

        final String expectedStr = """
                [
                  {
                    "agent_id": "foo-id",
                    "agent_state": "Idle",
                    "build_state": "Idle",
                    "config_state": "Enabled"
                  },
                  {
                    "agent_id": "bar-id",
                    "agent_state": "Idle",
                    "build_state": "Building",
                    "config_state": "Enabled"
                  }
                ]""";

        assertThatJson(expectedStr).isEqualTo(responseBody);
    }

    @Test
    public void shouldDeserializeDeleteAndDisableAgentRequestBodyToAgentMetadataList() {
        final String responseBody = """
                [
                  {
                    "agent_id": "foo-id",
                    "agent_state": "Idle",
                    "build_state": "Idle",
                    "config_state": "Enabled"
                  },
                  {
                    "agent_id": "bar-id",
                    "agent_state": "Idle",
                    "build_state": "Idle",
                    "config_state": "Enabled"
                  }
                ]""";

        final Collection<AgentMetadata> agentMetadataList = elasticAgentProcessorConverterV1.deleteAndDisableAgentRequestBody(responseBody);
        assertThat(agentMetadataList, contains(
                new AgentMetadata("foo-id", "Idle", "Idle", "Enabled"),
                new AgentMetadata("bar-id", "Idle", "Idle", "Enabled")
        ));
    }
}
