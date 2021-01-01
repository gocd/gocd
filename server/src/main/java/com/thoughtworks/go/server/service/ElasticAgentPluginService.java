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
package com.thoughtworks.go.server.service;

import com.google.common.collect.Sets;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.access.exceptions.SecretResolutionFailureException;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.JobInstanceSqlMapDao;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.exceptions.RulesViolationException;
import com.thoughtworks.go.server.messaging.JobStatusMessage;
import com.thoughtworks.go.server.messaging.JobStatusTopic;
import com.thoughtworks.go.server.messaging.elasticagents.CreateAgentMessage;
import com.thoughtworks.go.server.messaging.elasticagents.CreateAgentQueueHandler;
import com.thoughtworks.go.server.messaging.elasticagents.ServerPingMessage;
import com.thoughtworks.go.server.messaging.elasticagents.ServerPingQueueHandler;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.utils.Timeout;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static com.thoughtworks.go.serverhealth.HealthStateScope.forJob;
import static com.thoughtworks.go.serverhealth.HealthStateType.general;
import static com.thoughtworks.go.serverhealth.ServerHealthState.error;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

@Service
public class ElasticAgentPluginService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticAgentPluginService.class);

    private final PluginManager pluginManager;
    private ElasticAgentPluginRegistry elasticAgentPluginRegistry;
    private final AgentService agentService;
    private final EnvironmentConfigService environmentConfigService;
    private CreateAgentQueueHandler createAgentQueue;
    private final ServerPingQueueHandler serverPingQueue;
    private final GoConfigService goConfigService;
    private final TimeProvider timeProvider;
    private final ServerHealthService serverHealthService;
    private final ConcurrentHashMap<Long, Long> jobCreationTimeMap = new ConcurrentHashMap<>();
    private final ScheduleService scheduleService;
    private ConsoleService consoleService;
    private EphemeralAutoRegisterKeyService ephemeralAutoRegisterKeyService;
    private final SecretParamResolver secretParamResolver;
    private JobInstanceSqlMapDao jobInstanceSqlMapDao = null;
    private JobStatusTopic jobStatusTopic;

    @Value("${go.elasticplugin.heartbeat.interval}")
    private long elasticPluginHeartBeatInterval;
    private final ElasticAgentMetadataStore elasticAgentMetadataStore;
    private ClusterProfilesService clusterProfilesService;

    @Deprecated
    // for test only
    public void setElasticPluginHeartBeatInterval(long elasticPluginHeartBeatInterval) {
        this.elasticPluginHeartBeatInterval = elasticPluginHeartBeatInterval;
    }

    @Autowired
    public ElasticAgentPluginService(
            PluginManager pluginManager, ElasticAgentPluginRegistry elasticAgentPluginRegistry,
            AgentService agentService, EnvironmentConfigService environmentConfigService,
            CreateAgentQueueHandler createAgentQueue, ServerPingQueueHandler serverPingQueue,
            GoConfigService goConfigService, TimeProvider timeProvider, ClusterProfilesService clusterProfilesService,
            ServerHealthService serverHealthService, JobInstanceSqlMapDao jobInstanceSqlMapDao, ScheduleService scheduleService,
            ConsoleService consoleService, EphemeralAutoRegisterKeyService ephemeralAutoRegisterKeyService, SecretParamResolver secretParamResolver,
            JobStatusTopic jobStatusTopic) {

        this(pluginManager, elasticAgentPluginRegistry, agentService, environmentConfigService, createAgentQueue,
                serverPingQueue, goConfigService, timeProvider, serverHealthService, ElasticAgentMetadataStore.instance(),
                clusterProfilesService, jobInstanceSqlMapDao, scheduleService, consoleService, ephemeralAutoRegisterKeyService, secretParamResolver, jobStatusTopic);
    }

    ElasticAgentPluginService(
            PluginManager pluginManager, ElasticAgentPluginRegistry elasticAgentPluginRegistry,
            AgentService agentService, EnvironmentConfigService environmentConfigService,
            CreateAgentQueueHandler createAgentQueue, ServerPingQueueHandler serverPingQueue,
            GoConfigService goConfigService, TimeProvider timeProvider, ServerHealthService serverHealthService,
            ElasticAgentMetadataStore elasticAgentMetadataStore, ClusterProfilesService clusterProfilesService,
            JobInstanceSqlMapDao jobInstanceSqlMapDao, ScheduleService scheduleService, ConsoleService consoleService,
            EphemeralAutoRegisterKeyService ephemeralAutoRegisterKeyService,
            SecretParamResolver secretParamResolver, JobStatusTopic jobStatusTopic) {
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
        this.scheduleService = scheduleService;
        this.consoleService = consoleService;
        this.ephemeralAutoRegisterKeyService = ephemeralAutoRegisterKeyService;
        this.secretParamResolver = secretParamResolver;
        this.jobStatusTopic = jobStatusTopic;
    }

    public void heartbeat() {
        LinkedMultiValueMap<String, ElasticAgentMetadata> elasticAgentsOfMissingPlugins = agentService.allElasticAgents();
//      pingMessage TTL is set lesser than elasticPluginHeartBeatInterval to ensure there aren't multiple ping request for the same plugin
        long pingMessageTimeToLive = elasticPluginHeartBeatInterval - 10000L;

        for (PluginDescriptor descriptor : elasticAgentPluginRegistry.getPlugins()) {
            elasticAgentsOfMissingPlugins.remove(descriptor.id());
            List<ClusterProfile> clusterProfiles = clusterProfilesService.getPluginProfiles().findByPluginId(descriptor.id());
            boolean secretsResolved = resolveSecrets(descriptor.id(), clusterProfiles);
            if (!secretsResolved) continue;
            serverPingQueue.post(new ServerPingMessage(descriptor.id(), clusterProfiles), pingMessageTimeToLive);
            serverHealthService.removeByScope(scope(descriptor.id()));
        }

        if (!elasticAgentsOfMissingPlugins.isEmpty()) {
            for (String pluginId : elasticAgentsOfMissingPlugins.keySet()) {
                Collection<String> uuids = elasticAgentsOfMissingPlugins.get(pluginId).stream().map(ElasticAgentMetadata::uuid).collect(toList());
                String description = format("Elastic agent plugin with identifier %s has gone missing, but left behind %s agent(s) with UUIDs %s.", pluginId, elasticAgentsOfMissingPlugins.get(pluginId).size(), uuids);
                serverHealthService.update(ServerHealthState.warning("Elastic agents with no matching plugins", description, general(scope(pluginId))));
                LOGGER.warn(description);
            }
        }
    }

    @Deprecated
    // for test only
    public void setElasticAgentPluginRegistry(ElasticAgentPluginRegistry elasticAgentPluginRegistry) {
        this.elasticAgentPluginRegistry = elasticAgentPluginRegistry;
    }

    @Deprecated
    // for test only
    public void setCreateAgentQueue(CreateAgentQueueHandler createAgentQueue) {
        this.createAgentQueue = createAgentQueue;
    }

    @Deprecated
    // for test only
    protected void setEphemeralAutoRegisterKeyService(EphemeralAutoRegisterKeyService ephemeralAutoRegisterKeyService) {
        this.ephemeralAutoRegisterKeyService = ephemeralAutoRegisterKeyService;
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

        List<JobPlan> plansThatRequireElasticAgent = jobsThatRequireAgent.stream().filter(isElasticAgent()).collect(toList());
//      messageTimeToLive is lesser than the starvation threshold to ensure there are no duplicate create agent message
        long messageTimeToLive = goConfigService.elasticJobStarvationThreshold() - 10000;

        for (JobPlan plan : plansThatRequireElasticAgent) {
            jobCreationTimeMap.put(plan.getJobId(), timeProvider.currentTimeMillis());
            ElasticProfile elasticProfile = plan.getElasticProfile();
            ClusterProfile clusterProfile = plan.getClusterProfile();
            JobIdentifier jobIdentifier = plan.getIdentifier();
            if (clusterProfile == null) {
                String cancellationMessage = "\nThis job was cancelled by GoCD. The version of your GoCD server requires elastic profiles to be associated with a cluster(required from Version 19.3.0). " +
                        "This job is configured to run on an Elastic Agent, but the associated elastic profile does not have information about the cluster.  \n\n" +
                        "The possible reason for the missing cluster information on the elastic profile could be, an upgrade of the GoCD server to a version >= 19.3.0 before the completion of the job.\n\n" +
                        "A re-run of this job should fix this issue.";
                logToJobConsole(jobIdentifier, cancellationMessage);
                scheduleService.cancelJob(jobIdentifier);
            } else if (elasticAgentPluginRegistry.has(clusterProfile.getPluginId())) {
                String environment = environmentConfigService.envForPipeline(plan.getPipelineName());
                try {
                    resolveSecrets(clusterProfile, elasticProfile);
                    createAgentQueue.post(new CreateAgentMessage(ephemeralAutoRegisterKeyService.autoRegisterKey(), environment, elasticProfile, clusterProfile, jobIdentifier), messageTimeToLive);
                    serverHealthService.removeByScope(scopeForJob(jobIdentifier));
                } catch (RulesViolationException | SecretResolutionFailureException e) {
                    JobInstance jobInstance = jobInstanceSqlMapDao.buildById(plan.getJobId());
                    String failureMessage = format("\nThis job was failed by GoCD. This job is configured to run on an elastic agent, there were errors while resolving secrets for the the associated elastic configurations.\nReasons: %s", e.getMessage());
                    logToJobConsole(jobIdentifier, failureMessage);
                    scheduleService.failJob(jobInstance);
                    jobStatusTopic.post(new JobStatusMessage(jobIdentifier, jobInstance.getState(), plan.getAgentUuid()));
                }
            } else {
                String jobConfigIdentifier = jobIdentifier.jobConfigIdentifier().toString();
                String description = format("Plugin [%s] associated with %s is missing. Either the plugin is not " +
                        "installed or could not be registered. Please check plugins tab " +
                        "and server logs for more details.", clusterProfile.getPluginId(), jobConfigIdentifier);
                serverHealthService.update(error(format("Unable to find agent for %s",
                        jobConfigIdentifier), description, general(scopeForJob(jobIdentifier))));
                LOGGER.error(description);
            }
        }
    }

    public boolean shouldAssignWork(ElasticAgentMetadata metadata, String environment, ElasticProfile elasticProfile, ClusterProfile clusterProfile, JobIdentifier identifier) {
        if (clusterProfile == null || !StringUtils.equals(clusterProfile.getPluginId(), metadata.elasticPluginId())) {
            return false;
        }

        resolveSecrets(clusterProfile, elasticProfile);
        Map<String, String> clusterProfileProperties = clusterProfile.getConfigurationAsMap(true, true);
        GoPluginDescriptor pluginDescriptor = pluginManager.getPluginDescriptorFor(metadata.elasticPluginId());
        Map<String, String> configuration = elasticProfile.getConfigurationAsMap(true, true);

        return elasticAgentPluginRegistry.shouldAssignWork(pluginDescriptor, toAgentMetadata(metadata), environment, configuration, clusterProfileProperties, identifier);
    }

    public String getPluginStatusReport(String pluginId) {
        final ElasticAgentPluginInfo pluginInfo = elasticAgentMetadataStore.getPluginInfo(pluginId);
        if (pluginInfo == null) {
            throw new RecordNotFoundException(format("Plugin with id: '%s' is not found.", pluginId));
        }
        if (pluginInfo.getCapabilities().supportsPluginStatusReport()) {
            List<Map<String, String>> clusterProfiles = clusterProfilesService.getPluginProfiles().findByPluginId(pluginId)
                    .stream()
                    .map((profile) -> {
                        secretParamResolver.resolve(profile);
                        return profile.getConfigurationAsMap(true, true);
                    })
                    .collect(toList());
            return elasticAgentPluginRegistry.getPluginStatusReport(pluginId, clusterProfiles);
        }

        throw new UnsupportedOperationException("Plugin does not plugin support status report.");
    }

    public String getAgentStatusReport(String pluginId, JobIdentifier jobIdentifier, String elasticAgentId) throws Exception {
        final ElasticAgentPluginInfo pluginInfo = elasticAgentMetadataStore.getPluginInfo(pluginId);
        if (pluginInfo == null) {
            throw new RecordNotFoundException(format("Plugin with id: '%s' is not found.", pluginId));
        }
        if (pluginInfo.getCapabilities().supportsAgentStatusReport()) {
            JobPlan jobPlan = jobInstanceSqlMapDao.loadPlan(jobIdentifier.getId());
            if (jobPlan != null) {
                ClusterProfile clusterProfile = jobPlan.getClusterProfile();
                Map<String, String> clusterProfileConfigurations = emptyMap();
                if (clusterProfile != null) {
                    secretParamResolver.resolve(clusterProfile);
                    clusterProfileConfigurations = clusterProfile.getConfigurationAsMap(true, true);
                }
                return elasticAgentPluginRegistry.getAgentStatusReport(pluginId, jobIdentifier, elasticAgentId, clusterProfileConfigurations);
            }
            throw new Exception(format("Could not fetch agent status report for agent %s as either the job running on the agent has been completed or the agent has been terminated.", elasticAgentId));
        }

        throw new UnsupportedOperationException("Plugin does not support agent status report.");
    }

    public String getClusterStatusReport(String pluginId, String clusterProfileId) {
        final ElasticAgentPluginInfo pluginInfo = elasticAgentMetadataStore.getPluginInfo(pluginId);
        if (pluginInfo == null) {
            throw new RecordNotFoundException(format("Plugin with id: '%s' is not found.", pluginId));
        }
        if (pluginInfo.getCapabilities().supportsClusterStatusReport()) {
            ClusterProfile clusterProfile = clusterProfilesService.getPluginProfiles().findByPluginIdAndProfileId(pluginId, clusterProfileId);
            if (clusterProfile == null) {
                throw new RecordNotFoundException(format("Cluster profile with id: '%s' is not found.", clusterProfileId));
            }
            secretParamResolver.resolve(clusterProfile);
            return elasticAgentPluginRegistry.getClusterStatusReport(pluginId, clusterProfile.getConfigurationAsMap(true, true));
        }

        throw new UnsupportedOperationException("Plugin does not support cluster status report.");
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
        JobIdentifier jobIdentifier = job.getIdentifier();
        ElasticProfile elasticProfile = job.getPlan().getElasticProfile();
        ClusterProfile clusterProfile = job.getPlan().getClusterProfile();
        try {
            secretParamResolver.resolve(elasticProfile);
            Map<String, String> elasticProfileConfiguration = elasticProfile.getConfigurationAsMap(true, true);
            Map<String, String> clusterProfileConfiguration = emptyMap();
            if (clusterProfile != null) {
                secretParamResolver.resolve(clusterProfile);
                clusterProfileConfiguration = clusterProfile.getConfigurationAsMap(true, true);
            }
            elasticAgentPluginRegistry.reportJobCompletion(pluginId, elasticAgentId, jobIdentifier, elasticProfileConfiguration, clusterProfileConfiguration);
        } catch (RulesViolationException | SecretResolutionFailureException e) {
            String description = format("The job completion call to the plugin for the job identifier [%s] failed for secrets resolution: %s ", jobIdentifier.toString(), e.getMessage());
            ServerHealthState healthState = error("Failed to notify plugin", description, general(scopeForJob(jobIdentifier)));
            healthState.setTimeout(Timeout.FIVE_MINUTES);
            serverHealthService.update(healthState);
            LOGGER.error(description);
        }
    }

    private void logToJobConsole(JobIdentifier identifier, String message) {
        try {
            consoleService.appendToConsoleLog(identifier, message);
        } catch (IllegalArtifactLocationException | IOException e) {
            LOGGER.error(format("Failed to add message(%s) to the job(%s) console", message, identifier), e);
        }
    }

    private Predicate<JobPlan> isElasticAgent() {
        return JobPlan::requiresElasticAgent;
    }

    private HealthStateScope scope(String pluginId) {
        return HealthStateScope.aboutPlugin(pluginId, "missingPlugin");
    }

    @NotNull
    private HealthStateScope scopeForJob(JobIdentifier jobIdentifier) {
        return forJob(jobIdentifier.getPipelineName(), jobIdentifier.getStageName(), jobIdentifier.getBuildName());
    }

    private boolean resolveSecrets(String pluginId, List<ClusterProfile> clusterProfiles) {
        try {
            for (ClusterProfile clusterProfile : clusterProfiles) {
                secretParamResolver.resolve(clusterProfile);
            }
        } catch (RulesViolationException | SecretResolutionFailureException e) {
            String description = format("Secrets resolution failed for cluster profile associated with plugin [%s]: %s ", pluginId, e.getMessage());
            serverHealthService.update(error(format("Ping failed for '%s' plugin", pluginId), description, general(scope(pluginId))));
            LOGGER.error(description);
            return false;
        }
        return true;
    }

    private void resolveSecrets(ClusterProfile clusterProfile, ElasticProfile elasticProfile) {
        if (clusterProfile != null)
            secretParamResolver.resolve(clusterProfile);
        secretParamResolver.resolve(elasticProfile);
    }
}
