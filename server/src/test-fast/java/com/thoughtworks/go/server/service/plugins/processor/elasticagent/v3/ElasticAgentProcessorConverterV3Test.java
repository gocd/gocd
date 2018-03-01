/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.plugins.processor.elasticagent.v3;

import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class ElasticAgentProcessorConverterV3Test {
    private ElasticAgentProcessorConverterV3 elasticAgentProcessorConverterV3;

    @Before
    public void setUp() throws Exception {
        elasticAgentProcessorConverterV3 = new ElasticAgentProcessorConverterV3();
    }

    @Test
    public void shouldJsonizeAgentMetadataListConvertAgentMetadataList() throws JSONException {
        final List<AgentMetadata> agentMetadataList = Arrays.asList(
                new AgentMetadata("foo-id", "Idle", "Idle", "Enabled"),
                new AgentMetadata("bar-id", "Idle", "Building", "Enabled")
        );

        final String responseBody = elasticAgentProcessorConverterV3.listAgentsResponseBody(agentMetadataList);

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

        JSONAssert.assertEquals(expectedStr, responseBody, true);
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

        final Collection<AgentMetadata> agentMetadataList = elasticAgentProcessorConverterV3.deleteAndDisableAgentRequestBody(responseBody);
        assertThat(agentMetadataList, contains(
                new AgentMetadata("foo-id", "Idle", "Idle", "Enabled"),
                new AgentMetadata("bar-id", "Idle", "Idle", "Enabled")
        ));
    }
}
