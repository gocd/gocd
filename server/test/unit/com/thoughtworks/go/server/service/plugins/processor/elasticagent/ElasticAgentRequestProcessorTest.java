/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.plugins.processor.elasticagent;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.domain.AgentConfigStatus;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.plugin.access.elastic.AgentMetadata;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.service.AgentConfigService;
import com.thoughtworks.go.server.service.AgentService;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Arrays;

import static com.thoughtworks.go.plugin.access.elastic.Constants.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElasticAgentRequestProcessorTest {
    private AgentService agentService = mock(AgentService.class);
    private AgentConfigService agentConfigService = mock(AgentConfigService.class);
    private ElasticAgentExtension extension = new ElasticAgentExtension(null);

    private ElasticAgentRequestProcessor processor = new ElasticAgentRequestProcessor(new PluginRequestProcessorRegistry(), agentService, agentConfigService, extension);
    private GoPluginDescriptor pluginDescriptor = new GoPluginDescriptor("docker", null, null, null, null, false);
    private GoPluginIdentifier pluginIdentifier = new GoPluginIdentifier(EXTENSION_NAME, SUPPORTED_VERSIONS);

    @Test
    public void shouldProcessListAgentRequest() throws Exception {
        LinkedMultiValueMap<String, ElasticAgentMetadata> allAgents = new LinkedMultiValueMap<>();
        ElasticAgentMetadata agent = new ElasticAgentMetadata("foo", "bar", "docker", AgentRuntimeStatus.Building, AgentConfigStatus.Disabled);
        allAgents.put("docker", Arrays.asList(agent));

        when(agentService.allElasticAgents()).thenReturn(allAgents);
        GoApiResponse response = processor.process(pluginDescriptor, new DefaultGoApiRequest(REQUEST_SERVER_LIST_AGENTS, "1.0", pluginIdentifier));

        JSONAssert.assertEquals("[{\"agent_id\":\"bar\",\"agent_state\":\"Building\",\"build_state\":\"Building\",\"config_state\":\"Disabled\"}]", response.responseBody(), true);
    }

    @Test
    public void shouldProcessDisableAgentRequest() throws Exception {
        AgentMetadata agent = new AgentMetadata("foo", null, null, null);
        DefaultGoApiRequest goPluginApiRequest = new DefaultGoApiRequest(PROCESS_DISABLE_AGENTS, "1.0", pluginIdentifier);
        goPluginApiRequest.setRequestBody(extension.getElasticAgentMessageConverter(goPluginApiRequest.apiVersion()).listAgentsResponseBody(Arrays.asList(agent)));


        AgentInstance agentInstance = AgentInstance.createFromConfig(new AgentConfig("uuid"), null);
        when(agentService.findElasticAgent("foo", "docker")).thenReturn(agentInstance);
        processor.process(pluginDescriptor, goPluginApiRequest);
        verify(agentConfigService).disableAgents(processor.usernameFor(pluginDescriptor), agentInstance);
    }

    @Test
    public void shouldProcessDeleteAgentRequest() throws Exception {
        AgentMetadata agent = new AgentMetadata("foo", null, null, null);
        DefaultGoApiRequest goPluginApiRequest = new DefaultGoApiRequest(PROCESS_DELETE_AGENTS, "1.0", pluginIdentifier);
        goPluginApiRequest.setRequestBody(extension.getElasticAgentMessageConverter(goPluginApiRequest.apiVersion()).listAgentsResponseBody(Arrays.asList(agent)));


        AgentInstance agentInstance = AgentInstance.createFromConfig(new AgentConfig("uuid"), null);
        when(agentService.findElasticAgent("foo", "docker")).thenReturn(agentInstance);
        processor.process(pluginDescriptor, goPluginApiRequest);
        verify(agentConfigService).deleteAgents(processor.usernameFor(pluginDescriptor), agentInstance);
    }
}
