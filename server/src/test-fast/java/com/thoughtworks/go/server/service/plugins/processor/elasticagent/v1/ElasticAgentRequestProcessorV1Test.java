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

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.domain.AgentConfigStatus;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.service.AgentService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.util.LinkedMultiValueMap;

import static com.thoughtworks.go.server.service.plugins.processor.elasticagent.v1.ElasticAgentProcessorRequestsV1.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ElasticAgentRequestProcessorV1Test {
    @Mock
    private AgentService agentService;
    @Mock
    private GoPluginDescriptor pluginDescriptor;
    @Mock
    DefaultGoApiRequest request;
    private ElasticAgentRequestProcessorV1 processor;


    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(pluginDescriptor.id()).thenReturn("cd.go.example.plugin");

        processor = new ElasticAgentRequestProcessorV1(agentService);
    }

    @Test
    public void shouldProcessListAgentRequest() throws Exception {
        LinkedMultiValueMap<String, ElasticAgentMetadata> allAgents = new LinkedMultiValueMap<>();
        ElasticAgentMetadata agent = new ElasticAgentMetadata("foo", "bar", "cd.go.example.plugin", AgentRuntimeStatus.Building, AgentConfigStatus.Disabled);
        allAgents.put("cd.go.example.plugin", asList(agent));

        when(agentService.allElasticAgents()).thenReturn(allAgents);
        when(request.api()).thenReturn(REQUEST_SERVER_LIST_AGENTS);

        GoApiResponse response = processor.process(pluginDescriptor, request);

        assertThatJson("[{\"agent_id\":\"bar\",\"agent_state\":\"Building\",\"build_state\":\"Building\",\"config_state\":\"Disabled\"}]").isEqualTo(response.responseBody());
    }

    @Test
    public void shouldProcessDisableAgentRequest() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(new Agent("uuid"), null, null);

        when(request.api()).thenReturn(REQUEST_DISABLE_AGENTS);
        when(request.requestBody()).thenReturn("[{\"agent_id\":\"foo\"}]");
        when(agentService.findElasticAgent("foo", "cd.go.example.plugin")).thenReturn(agentInstance);

        processor.process(pluginDescriptor, request);

        verify(agentService, times(1)).findElasticAgent("foo", "cd.go.example.plugin");
        verify(agentService).disableAgents(singletonList(agentInstance.getUuid()));
    }

    @Test
    public void shouldIgnoreDisableAgentRequestInAbsenceOfAgentsMatchingTheProvidedAgentMetadata() {
        when(request.api()).thenReturn(REQUEST_DISABLE_AGENTS);
        when(request.requestBody()).thenReturn("[{\"agent_id\":\"foo\"}]");
        when(agentService.findElasticAgent("foo", "cd.go.example.plugin")).thenReturn(null);

        processor.process(pluginDescriptor, request);

        verify(agentService, times(1)).findElasticAgent("foo", "cd.go.example.plugin");
        verifyNoMoreInteractions(agentService);
    }

    @Test
    public void shouldProcessDeleteAgentRequest() {
        AgentInstance agentInstance = AgentInstance.createFromAgent(new Agent("uuid"), null, null);

        when(request.api()).thenReturn(REQUEST_DELETE_AGENTS);
        when(request.requestBody()).thenReturn("[{\"agent_id\":\"foo\"}]");
        when(agentService.findElasticAgent("foo", "cd.go.example.plugin")).thenReturn(agentInstance);

        processor.process(pluginDescriptor, request);

        verify(agentService, times(1)).findElasticAgent("foo", "cd.go.example.plugin");
        verify(agentService, times(1)).deleteAgentsWithoutValidations(eq(singletonList(agentInstance.getUuid())));
    }

    @Test
    public void shouldIgnoreDeleteAgentRequestInAbsenceOfAgentsMatchingTheProvidedAgentMetadata() {
        when(request.api()).thenReturn(REQUEST_DELETE_AGENTS);
        when(request.requestBody()).thenReturn("[{\"agent_id\":\"foo\"}]");
        when(agentService.findElasticAgent("foo", "cd.go.example.plugin")).thenReturn(null);

        processor.process(pluginDescriptor, request);

        verify(agentService, times(1)).findElasticAgent("foo", "cd.go.example.plugin");
        verifyNoMoreInteractions(agentService);
    }
}
