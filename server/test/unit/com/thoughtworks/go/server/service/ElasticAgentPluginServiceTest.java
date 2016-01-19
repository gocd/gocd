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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.plugin.access.elastic.AgentMetadata;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.messaging.elasticagents.ServerPingMessage;
import com.thoughtworks.go.server.messaging.elasticagents.ServerPingQueue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.util.LinkedMultiValueMap;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ElasticAgentPluginServiceTest {
    @Mock
    private PluginManager pluginManager;
    @Mock
    private ElasticAgentPluginRegistry registry;
    @Mock
    private AgentService agentService;
    @Mock
    private EnvironmentConfigService environmentConfigService;
    @Mock
    private ServerPingQueue serverPingQueue;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldSendServerHeartbeatToAllElasticPlugins() {
        ArrayList<PluginDescriptor> plugins = new ArrayList<>();
        plugins.add(new GoPluginDescriptor("p1", null, null, null, null, true));
        plugins.add(new GoPluginDescriptor("p2", null, null, null, null, true));
        when(registry.getPlugins()).thenReturn(plugins);
        when(agentService.allElasticAgents()).thenReturn(new LinkedMultiValueMap<String, ElasticAgentMetadata>());
        ElasticAgentPluginService service = new ElasticAgentPluginService(pluginManager, registry, agentService, environmentConfigService, null, serverPingQueue, null);

        service.heartbeat();

        ArgumentCaptor<ServerPingMessage> captor = ArgumentCaptor.forClass(ServerPingMessage.class);
        verify(serverPingQueue, times(2)).post(captor.capture());
        List<ServerPingMessage> messages = captor.getAllValues();
        assertThat(messages.contains(new ServerPingMessage("p1", new ArrayList<AgentMetadata>())), is(true));
        assertThat(messages.contains(new ServerPingMessage("p2", new ArrayList<AgentMetadata>())), is(true));

    }

}
