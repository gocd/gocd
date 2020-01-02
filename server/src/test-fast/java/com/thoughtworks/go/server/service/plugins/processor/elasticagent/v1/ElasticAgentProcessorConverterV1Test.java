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
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class ElasticAgentProcessorConverterV1Test {
    private ElasticAgentProcessorConverterV1 elasticAgentProcessorConverterV1;

    @Before
    public void setUp() throws Exception {
        elasticAgentProcessorConverterV1 = new ElasticAgentProcessorConverterV1();
    }

    @Test
    public void shouldJsonizeAgentMetadataListConvertAgentMetadataList() {
        final List<AgentMetadata> agentMetadataList = Arrays.asList(
                new AgentMetadata("foo-id", "Idle", "Idle", "Enabled"),
                new AgentMetadata("bar-id", "Idle", "Building", "Enabled")
        );

        final String responseBody = elasticAgentProcessorConverterV1.listAgentsResponseBody(agentMetadataList);

        final String expectedStr = "[\n" +
                "  {\n" +
                "    \"agent_id\": \"foo-id\",\n" +
                "    \"agent_state\": \"Idle\",\n" +
                "    \"build_state\": \"Idle\",\n" +
                "    \"config_state\": \"Enabled\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"agent_id\": \"bar-id\",\n" +
                "    \"agent_state\": \"Idle\",\n" +
                "    \"build_state\": \"Building\",\n" +
                "    \"config_state\": \"Enabled\"\n" +
                "  }\n" +
                "]";

        assertThatJson(expectedStr).isEqualTo(responseBody);
    }

    @Test
    public void shouldDeserializeDeleteAndDisableAgentRequestBodyToAgentMetadataList() {
        final String responseBody = "[\n" +
                "  {\n" +
                "    \"agent_id\": \"foo-id\",\n" +
                "    \"agent_state\": \"Idle\",\n" +
                "    \"build_state\": \"Idle\",\n" +
                "    \"config_state\": \"Enabled\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"agent_id\": \"bar-id\",\n" +
                "    \"agent_state\": \"Idle\",\n" +
                "    \"build_state\": \"Idle\",\n" +
                "    \"config_state\": \"Enabled\"\n" +
                "  }\n" +
                "]";

        final Collection<AgentMetadata> agentMetadataList = elasticAgentProcessorConverterV1.deleteAndDisableAgentRequestBody(responseBody);
        assertThat(agentMetadataList, contains(
                new AgentMetadata("foo-id", "Idle", "Idle", "Enabled"),
                new AgentMetadata("bar-id", "Idle", "Idle", "Enabled")
        ));
    }
}
