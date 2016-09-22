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

import com.thoughtworks.go.config.ArtifactPlans;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.DefaultJobPlan;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.messaging.elasticagents.CreateAgentMessage;
import com.thoughtworks.go.server.messaging.elasticagents.CreateAgentQueueHandler;
import com.thoughtworks.go.server.messaging.elasticagents.ServerPingMessage;
import com.thoughtworks.go.server.messaging.elasticagents.ServerPingQueueHandler;
import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.util.LinkedMultiValueMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
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
    private ServerPingQueueHandler serverPingQueue;
    @Mock
    private ServerHealthService serverHealthService;
    @Mock
    private ServerConfigService serverConfigService;
    @Mock
    private CreateAgentQueueHandler createAgentQueue;
    private TimeProvider timeProvider;
    private ElasticAgentPluginService service;
    private String autoRegisterKey = "key";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ArrayList<PluginDescriptor> plugins = new ArrayList<>();
        plugins.add(new GoPluginDescriptor("p1", null, null, null, null, true));
        plugins.add(new GoPluginDescriptor("p2", null, null, null, null, true));
        when(registry.getPlugins()).thenReturn(plugins);
        when(agentService.allElasticAgents()).thenReturn(new LinkedMultiValueMap<String, ElasticAgentMetadata>());
        timeProvider = new TimeProvider();
        service = new ElasticAgentPluginService(pluginManager, registry, agentService, environmentConfigService, createAgentQueue, serverPingQueue, serverConfigService, timeProvider, serverHealthService);
        when(serverConfigService.getAutoregisterKey()).thenReturn(autoRegisterKey);
    }

    @Test
    public void shouldSendServerHeartbeatToAllElasticPlugins() {
        service.heartbeat();

        ArgumentCaptor<ServerPingMessage> captor = ArgumentCaptor.forClass(ServerPingMessage.class);
        verify(serverPingQueue, times(2)).post(captor.capture());
        List<ServerPingMessage> messages = captor.getAllValues();
        assertThat(messages.contains(new ServerPingMessage("p1")), is(true));
        assertThat(messages.contains(new ServerPingMessage("p2")), is(true));
    }

    @Test
    public void shouldNotCreateAgentIfAutoRegisterIsNotSetup() {
        JobPlan plan = plan(1);
        when(serverConfigService.hasAutoregisterKey()).thenReturn(false);
        ArgumentCaptor<ServerHealthState> captor = ArgumentCaptor.forClass(ServerHealthState.class);

        service.createAgentsFor(new ArrayList<JobPlan>(), Arrays.asList(plan));
        verify(serverHealthService).update(captor.capture());
        ServerHealthState serverHealthState = captor.getValue();
        assertThat(serverHealthState.getLogLevel(), is(HealthStateLevel.ERROR));
        assertThat(serverHealthState.getType(), is(HealthStateType.autoregisterKeyRequired()));
        verifyZeroInteractions(createAgentQueue);
    }

    @Test
    public void shouldCreateAgentForNewlyAddedJobPlansOnly() {
        when(serverConfigService.hasAutoregisterKey()).thenReturn(true);
        JobPlan plan1 = plan(1);
        JobPlan plan2 = plan(2);
        ArgumentCaptor<ServerHealthState> captorForHealthState = ArgumentCaptor.forClass(ServerHealthState.class);
        ArgumentCaptor<CreateAgentMessage> captor = ArgumentCaptor.forClass(CreateAgentMessage.class);
        when(environmentConfigService.envForPipeline("pipeline-2")).thenReturn("env-2");
        service.createAgentsFor(Arrays.asList(plan1), Arrays.asList(plan1, plan2));

        verify(createAgentQueue).post(captor.capture());
        CreateAgentMessage createAgentMessage = captor.getValue();
        assertThat(createAgentMessage.autoregisterKey(), is(autoRegisterKey));
        assertThat(createAgentMessage.pluginId(), is(plan2.getElasticProfile().getPluginId()));
        assertThat(createAgentMessage.configuration(), is(plan2.getElasticProfile().getConfigurationAsMap(true)));
        assertThat(createAgentMessage.environment(), is("env-2"));

        verify(serverHealthService).update(captorForHealthState.capture());
        ServerHealthState serverHealthState = captorForHealthState.getValue();
        assertThat(serverHealthState.getLogLevel(), is(HealthStateLevel.OK));
        assertThat(serverHealthState.getType(), is(HealthStateType.autoregisterKeyRequired()));
    }

    @Test
    public void shouldRetryCreateAgentForJobThatHasBeenWaitingForAnAgentForALongTime() {
        when(serverConfigService.hasAutoregisterKey()).thenReturn(true);
        when(serverConfigService.elasticJobStarvationThreshold()).thenReturn(0L);
        JobPlan plan1 = plan(1);
        ArgumentCaptor<ServerHealthState> captorForHealthState = ArgumentCaptor.forClass(ServerHealthState.class);
        ArgumentCaptor<CreateAgentMessage> captor = ArgumentCaptor.forClass(CreateAgentMessage.class);
        service.createAgentsFor(new ArrayList<JobPlan>(), Arrays.asList(plan1));
        service.createAgentsFor(Arrays.asList(plan1), Arrays.asList(plan1));//invoke create again

        verify(createAgentQueue, times(2)).post(captor.capture());
        CreateAgentMessage createAgentMessage = captor.getValue();
        assertThat(createAgentMessage.autoregisterKey(), is(autoRegisterKey));
        assertThat(createAgentMessage.pluginId(), is(plan1.getElasticProfile().getPluginId()));
        assertThat(createAgentMessage.configuration(), is(plan1.getElasticProfile().getConfigurationAsMap(true)));
        verifyNoMoreInteractions(createAgentQueue);
        verify(serverHealthService, times(2)).update(captorForHealthState.capture());
        ServerHealthState serverHealthState = captorForHealthState.getValue();
        assertThat(serverHealthState.getLogLevel(), is(HealthStateLevel.OK));
        assertThat(serverHealthState.getType(), is(HealthStateType.autoregisterKeyRequired()));
    }

    private JobPlan plan(int jobId) {
        ElasticProfile elasticProfile = new ElasticProfile("id", "docker");
        JobIdentifier identifier = new JobIdentifier("pipeline-" + jobId, 1, "1", "stage", "1", "job");
        return new DefaultJobPlan(null, new ArtifactPlans(), null, jobId, identifier, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), elasticProfile);
    }
}
