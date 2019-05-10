/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.PluginProfiles;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.JobInstanceSqlMapDao;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.util.LinkedMultiValueMap;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class ElasticAgentPluginServiceTest {
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
    private GoConfigService goConfigService;
    @Mock
    private CreateAgentQueueHandler createAgentQueue;
    @Mock
    private ClusterProfilesService clusterProfilesService;

    private TimeProvider timeProvider;
    private String autoRegisterKey = "key";
    private ElasticAgentPluginService service;
    private ElasticAgentMetadataStore elasticAgentMetadataStore;
    private JobInstanceSqlMapDao jobInstanceSqlMapDao;

    @BeforeEach
    void setUp() throws Exception {
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

        elasticAgentMetadataStore = ElasticAgentMetadataStore.instance();
        timeProvider = new TimeProvider();

        jobInstanceSqlMapDao = mock(JobInstanceSqlMapDao.class);
        service = new ElasticAgentPluginService(pluginManager, registry, agentService, environmentConfigService, createAgentQueue, serverPingQueue, goConfigService, timeProvider, serverHealthService, elasticAgentMetadataStore, clusterProfilesService, jobInstanceSqlMapDao);
        when(goConfigService.serverConfig()).thenReturn(GoConfigMother.configWithAutoRegisterKey(autoRegisterKey).server());
    }

    @AfterEach
    void tearDown() {
        elasticAgentMetadataStore.clear();
    }

    @Test
    void shouldSendServerHeartbeatToAllElasticPlugins() {
        ClusterProfiles allClusterProfiles = new ClusterProfiles();
        allClusterProfiles.add(new ClusterProfile("id1", "p1"));
        allClusterProfiles.add(new ClusterProfile("id2", "p2"));
        allClusterProfiles.add(new ClusterProfile("id3", "docker"));
        when(clusterProfilesService.getPluginProfiles()).thenReturn(allClusterProfiles);

        ClusterProfiles p1ClusterProfiles = new ClusterProfiles();
        p1ClusterProfiles.add(new ClusterProfile("id1", "p1"));
        ClusterProfiles p2ClusterProfiles = new ClusterProfiles();
        p2ClusterProfiles.add(new ClusterProfile("id2", "p2"));
        ClusterProfiles dockerClusterProfiles = new ClusterProfiles();
        dockerClusterProfiles.add(new ClusterProfile("id3", "docker"));

        service.heartbeat();

        ArgumentCaptor<ServerPingMessage> captor = ArgumentCaptor.forClass(ServerPingMessage.class);
        ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);
        verify(serverPingQueue, times(3)).post(captor.capture(), ttl.capture());
        List<ServerPingMessage> messages = captor.getAllValues();
        assertThat(messages).hasSize(3)
                .contains(
                        new ServerPingMessage("p1", p1ClusterProfiles),
                        new ServerPingMessage("p2", p2ClusterProfiles),
                        new ServerPingMessage("docker", dockerClusterProfiles)
                );
    }

    @Test
    void shouldSendServerHeartBeatMessageWithTimeToLive() {
        when(clusterProfilesService.getPluginProfiles()).thenReturn(new ClusterProfiles());
        service.setElasticPluginHeartBeatInterval(60000L);
        ArgumentCaptor<ServerPingMessage> captor = ArgumentCaptor.forClass(ServerPingMessage.class);
        ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);

        service.heartbeat();

        verify(serverPingQueue, times(3)).post(captor.capture(), ttl.capture());

        assertThat(ttl.getValue()).isEqualTo(50000L);
    }

    @Test
    void shouldCreateAgentForNewlyAddedJobPlansOnly() {
        JobPlan plan1 = plan(1, "docker");
        JobPlan plan2 = plan(2, "docker");
        when(goConfigService.elasticJobStarvationThreshold()).thenReturn(10000L);
        ClusterProfile clusterProfile = new ClusterProfile(plan1.getElasticProfile().getClusterProfileId(), plan1.getClusterProfile().getPluginId());
        when(clusterProfilesService.findProfile(plan1.getElasticProfile().getClusterProfileId())).thenReturn(clusterProfile);

        ArgumentCaptor<CreateAgentMessage> createAgentMessageArgumentCaptor = ArgumentCaptor.forClass(CreateAgentMessage.class);
        ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);
        when(environmentConfigService.envForPipeline("pipeline-2")).thenReturn("env-2");
        service.createAgentsFor(Arrays.asList(plan1), Arrays.asList(plan1, plan2));

        verify(createAgentQueue).post(createAgentMessageArgumentCaptor.capture(), ttl.capture());
        CreateAgentMessage createAgentMessage = createAgentMessageArgumentCaptor.getValue();
        assertThat(createAgentMessage.autoregisterKey()).isEqualTo(autoRegisterKey);
        assertThat(createAgentMessage.pluginId()).isEqualTo(plan2.getClusterProfile().getPluginId());
        assertThat(createAgentMessage.configuration()).isEqualTo(plan2.getElasticProfile().getConfigurationAsMap(true));
        assertThat(createAgentMessage.environment()).isEqualTo("env-2");
        assertThat(createAgentMessage.jobIdentifier()).isEqualTo(plan2.getIdentifier());
    }

    @Test
    void shouldPostCreateAgentMessageWithTimeToLiveLesserThanJobStarvationThreshold() throws Exception {
        JobPlan plan1 = plan(1, "docker");
        JobPlan plan2 = plan(2, "docker");
        when(goConfigService.elasticJobStarvationThreshold()).thenReturn(20000L);
        ClusterProfile clusterProfile = new ClusterProfile(plan1.getElasticProfile().getClusterProfileId(), plan1.getClusterProfile().getPluginId());
        when(clusterProfilesService.findProfile(plan1.getElasticProfile().getClusterProfileId())).thenReturn(clusterProfile);

        ArgumentCaptor<CreateAgentMessage> createAgentMessageArgumentCaptor = ArgumentCaptor.forClass(CreateAgentMessage.class);
        ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);
        when(environmentConfigService.envForPipeline("pipeline-2")).thenReturn("env-2");
        service.createAgentsFor(Arrays.asList(plan1), Arrays.asList(plan1, plan2));

        verify(createAgentQueue).post(createAgentMessageArgumentCaptor.capture(), ttl.capture());
        assertThat(ttl.getValue()).isEqualTo(10000L);
    }

    @Test
    void shouldRetryCreateAgentForJobThatHasBeenWaitingForAnAgentForALongTime() {
        JobPlan plan1 = plan(1, "docker");

        when(goConfigService.elasticJobStarvationThreshold()).thenReturn(0L);
        ClusterProfile clusterProfile = new ClusterProfile(plan1.getElasticProfile().getClusterProfileId(), plan1.getClusterProfile().getPluginId());
        when(clusterProfilesService.findProfile(plan1.getElasticProfile().getClusterProfileId())).thenReturn(clusterProfile);
        ArgumentCaptor<CreateAgentMessage> captor = ArgumentCaptor.forClass(CreateAgentMessage.class);
        ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);
        service.createAgentsFor(new ArrayList<>(), Arrays.asList(plan1));
        service.createAgentsFor(Arrays.asList(plan1), Arrays.asList(plan1));//invoke create again

        verify(createAgentQueue, times(2)).post(captor.capture(), ttl.capture());
        verifyNoMoreInteractions(createAgentQueue);

        CreateAgentMessage createAgentMessage = captor.getValue();
        assertThat(createAgentMessage.autoregisterKey()).isEqualTo(autoRegisterKey);
        assertThat(createAgentMessage.pluginId()).isEqualTo(plan1.getClusterProfile().getPluginId());
        assertThat(createAgentMessage.configuration()).isEqualTo(plan1.getElasticProfile().getConfigurationAsMap(true));
    }

    @Test
    void shouldReportMissingElasticPlugin() {
        JobPlan plan1 = plan(1, "missing");
        ArgumentCaptor<ServerHealthState> captorForHealthState = ArgumentCaptor.forClass(ServerHealthState.class);
        service.createAgentsFor(new ArrayList<>(), Arrays.asList(plan1));

        verify(serverHealthService).update(captorForHealthState.capture());
        verifyZeroInteractions(createAgentQueue);

        ServerHealthState serverHealthState = captorForHealthState.getValue();

        assertThat(serverHealthState.getLogLevel()).isEqualTo(HealthStateLevel.ERROR);
        assertThat(serverHealthState.getMessage()).isEqualTo("Unable to find agent for JobConfigIdentifier[pipeline-1:stage:job]");
        assertThat(serverHealthState.getDescription()).isEqualTo("Plugin [missing] associated with JobConfigIdentifier[pipeline-1:stage:job] is missing. Either the plugin is not installed or could not be registered. Please check plugins tab and server logs for more details.");
    }

    @Test
    void shouldRemoveExistingMissingPluginErrorFromAPreviousAttemptIfThePluginIsNowRegistered() {
        JobPlan plan1 = plan(1, "docker");
        ClusterProfile clusterProfile = new ClusterProfile(plan1.getElasticProfile().getClusterProfileId(), plan1.getClusterProfile().getPluginId());
        when(clusterProfilesService.findProfile(plan1.getElasticProfile().getClusterProfileId())).thenReturn(clusterProfile);
        ArgumentCaptor<HealthStateScope> captor = ArgumentCaptor.forClass(HealthStateScope.class);
        ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);

        service.createAgentsFor(new ArrayList<>(), Arrays.asList(plan1));

        verify(createAgentQueue, times(1)).post(any(), ttl.capture());
        verify(serverHealthService).removeByScope(captor.capture());
        HealthStateScope healthStateScope = captor.getValue();
        assertThat(healthStateScope.getScope()).isEqualTo("pipeline-1/stage/job");
    }

    @Test
    void shouldRetryCreateAgentForJobForWhichAssociatedPluginIsMissing() {
        when(goConfigService.elasticJobStarvationThreshold()).thenReturn(0L);
        JobPlan plan1 = plan(1, "missing");
        service.createAgentsFor(new ArrayList<>(), Arrays.asList(plan1));
        service.createAgentsFor(Arrays.asList(plan1), Arrays.asList(plan1));//invoke create again

        verifyZeroInteractions(createAgentQueue);
        ArgumentCaptor<ServerHealthState> captorForHealthState = ArgumentCaptor.forClass(ServerHealthState.class);
        verify(serverHealthService, times(2)).update(captorForHealthState.capture());
        List<ServerHealthState> allValues = captorForHealthState.getAllValues();
        for (ServerHealthState serverHealthState : allValues) {
            assertThat(serverHealthState.getType().getScope().isForJob()).isTrue();
            assertThat(serverHealthState.getType().getScope().getScope()).isEqualTo("pipeline-1/stage/job");
        }
    }

    @Test
    void shouldAssignJobToAnAgentIfThePluginMatchesForTheAgentAndJob_AndThePluginAgreesToTheAssignment() {
        String uuid = UUID.randomUUID().toString();
        String elasticPluginId = "plugin-1";

        ElasticAgentMetadata agentMetadata = new ElasticAgentMetadata(uuid, uuid, elasticPluginId, AgentRuntimeStatus.Idle, AgentConfigStatus.Enabled);
        ElasticProfile elasticProfile = new ElasticProfile("1", "clusterProfileId");

        when(registry.shouldAssignWork(any(), any(), any(), any(), any(), any())).thenReturn(true);
        assertThat(service.shouldAssignWork(agentMetadata, null, elasticProfile, new ClusterProfile("clusterProfileId", elasticPluginId), null)).isTrue();
    }

    @Test
    void shouldNotAssignJobToAnAgentIfThePluginMatchesForTheAgentAndJob_ButThePluginRefusesToTheAssignment() {
        String uuid = UUID.randomUUID().toString();
        String elasticPluginId = "plugin-1";
        ElasticAgentMetadata agentMetadata = new ElasticAgentMetadata(uuid, uuid, elasticPluginId, AgentRuntimeStatus.Idle, AgentConfigStatus.Enabled);
        ElasticProfile elasticProfile = new ElasticProfile("1", elasticPluginId);
        when(registry.shouldAssignWork(any(), any(), any(), any(), any(), any())).thenReturn(false);

        assertThat(service.shouldAssignWork(agentMetadata, null, elasticProfile, new ClusterProfile("clusterProfileId", elasticPluginId), null)).isFalse();
    }

    @Test
    void shouldNotAssignJobToAnAgentBroughtUpByADifferentElasticPlugin() {
        String uuid = UUID.randomUUID().toString();
        ElasticAgentMetadata agentMetadata = new ElasticAgentMetadata(uuid, uuid, "plugin-1", AgentRuntimeStatus.Idle, AgentConfigStatus.Enabled);
        ElasticProfile elasticProfile = new ElasticProfile("1", "clusterProfileId");

        assertThat(service.shouldAssignWork(agentMetadata, null, elasticProfile, new ClusterProfile("clusterProfileId", "plugin-2"), null)).isFalse();
        verifyNoMoreInteractions(registry);
    }

    @Test
    void shouldGetAPluginStatusReportWhenPluginSupportsStatusReport() {
        final Capabilities capabilities = new Capabilities(true);
        final GoPluginDescriptor descriptor = new GoPluginDescriptor("cd.go.example.plugin", null, null, null, null, false);
        elasticAgentMetadataStore.setPluginInfo(new ElasticAgentPluginInfo(descriptor, null, null, null, null, capabilities));

        when(clusterProfilesService.getPluginProfiles()).thenReturn(new ClusterProfiles());
        when(registry.getPluginStatusReport(eq("cd.go.example.plugin"), anyList())).thenReturn("<div>This is a plugin status report snippet.</div>");

        final String pluginStatusReport = service.getPluginStatusReport("cd.go.example.plugin");

        assertThat(pluginStatusReport).isEqualTo("<div>This is a plugin status report snippet.</div>");
    }

    @Test
    void shouldPassAlongAllClusterProfilesBelongingToThePluginWhileGettingPluginStatusReport() {
        final Capabilities capabilities = new Capabilities(true);
        final GoPluginDescriptor descriptor = new GoPluginDescriptor("cd.go.example.plugin", null, null, null, null, false);
        elasticAgentMetadataStore.setPluginInfo(new ElasticAgentPluginInfo(descriptor, null, null, null, null, capabilities));

        ClusterProfiles allClusterProfiles = new ClusterProfiles();
        ClusterProfile cluster1 = new ClusterProfile("id1", "cd.go.example.plugin", new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1")));
        ClusterProfile cluster2 = new ClusterProfile("id2", "cd.go.example.plugin2", new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2")));
        allClusterProfiles.add(cluster1);
        allClusterProfiles.add(cluster2);

        when(clusterProfilesService.getPluginProfiles()).thenReturn(allClusterProfiles);
        when(registry.getPluginStatusReport("cd.go.example.plugin", Arrays.asList(cluster1.getConfigurationAsMap(true)))).thenReturn("<div>This is a plugin status report snippet.</div>");

        final String pluginStatusReport = service.getPluginStatusReport("cd.go.example.plugin");

        assertThat(pluginStatusReport).isEqualTo("<div>This is a plugin status report snippet.</div>");
    }

    @Test
    void shouldErrorOutWhenPluginDoesNotSupportStatusReport() {
        final Capabilities capabilities = new Capabilities(false);
        final GoPluginDescriptor descriptor = new GoPluginDescriptor("cd.go.example.plugin", null, null, null, null, false);
        elasticAgentMetadataStore.setPluginInfo(new ElasticAgentPluginInfo(descriptor, null, null, null, null, capabilities));

        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> service.getPluginStatusReport("cd.go.example.plugin"));
        assertThat(exception.getMessage()).isEqualTo("Plugin does not plugin support status report.");
    }

    @Test
    void shouldGetAPluginAgentReportWhenPluginSupportsStatusReport() throws Exception {
        final Capabilities capabilities = new Capabilities(false, true);
        final GoPluginDescriptor descriptor = new GoPluginDescriptor("cd.go.example.plugin", null, null, null, null, false);
        elasticAgentMetadataStore.setPluginInfo(new ElasticAgentPluginInfo(descriptor, null, null, null, null, capabilities));

        JobIdentifier jobIdentifier = mock(JobIdentifier.class);
        ClusterProfile clusterProfile = mock(ClusterProfile.class);
        JobPlan jobPlan = mock(JobPlan.class);

        when(jobIdentifier.getId()).thenReturn(2L);
        when(jobInstanceSqlMapDao.loadPlan(jobIdentifier.getId())).thenReturn(jobPlan);
        when(jobPlan.getClusterProfile()).thenReturn(clusterProfile);
        when(registry.getAgentStatusReport("cd.go.example.plugin", jobIdentifier, "some-id", clusterProfile.getConfigurationAsMap(true)))
                .thenReturn("<div>This is a agent status report snippet.</div>");

        final String agentStatusReport = service.getAgentStatusReport("cd.go.example.plugin", jobIdentifier, "some-id");

        assertThat(agentStatusReport).isEqualTo("<div>This is a agent status report snippet.</div>");
    }

    @Test
    void shouldErrorOutWhenPluginDoesNotAgentSupportStatusReport() {
        final Capabilities capabilities = new Capabilities(true, false);
        final GoPluginDescriptor descriptor = new GoPluginDescriptor("cd.go.example.plugin", null, null, null, null, false);
        elasticAgentMetadataStore.setPluginInfo(new ElasticAgentPluginInfo(descriptor, null, null, null, null, capabilities));

        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> service.getAgentStatusReport("cd.go.example.plugin", null, null));
        assertThat(exception.getMessage()).isEqualTo("Plugin does not support agent status report.");
    }

    @Test
    void shouldRaiseExceptionIfJobPlanIsNull() {
        final Capabilities capabilities = new Capabilities(false, true);
        final GoPluginDescriptor descriptor = new GoPluginDescriptor("cd.go.example.plugin", null, null, null, null, false);
        elasticAgentMetadataStore.setPluginInfo(new ElasticAgentPluginInfo(descriptor, null, null, null, null, capabilities));

        JobIdentifier jobIdentifier = mock(JobIdentifier.class);

        when(jobIdentifier.getId()).thenReturn(2L);
        when(jobInstanceSqlMapDao.loadPlan(jobIdentifier.getId())).thenReturn(null);

        final Exception exception = assertThrows(Exception.class, () -> service.getAgentStatusReport("cd.go.example.plugin", jobIdentifier, "some-id"));

        assertThat(exception.getMessage()).isEqualTo("Could not fetch agent status report for agent some-id as either the job running on the agent has been completed or the agent has been terminated.");
        verifyZeroInteractions(registry);
    }

    @Test
    void shouldGetAPluginClusterReportWhenPluginSupportsStatusReport() {
        final Capabilities capabilities = new Capabilities(false, true, false);
        final GoPluginDescriptor descriptor = new GoPluginDescriptor("cd.go.example.plugin", null, null, null, null, false);
        elasticAgentMetadataStore.setPluginInfo(new ElasticAgentPluginInfo(descriptor, null, null, null, null, capabilities));

        ClusterProfile clusterProfile = new ClusterProfile("cluster-profile-id", "cd.go.example.plugin");
        clusterProfile.addNewConfigurationWithValue("go-server-url", "server-url", false);

        PluginProfiles<ClusterProfile> clusterProfiles = new ClusterProfiles(clusterProfile);
        when(clusterProfilesService.getPluginProfiles()).thenReturn(clusterProfiles);

        when(registry.getClusterStatusReport("cd.go.example.plugin", clusterProfile.getConfigurationAsMap(true)))
                .thenReturn("<div>This is a cluster status report snippet.</div>");

        final String clusterStatusReport = service.getClusterStatusReport("cd.go.example.plugin", "cluster-profile-id");

        assertThat(clusterStatusReport).isEqualTo("<div>This is a cluster status report snippet.</div>");
    }

    @Test
    void shouldErrorOutWhenPluginDoesNotClusterSupportStatusReport() {
        final Capabilities capabilities = new Capabilities(true, false, false);
        final GoPluginDescriptor descriptor = new GoPluginDescriptor("cd.go.example.plugin", null, null, null, null, false);
        elasticAgentMetadataStore.setPluginInfo(new ElasticAgentPluginInfo(descriptor, null, null, null, null, capabilities));

        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> service.getClusterStatusReport("cd.go.example.plugin", null));
        assertThat(exception.getMessage()).isEqualTo("Plugin does not support cluster status report.");


    }

    @Test
    void shouldErrorOutWhenClusterProfileNotFound() {
        final Capabilities capabilities = new Capabilities(true, true, false);
        final GoPluginDescriptor descriptor = new GoPluginDescriptor("cd.go.example.plugin", null, null, null, null, false);
        elasticAgentMetadataStore.setPluginInfo(new ElasticAgentPluginInfo(descriptor, null, null, null, null, capabilities));
        ClusterProfile clusterProfile = new ClusterProfile("cluster-profile-id", "cd.go.example.plugin");
        clusterProfile.addNewConfigurationWithValue("go-server-url", "server-url", false);
        PluginProfiles<ClusterProfile> clusterProfiles = new ClusterProfiles(clusterProfile);
        when(clusterProfilesService.getPluginProfiles()).thenReturn(clusterProfiles);

        final RecordNotFoundException exception = assertThrows(RecordNotFoundException.class, () -> service.getClusterStatusReport("cd.go.example.plugin", "test"));
        assertThat(exception.getMessage()).isEqualTo("Cluster profile with id: 'test' is not found.");
    }

    @Nested
    class JobCompleted {

        @Test
        void shouldMakeJobCompletionCallToThePluginWhenJobAssignedToAnElastic() {
            ElasticProfile elasticProfile = new ElasticProfile("foo", "clusterId");
            ClusterProfile clusterProfile = new ClusterProfile("clusterId", "docker");

            String elasticAgentId = "i-123456";
            String elasticPluginId = "com.example.aws";

            AgentInstance agent = AgentInstanceMother.idle();
            AgentConfig agentConfig = new AgentConfig(agent.getUuid(), agent.getHostname(), agent.getIpAddress());
            agentConfig.setElasticAgentId(elasticAgentId);
            agentConfig.setElasticPluginId(elasticPluginId);
            agent.syncConfig(agentConfig);

            JobInstance up42_job = JobInstanceMother.completed("up42_job");
            up42_job.setAgentUuid(agent.getUuid());
            DefaultJobPlan plan = new DefaultJobPlan(null, new ArrayList<>(), new ArrayList<>(), -1, null, null, null, new EnvironmentVariables(), elasticProfile, clusterProfile);
            up42_job.setPlan(plan);

            when(agentService.findAgent(agent.getUuid())).thenReturn(agent);
            when(clusterProfilesService.findProfile("clusterId")).thenReturn(clusterProfile);
            Map<String, String> elasticProfileConfiguration = elasticProfile.getConfigurationAsMap(true);
            Map<String, String> clusterProfileConfiguration = clusterProfile.getConfigurationAsMap(true);

            service.jobCompleted(up42_job);

            verify(registry, times(1)).reportJobCompletion(elasticPluginId, elasticAgentId, up42_job.getIdentifier(), elasticProfileConfiguration, clusterProfileConfiguration);
        }

        @Test
        void shouldNotMakeJobCompletionCallToThePluginWhenJobAssignedToNonElastic() {
            AgentInstance agent = AgentInstanceMother.idle();
            JobInstance up42_job = JobInstanceMother.completed("up42_job");
            up42_job.setAgentUuid(agent.getUuid());

            when(agentService.findAgent(agent.getUuid())).thenReturn(agent);

            service.jobCompleted(up42_job);

            verify(registry, times(0)).reportJobCompletion(any(), any(), any(), any(), any());
        }
    }

    private JobPlan plan(int jobId, String pluginId) {
        ClusterProfile clusterProfile = new ClusterProfile("clusterProfileId", pluginId);
        ElasticProfile elasticProfile = new ElasticProfile("id", "clusterProfileId");
        JobIdentifier identifier = new JobIdentifier("pipeline-" + jobId, 1, "1", "stage", "1", "job");
        return new DefaultJobPlan(null, new ArrayList<>(), null, jobId, identifier, null, new EnvironmentVariables(), new EnvironmentVariables(), elasticProfile, clusterProfile);
    }
}
