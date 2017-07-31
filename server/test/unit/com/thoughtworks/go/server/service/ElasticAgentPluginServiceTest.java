/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.*;
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
import com.thoughtworks.go.serverhealth.HealthStateScope;
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
import java.util.UUID;

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
        plugins.add(new GoPluginDescriptor("docker", null, null, null, null, true));
        when(registry.getPlugins()).thenReturn(plugins);
        when(registry.has("docker")).thenReturn(true);
        when(registry.has("p1")).thenReturn(true);
        when(registry.has("p2")).thenReturn(true);
        when(registry.has("missing")).thenReturn(false);
        when(agentService.allElasticAgents()).thenReturn(new LinkedMultiValueMap<>());
        timeProvider = new TimeProvider();
        service = new ElasticAgentPluginService(pluginManager, registry, agentService, environmentConfigService, createAgentQueue, serverPingQueue, serverConfigService, timeProvider, serverHealthService);
        when(serverConfigService.getAutoregisterKey()).thenReturn(autoRegisterKey);
    }

    @Test
    public void shouldSendServerHeartbeatToAllElasticPlugins() {
        service.heartbeat();

        ArgumentCaptor<ServerPingMessage> captor = ArgumentCaptor.forClass(ServerPingMessage.class);
        ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);
        verify(serverPingQueue, times(3)).post(captor.capture(), ttl.capture());
        List<ServerPingMessage> messages = captor.getAllValues();
        assertThat(messages.contains(new ServerPingMessage("p1")), is(true));
        assertThat(messages.contains(new ServerPingMessage("p2")), is(true));
        assertThat(messages.contains(new ServerPingMessage("docker")), is(true));
    }

    @Test
    public void shouldSendServerHeartBeatMessageWithTimeToLive() throws Exception {
        service.setElasticPluginHeartBeatInterval(60000L);
        ArgumentCaptor<ServerPingMessage> captor = ArgumentCaptor.forClass(ServerPingMessage.class);
        ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);

        service.heartbeat();

        verify(serverPingQueue, times(3)).post(captor.capture(), ttl.capture());

        assertThat(ttl.getValue(), is(50000L));
    }

    @Test
    public void shouldCreateAgentForNewlyAddedJobPlansOnly() {
        when(serverConfigService.elasticJobStarvationThreshold()).thenReturn(10000L);
        when(serverConfigService.hasAutoregisterKey()).thenReturn(true);
        JobPlan plan1 = plan(1, "docker");
        JobPlan plan2 = plan(2, "docker");
        ArgumentCaptor<ServerHealthState> captorForHealthState = ArgumentCaptor.forClass(ServerHealthState.class);
        ArgumentCaptor<CreateAgentMessage> createAgentMessageArgumentCaptor = ArgumentCaptor.forClass(CreateAgentMessage.class);
        ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);
        when(environmentConfigService.envForPipeline("pipeline-2")).thenReturn("env-2");
        service.createAgentsFor(Arrays.asList(plan1), Arrays.asList(plan1, plan2));

        verify(createAgentQueue).post(createAgentMessageArgumentCaptor.capture(), ttl.capture());
        CreateAgentMessage createAgentMessage = createAgentMessageArgumentCaptor.getValue();
        assertThat(createAgentMessage.autoregisterKey(), is(autoRegisterKey));
        assertThat(createAgentMessage.pluginId(), is(plan2.getElasticProfile().getPluginId()));
        assertThat(createAgentMessage.configuration(), is(plan2.getElasticProfile().getConfigurationAsMap(true)));
        assertThat(createAgentMessage.environment(), is("env-2"));
    }

    @Test
    public void shouldPostCreateAgentMessageWithTimeToLiveLesserThanJobStarvationThreshold() throws Exception {
        when(serverConfigService.elasticJobStarvationThreshold()).thenReturn(20000L);
        when(serverConfigService.hasAutoregisterKey()).thenReturn(true);
        JobPlan plan1 = plan(1, "docker");
        JobPlan plan2 = plan(2, "docker");

        ArgumentCaptor<CreateAgentMessage> createAgentMessageArgumentCaptor = ArgumentCaptor.forClass(CreateAgentMessage.class);
        ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);
        when(environmentConfigService.envForPipeline("pipeline-2")).thenReturn("env-2");
        service.createAgentsFor(Arrays.asList(plan1), Arrays.asList(plan1, plan2));

        verify(createAgentQueue).post(createAgentMessageArgumentCaptor.capture(), ttl.capture());
        assertThat(ttl.getValue(), is(10000L));
    }

    @Test
    public void shouldRetryCreateAgentForJobThatHasBeenWaitingForAnAgentForALongTime() {
        when(serverConfigService.elasticJobStarvationThreshold()).thenReturn(0L);
        JobPlan plan1 = plan(1, "docker");
        ArgumentCaptor<CreateAgentMessage> captor = ArgumentCaptor.forClass(CreateAgentMessage.class);
        ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);
        service.createAgentsFor(new ArrayList<>(), Arrays.asList(plan1));
        service.createAgentsFor(Arrays.asList(plan1), Arrays.asList(plan1));//invoke create again

        verify(createAgentQueue, times(2)).post(captor.capture(), ttl.capture());
        CreateAgentMessage createAgentMessage = captor.getValue();
        assertThat(createAgentMessage.autoregisterKey(), is(autoRegisterKey));
        assertThat(createAgentMessage.pluginId(), is(plan1.getElasticProfile().getPluginId()));
        assertThat(createAgentMessage.configuration(), is(plan1.getElasticProfile().getConfigurationAsMap(true)));
        verifyNoMoreInteractions(createAgentQueue);
    }

    @Test
    public void shouldReportMissingElasticPlugin(){
        JobPlan plan1 = plan(1, "missing");
        ArgumentCaptor<ServerHealthState> captorForHealthState = ArgumentCaptor.forClass(ServerHealthState.class);
        service.createAgentsFor(new ArrayList<>(), Arrays.asList(plan1));

        verify(serverHealthService).update(captorForHealthState.capture());
        ServerHealthState serverHealthState = captorForHealthState.getValue();
        assertThat(serverHealthState.getDescription(), is("Plugin [missing] associated with JobConfigIdentifier[pipeline-1:stage:job] is missing. Either the plugin is not installed or could not be registered. Please check plugins tab and server logs for more details."));
        assertThat(serverHealthState.getLogLevel(), is(HealthStateLevel.ERROR));
        assertThat(serverHealthState.getMessage(), is("Unable to find agent for JobConfigIdentifier[pipeline-1:stage:job]"));
        verifyZeroInteractions(createAgentQueue);
    }

    @Test
    public void shouldRemoveExistingMissingPluginErrorFromAPreviousAttemptIfThePluginIsNowRegistered(){
        JobPlan plan1 = plan(1, "docker");
        ArgumentCaptor<HealthStateScope> captor = ArgumentCaptor.forClass(HealthStateScope.class);
        ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);

        service.createAgentsFor(new ArrayList<>(), Arrays.asList(plan1));

        verify(createAgentQueue, times(1)).post(any(), ttl.capture());
        verify(serverHealthService).removeByScope(captor.capture());
        HealthStateScope healthStateScope = captor.getValue();
        assertThat(healthStateScope.getScope(), is("pipeline-1/stage/job"));
    }

    @Test
    public void shouldRetryCreateAgentForJobForWhichAssociatedPluginIsMissing() {
        when(serverConfigService.elasticJobStarvationThreshold()).thenReturn(0L);
        JobPlan plan1 = plan(1, "missing");
        service.createAgentsFor(new ArrayList<>(), Arrays.asList(plan1));
        service.createAgentsFor(Arrays.asList(plan1), Arrays.asList(plan1));//invoke create again

        verifyZeroInteractions(createAgentQueue);
        ArgumentCaptor<ServerHealthState> captorForHealthState = ArgumentCaptor.forClass(ServerHealthState.class);
        verify(serverHealthService, times(2)).update(captorForHealthState.capture());
        List<ServerHealthState> allValues = captorForHealthState.getAllValues();
        for (ServerHealthState serverHealthState : allValues) {
            assertThat(serverHealthState.getType().getScope().isForJob(), is(true));
            assertThat(serverHealthState.getType().getScope().getScope(), is("pipeline-1/stage/job"));
        }
    }

    @Test
    public void shouldAssignJobToAnAgentIfThePluginMatchesForTheAgentAndJob_AndThePluginAgreesToTheAssignment(){
        String uuid = UUID.randomUUID().toString();
        String elasticPluginId = "plugin-1";
        ElasticAgentMetadata agentMetadata = new ElasticAgentMetadata(uuid, uuid, elasticPluginId, AgentRuntimeStatus.Idle, AgentConfigStatus.Enabled);
        ElasticProfile elasticProfile = new ElasticProfile("1", elasticPluginId);

        when(registry.shouldAssignWork(any(), any(), any(), any())).thenReturn(true);
        assertThat(service.shouldAssignWork(agentMetadata, null, elasticProfile), is(true));
    }

    @Test
    public void shouldNotAssignJobToAnAgentIfThePluginMatchesForTheAgentAndJob_ButThePluginRefusesToTheAssignment(){
        String uuid = UUID.randomUUID().toString();
        String elasticPluginId = "plugin-1";
        ElasticAgentMetadata agentMetadata = new ElasticAgentMetadata(uuid, uuid, elasticPluginId, AgentRuntimeStatus.Idle, AgentConfigStatus.Enabled);
        ElasticProfile elasticProfile = new ElasticProfile("1", elasticPluginId);
        when(registry.shouldAssignWork(any(), any(), any(), any())).thenReturn(false);

        assertThat(service.shouldAssignWork(agentMetadata, null, elasticProfile), is(false));
    }

    @Test
    public void shouldNotAssignJobToAnAgentBroughtUpByADifferentElasticPlugin(){
        String uuid = UUID.randomUUID().toString();
        ElasticAgentMetadata agentMetadata = new ElasticAgentMetadata(uuid, uuid, "plugin-1", AgentRuntimeStatus.Idle, AgentConfigStatus.Enabled);
        ElasticProfile elasticProfile = new ElasticProfile("1", "plugin-2");
        when(registry.shouldAssignWork(any(), any(), any(), any())).thenReturn(true);

        assertThat(service.shouldAssignWork(agentMetadata, null, elasticProfile), is(false));
    }

    private JobPlan plan(int jobId, String pluginId) {
        ElasticProfile elasticProfile = new ElasticProfile("id", pluginId);
        JobIdentifier identifier = new JobIdentifier("pipeline-" + jobId, 1, "1", "stage", "1", "job");
        return new DefaultJobPlan(null, new ArtifactPlans(), null, jobId, identifier, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), elasticProfile);
    }
}
