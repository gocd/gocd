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
package com.thoughtworks.go.server.messaging.notifications;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentStatusChangeNotifierTest {
    @Mock
    private NotificationPluginRegistry notificationPluginRegistry;
    @Mock
    private PluginNotificationService pluginNotificationService;

    private AgentStatusChangeNotifier agentStatusChangeNotifier;

    @BeforeEach
    public void setUp() throws Exception {
        agentStatusChangeNotifier = new AgentStatusChangeNotifier(notificationPluginRegistry, pluginNotificationService);
    }

    @Test
    public void shouldNotifyInterestedPluginsWithAgentInformation() {
        AgentInstance agentInstance = AgentInstanceMother.building();
        when(notificationPluginRegistry.isAnyPluginInterestedIn("agent-status")).thenReturn(true);

        agentStatusChangeNotifier.onAgentStatusChange(agentInstance);

        verify(pluginNotificationService).notifyAgentStatus(agentInstance);
}

    @Test
    public void shouldNotifyIfAgentIsElastic() {
        ElasticAgentRuntimeInfo agentRuntimeInfo = new ElasticAgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, "/foo/one", null, "42", "go.cd.elastic-agent-plugin.docker");

        Agent agent = new Agent("some-uuid");
        agent.setElasticAgentId("42");
        agent.setElasticPluginId("go.cd.elastic-agent-plugin.docker");
        agent.setIpaddress("127.0.0.1");

        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, new SystemEnvironment(), mock(AgentStatusChangeListener.class));
        agentInstance.update(agentRuntimeInfo);

        when(notificationPluginRegistry.isAnyPluginInterestedIn("agent-status")).thenReturn(true);

        agentStatusChangeNotifier.onAgentStatusChange(agentInstance);

        verify(pluginNotificationService).notifyAgentStatus(agentInstance);
}

    @Test
    public void shouldNotifyInAbsenceOfPluginsInterestedInAgentStatusNotifications() {
        AgentInstance agentInstance = AgentInstanceMother.building();

        when(notificationPluginRegistry.isAnyPluginInterestedIn("agent-status")).thenReturn(false);

        agentStatusChangeNotifier.onAgentStatusChange(agentInstance);

        verifyNoInteractions(pluginNotificationService);
    }
}
