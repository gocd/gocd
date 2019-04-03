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

import com.google.common.collect.Sets;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.JobInstanceSqlMapDao;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.messaging.elasticagents.CreateAgentMessage;
import com.thoughtworks.go.server.messaging.elasticagents.CreateAgentQueueHandler;
import com.thoughtworks.go.server.messaging.elasticagents.ServerPingMessage;
import com.thoughtworks.go.server.messaging.elasticagents.ServerPingQueueHandler;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
public class ElasticAgentPluginService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticAgentPluginService.class);

    private final PluginManager pluginManager;
    private final ElasticAgentPluginRegistry elasticAgentPluginRegistry;
    private final AgentService agentService;
    private final EnvironmentConfigService environmentConfigService;
    private final CreateAgentQueueHandler createAgentQueue;
    private final ServerPingQueueHandler serverPingQueue;
    private final GoConfigService goConfigService;
    private final TimeProvider timeProvider;
    private final ServerHealthService serverHealthService;
    private final ConcurrentHashMap<Long, Long> jobCreationTimeMap = new ConcurrentHashMap<>();
    private JobInstanceSqlMapDao jobInstanceSqlMapDao = null;

    @Value("${go.elasticplugin.heartbeat.interval}")
    private long elasticPluginHeartBeatInterval;
    private final ElasticAgentMetadataStore elasticAgentMetadataStore;
    private ClusterProfilesService clusterProfilesService;

    //    for test only
    public void setElasticPluginHeartBeatInterval(long elasticPluginHeartBeatInterval) {
        this.elasticPluginHeartBeatInterval = elasticPluginHeartBeatInterval;
    }

    @Autowired
    public ElasticAgentPluginService(
            PluginManager pluginManager, ElasticAgentPluginRegistry elasticAgentPluginRegistry,
            AgentService agentService, EnvironmentConfigService environmentConfigService,
            CreateAgentQueueHandler createAgentQueue, ServerPingQueueHandler serverPingQueue,
            GoConfigService goConfigService, TimeProvider timeProvider, ClusterProfilesService clusterProfilesService, ServerHealthService serverHealthService, JobInstanceSqlMapDao jobInstanceSqlMapDao) {

        this(pluginManager, elasticAgentPluginRegistry, agentService, environmentConfigService, createAgentQueue,
                serverPingQueue, goConfigService, timeProvider, serverHealthService, ElasticAgentMetadataStore.instance(), clusterProfilesService, jobInstanceSqlMapDao);
    }

    ElasticAgentPluginService(
            PluginManager pluginManager, ElasticAgentPluginRegistry elasticAgentPluginRegistry,
            AgentService agentService, EnvironmentConfigService environmentConfigService,
            CreateAgentQueueHandler createAgentQueue, ServerPingQueueHandler serverPingQueue,
            GoConfigService goConfigService, TimeProvider timeProvider, ServerHealthService serverHealthService,
            ElasticAgentMetadataStore elasticAgentMetadataStore, ClusterProfilesService clusterProfilesService, JobInstanceSqlMapDao jobInstanceSqlMapDao) {
        this.pluginManager = pluginManager;
        this.elasticAgentPluginRegistry = elasticAgentPluginRegistry;
        this.agentService = agentService;
        this.environmentConfigService = environmentConfigService;
        this.createAgentQueue = createAgentQueue;
        this.serverPingQueue = serverPingQueue;
        this.goConfigService = goConfigService;
        this.timeProvider = timeProvider;
        this.serverHealthService = serverHealthService;
        this.elasticAgentMetadataStore = elasticAgentMetadataStore;
        this.clusterProfilesService = clusterProfilesService;
        this.jobInstanceSqlMapDao = jobInstanceSqlMapDao;
    }

    public void heartbeat() {
        LinkedMultiValueMap<String, ElasticAgentMetadata> elasticAgentsOfMissingPlugins = agentService.allElasticAgents();
//      pingMessage TTL is set lesser than elasticPluginHeartBeatInterval to ensure there aren't multiple ping request for the same plugin
        long pingMessageTimeToLive = elasticPluginHeartBeatInterval - 10000L;

        for (PluginDescriptor descriptor : elasticAgentPluginRegistry.getPlugins()) {
            ClusterProfiles clusterProfiles = (ClusterProfiles) clusterProfilesService.getPluginProfiles();
            serverPingQueue.post(new ServerPingMessage(descriptor.id(), clusterProfiles), pingMessageTimeToLive);
            elasticAgentsOfMissingPlugins.remove(descriptor.id());
            serverHealthService.removeByScope(scope(descriptor.id()));
        }

        if (!elasticAgentsOfMissingPlugins.isEmpty()) {
            for (String pluginId : elasticAgentsOfMissingPlugins.keySet()) {
                Collection<String> uuids = elasticAgentsOfMissingPlugins.get(pluginId).stream().map(ElasticAgentMetadata::uuid).collect(Collectors.toList());
                String description = format("Elastic agent plugin with identifier %s has gone missing, but left behind %s agent(s) with UUIDs %s.", pluginId, elasticAgentsOfMissingPlugins.get(pluginId).size(), uuids);
                serverHealthService.update(ServerHealthState.warning("Elastic agents with no matching plugins", description, HealthStateType.general(scope(pluginId))));
                LOGGER.warn(description);
            }
        }
    }

    private HealthStateScope scope(String pluginId) {
        return HealthStateScope.aboutPlugin(pluginId, "missingPlugin");
    }

    public static AgentMetadata toAgentMetadata(ElasticAgentMetadata obj) {
        return new AgentMetadata(obj.elasticAgentId(), obj.agentState().toString(), obj.buildState().toString(), obj.configStatus().toString());
    }

    public void createAgentsFor(List<JobPlan> old, List<JobPlan> newPlan) {
        Collection<JobPlan> starvingJobs = new ArrayList<>();
        for (JobPlan jobPlan : newPlan) {
            if (jobPlan.requiresElasticAgent()) {
                if (!jobCreationTimeMap.containsKey(jobPlan.getJobId())) {
                    continue;
                }
                long lastTryTime = jobCreationTimeMap.get(jobPlan.getJobId());
                if ((timeProvider.currentTimeMillis() - lastTryTime) >= goConfigService.elasticJobStarvationThreshold()) {
                    starvingJobs.add(jobPlan);
                }
            }
        }

        ArrayList<JobPlan> jobsThatRequireAgent = new ArrayList<>();
        jobsThatRequireAgent.addAll(Sets.difference(new HashSet<>(newPlan), new HashSet<>(old)));
        jobsThatRequireAgent.addAll(starvingJobs);

        List<JobPlan> plansThatRequireElasticAgent = jobsThatRequireAgent.stream().filter(isElasticAgent()).collect(Collectors.toList());
//      messageTimeToLive is lesser than the starvation threshold to ensure there are no duplicate create agent message
        long messageTimeToLive = goConfigService.elasticJobStarvationThreshold() - 10000;

        for (JobPlan plan : plansThatRequireElasticAgent) {
            jobCreationTimeMap.put(plan.getJobId(), timeProvider.currentTimeMillis());
            ElasticProfile elasticProfile = plan.getElasticProfile();
            if (elasticAgentPluginRegistry.has(elasticProfile.getPluginId())) {
                String environment = environmentConfigService.envForPipeline(plan.getPipelineName());
                ClusterProfile clusterProfile = clusterProfilesService.findProfile(elasticProfile.getClusterProfileId());
                createAgentQueue.post(new CreateAgentMessage(goConfigService.serverConfig().getAgentAutoRegisterKey(), environment, elasticProfile, clusterProfile, plan.getIdentifier()), messageTimeToLive);
                serverHealthService.removeByScope(HealthStateScope.forJob(plan.getIdentifier().getPipelineName(), plan.getIdentifier().getStageName(), plan.getIdentifier().getBuildName()));
            } else {
                String jobConfigIdentifier = plan.getIdentifier().jobConfigIdentifier().toString();
                String description = format("Plugin [%s] associated with %s is missing. Either the plugin is not " +
                        "installed or could not be registered. Please check plugins tab " +
                        "and server logs for more details.", elasticProfile.getPluginId(), jobConfigIdentifier);
                serverHealthService.update(ServerHealthState.error(format("Unable to find agent for %s",
                        jobConfigIdentifier), description, HealthStateType.general(HealthStateScope.forJob(plan.getIdentifier().getPipelineName(), plan.getIdentifier().getStageName(), plan.getIdentifier().getBuildName()))));
                LOGGER.error(description);
            }
        }
    }

    private Predicate<JobPlan> isElasticAgent() {
        return JobPlan::requiresElasticAgent;
    }

    public boolean shouldAssignWork(ElasticAgentMetadata metadata, String environment, ElasticProfile elasticProfile, JobIdentifier identifier) {
        ClusterProfile clusterProfile = clusterProfilesService.findProfile(elasticProfile.getClusterProfileId());
        Map<String, String> clusterProfileProperties = clusterProfile != null ? clusterProfile.getConfigurationAsMap(true) : Collections.EMPTY_MAP;
        GoPluginDescriptor pluginDescriptor = pluginManager.getPluginDescriptorFor(metadata.elasticPluginId());
        Map<String, String> configuration = elasticProfile.getConfigurationAsMap(true);

        if (!StringUtils.equals(elasticProfile.getPluginId(), metadata.elasticPluginId())) {
            return false;
        }

        return elasticAgentPluginRegistry.shouldAssignWork(pluginDescriptor, toAgentMetadata(metadata), environment, configuration, clusterProfileProperties, identifier);
    }

    public String getPluginStatusReport(String pluginId) {
        final ElasticAgentPluginInfo pluginInfo = elasticAgentMetadataStore.getPluginInfo(pluginId);
        if (pluginInfo.getCapabilities().supportsPluginStatusReport()) {
            return elasticAgentPluginRegistry.getPluginStatusReport(pluginId);
        }

        throw new UnsupportedOperationException("Plugin does not plugin support status report.");
    }

    public String getAgentStatusReport(String pluginId, JobIdentifier jobIdentifier, String elasticAgentId) throws Exception {
        final ElasticAgentPluginInfo pluginInfo = elasticAgentMetadataStore.getPluginInfo(pluginId);
        if (pluginInfo.getCapabilities().supportsAgentStatusReport()) {
            JobPlan jobPlan = jobInstanceSqlMapDao.loadPlan(jobIdentifier.getId());
            if (jobPlan != null) {
                ClusterProfile clusterProfile = jobPlan.getClusterProfile();
                Map<String, String> clusterProfileConfigurations = (clusterProfile == null) ? Collections.emptyMap() : clusterProfile.getConfigurationAsMap(true);
                return elasticAgentPluginRegistry.getAgentStatusReport(pluginId, jobIdentifier, elasticAgentId, clusterProfileConfigurations);
            }
            throw new Exception(format("Could not fetch agent status report for agent %s as either the job running on the agent has been completed or the agent has been terminated.", elasticAgentId));
        }

        throw new UnsupportedOperationException("Plugin does not support agent status report.");
    }

    public void jobCompleted(JobInstance job) {
        AgentInstance agentInstance = agentService.findAgent(job.getAgentUuid());
        if (!agentInstance.isElastic()) {
            LOGGER.debug("Agent {} is not elastic. Skipping further execution.", agentInstance.getUuid());
            return;
        }

        if (job.isAssignedToAgent()) {
            jobCreationTimeMap.remove(job.getId());
        }

        String pluginId = agentInstance.elasticAgentMetadata().elasticPluginId();
        String elasticAgentId = agentInstance.elasticAgentMetadata().elasticAgentId();

        ElasticProfile elasticProfile = job.getPlan().getElasticProfile();
        ClusterProfile clusterProfile = job.getPlan().getClusterProfile();
        Map<String, String> elasticProfileConfiguration = elasticProfile.getConfigurationAsMap(true);
        Map<String, String> clusterProfileConfiguration = clusterProfile != null ? clusterProfile.getConfigurationAsMap(true) : Collections.EMPTY_MAP;

        elasticAgentPluginRegistry.reportJobCompletion(pluginId, elasticAgentId, job.getIdentifier(), elasticProfileConfiguration, clusterProfileConfiguration);
    }
}
