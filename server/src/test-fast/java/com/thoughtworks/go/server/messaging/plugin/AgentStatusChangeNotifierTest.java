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

package com.thoughtworks.go.server.messaging.plugin;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.notificationdata.AgentNotificationData;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AgentStatusChangeNotifierTest {
    @Mock
    private NotificationPluginRegistry notificationPluginRegistry;
    @Mock
    private PluginNotificationQueue pluginNotificationQueue;

    private ArgumentCaptor<PluginNotificationMessage> captor;
    private AgentStatusChangeNotifier agentStatusChangeNotifier;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        captor = ArgumentCaptor.forClass(PluginNotificationMessage.class);
        agentStatusChangeNotifier = new AgentStatusChangeNotifier(notificationPluginRegistry, pluginNotificationQueue);
    }

    @Test
    public void shouldNotifyInterestedPluginsWithAgentInformation() {
        AgentInstance agentInstance = AgentInstanceMother.building();

        when(notificationPluginRegistry.isAnyPluginInterestedIn("agent-status")).thenReturn(true);

        agentStatusChangeNotifier.onAgentStatusChange(agentInstance);

        verify(pluginNotificationQueue).post(captor.capture());


        assertThat(captor.getValue().getData() instanceof AgentNotificationData, is(true));
        AgentNotificationData data = (AgentNotificationData) captor.getValue().getData();

        assertThat(data.getUuid(), is(agentInstance.getUuid()));
        assertThat(data.getHostName(), is(agentInstance.getHostname()));
        assertFalse(data.isElastic());
        assertThat(data.getIpAddress(), is(agentInstance.getIpAddress()));
        assertThat(data.getFreeSpace(), is(agentInstance.freeDiskSpace().toString()));
        assertThat(data.getAgentConfigState(), is(agentInstance.getAgentConfigStatus().name()));
        assertThat(data.getAgentState(), is(agentInstance.getRuntimeStatus().agentState().name()));
        assertThat(data.getBuildState(), is(agentInstance.getRuntimeStatus().buildState().name()));
    }

    @Test
    public void shouldNotifyIfAgentIsElastic() throws Exception {
        ElasticAgentRuntimeInfo agentRuntimeInfo = new ElasticAgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, "/foo/one", null, "42", "go.cd.elastic-agent-plugin.docker");
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.setElasticAgentId("42");
        agentConfig.setElasticPluginId("go.cd.elastic-agent-plugin.docker");
        agentConfig.setIpAddress("127.0.0.1");
        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
        agentInstance.update(agentRuntimeInfo);

        when(notificationPluginRegistry.isAnyPluginInterestedIn("agent-status")).thenReturn(true);

        agentStatusChangeNotifier.onAgentStatusChange(agentInstance);

        verify(pluginNotificationQueue).post(captor.capture());

        assertThat(captor.getValue().getData() instanceof AgentNotificationData, is(true));
        AgentNotificationData data = (AgentNotificationData) captor.getValue().getData();

        assertTrue(data.isElastic());
    }

    @Test
    public void shouldNotifyInAbsenceOfPluginsInterestedInAgentStatusNotifications() throws Exception {
        AgentInstance agentInstance = AgentInstanceMother.building();

        when(notificationPluginRegistry.isAnyPluginInterestedIn("agent-status")).thenReturn(false);

        agentStatusChangeNotifier.onAgentStatusChange(agentInstance);

        verifyZeroInteractions(pluginNotificationQueue);
    }
}
