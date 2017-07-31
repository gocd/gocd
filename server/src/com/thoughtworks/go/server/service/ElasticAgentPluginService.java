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

import com.google.common.collect.Sets;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.messaging.elasticagents.CreateAgentMessage;
import com.thoughtworks.go.server.messaging.elasticagents.CreateAgentQueueHandler;
import com.thoughtworks.go.server.messaging.elasticagents.ServerPingMessage;
import com.thoughtworks.go.server.messaging.elasticagents.ServerPingQueueHandler;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.Filter;
import com.thoughtworks.go.util.ListUtil;
import com.thoughtworks.go.util.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ElasticAgentPluginService implements JobStatusListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticAgentPluginService.class);

    private final PluginManager pluginManager;
    private final ElasticAgentPluginRegistry elasticAgentPluginRegistry;
    private final AgentService agentService;
    private final EnvironmentConfigService environmentConfigService;
    private final CreateAgentQueueHandler createAgentQueue;
    private final ServerPingQueueHandler serverPingQueue;
    private final ServerConfigService serverConfigService;
    private final TimeProvider timeProvider;
    private final ServerHealthService serverHealthService;
    private final ConcurrentHashMap<Long, Long> map = new ConcurrentHashMap<>();

    @Value("${go.elasticplugin.heartbeat.interval}")
    private long elasticPluginHeartBeatInterval;

//    for test only
    public void setElasticPluginHeartBeatInterval(long elasticPluginHeartBeatInterval) {
        this.elasticPluginHeartBeatInterval = elasticPluginHeartBeatInterval;
    }

    @Autowired
    public ElasticAgentPluginService(
            PluginManager pluginManager, ElasticAgentPluginRegistry elasticAgentPluginRegistry,
            AgentService agentService, EnvironmentConfigService environmentConfigService,
            CreateAgentQueueHandler createAgentQueue, ServerPingQueueHandler serverPingQueue,
            ServerConfigService serverConfigService, TimeProvider timeProvider, ServerHealthService serverHealthService) {
        this.pluginManager = pluginManager;
        this.elasticAgentPluginRegistry = elasticAgentPluginRegistry;
        this.agentService = agentService;
        this.environmentConfigService = environmentConfigService;
        this.createAgentQueue = createAgentQueue;
        this.serverPingQueue = serverPingQueue;
        this.serverConfigService = serverConfigService;
        this.timeProvider = timeProvider;
        this.serverHealthService = serverHealthService;
    }

    public void heartbeat() {
        LinkedMultiValueMap<String, ElasticAgentMetadata> elasticAgentsOfMissingPlugins = agentService.allElasticAgents();
//      pingMessage TTL is set lesser than elasticPluginHeartBeatInterval to ensure there aren't multiple ping request for the same plugin
        long pingMessageTimeToLive = elasticPluginHeartBeatInterval - 10000L;

        for (PluginDescriptor descriptor : elasticAgentPluginRegistry.getPlugins()) {
            serverPingQueue.post(new ServerPingMessage(descriptor.id()), pingMessageTimeToLive);
            elasticAgentsOfMissingPlugins.remove(descriptor.id());
            serverHealthService.removeByScope(scope(descriptor.id()));
        }

        if (!elasticAgentsOfMissingPlugins.isEmpty()) {
            for (String pluginId : elasticAgentsOfMissingPlugins.keySet()) {
                Collection<String> uuids = ListUtil.map(elasticAgentsOfMissingPlugins.get(pluginId), new ListUtil.Transformer<ElasticAgentMetadata, String>() {
                    @Override
                    public String transform(ElasticAgentMetadata input) {
                        return input.uuid();
                    }
                });
                String description = String.format("Elastic agent plugin with identifier %s has gone missing, but left behind %s agent(s) with UUIDs %s.", pluginId, elasticAgentsOfMissingPlugins.get(pluginId).size(), uuids);
                serverHealthService.update(ServerHealthState.warning("Elastic agents with no matching plugins", description, HealthStateType.general(scope(pluginId))));
                LOGGER.warn(description);
            }
        }
    }

    private HealthStateScope scope(String pluginId) {
        return HealthStateScope.forPlugin(pluginId, "missingPlugin");
    }

    public static AgentMetadata toAgentMetadata(ElasticAgentMetadata obj) {
        return new AgentMetadata(obj.elasticAgentId(), obj.agentState().toString(), obj.buildState().toString(), obj.configStatus().toString());
    }

    public void createAgentsFor(List<JobPlan> old, List<JobPlan> newPlan) {
        Collection<JobPlan> starvingJobs = new ArrayList<>();
        for (JobPlan jobPlan : newPlan) {
            if (jobPlan.requiresElasticAgent()) {
                if (!map.containsKey(jobPlan.getJobId())) {
                    continue;
                }
                long lastTryTime = map.get(jobPlan.getJobId());
                if ((timeProvider.currentTimeMillis() - lastTryTime) >= serverConfigService.elasticJobStarvationThreshold()) {
                    starvingJobs.add(jobPlan);
                }
            }
        }

        ArrayList<JobPlan> jobsThatRequireAgent = new ArrayList<>();
        jobsThatRequireAgent.addAll(Sets.difference(new HashSet<>(newPlan), new HashSet<>(old)));
        jobsThatRequireAgent.addAll(starvingJobs);

        ArrayList<JobPlan> plansThatRequireElasticAgent = ListUtil.filterInto(new ArrayList<>(), jobsThatRequireAgent, isElasticAgent());
//      messageTimeToLive is lesser than the starvation threshold to ensure there are no duplicate create agent message
        long messageTimeToLive = serverConfigService.elasticJobStarvationThreshold() - 10000;

        for (JobPlan plan : plansThatRequireElasticAgent) {
            map.put(plan.getJobId(), timeProvider.currentTimeMillis());
            if (elasticAgentPluginRegistry.has(plan.getElasticProfile().getPluginId())) {
                String environment = environmentConfigService.envForPipeline(plan.getPipelineName());
                createAgentQueue.post(new CreateAgentMessage(serverConfigService.getAutoregisterKey(), environment, plan.getElasticProfile()), messageTimeToLive);
                serverHealthService.removeByScope(HealthStateScope.forJob(plan.getIdentifier().getPipelineName(), plan.getIdentifier().getStageName(), plan.getIdentifier().getBuildName()));
            } else {
                String jobConfigIdentifier = plan.getIdentifier().jobConfigIdentifier().toString();
                String description = String.format("Plugin [%s] associated with %s is missing. Either the plugin is not " +
                        "installed or could not be registered. Please check plugins tab " +
                        "and server logs for more details.", plan.getElasticProfile().getPluginId(), jobConfigIdentifier);
                serverHealthService.update(ServerHealthState.error(String.format("Unable to find agent for %s",
                        jobConfigIdentifier), description, HealthStateType.general(HealthStateScope.forJob(plan.getIdentifier().getPipelineName(), plan.getIdentifier().getStageName(), plan.getIdentifier().getBuildName()))));
                LOGGER.error(description);
            }
        }
    }

    private Filter<JobPlan> isElasticAgent() {
        return new Filter<JobPlan>() {
            @Override
            public boolean matches(JobPlan input) {
                return input.requiresElasticAgent();
            }
        };
    }

    public boolean shouldAssignWork(ElasticAgentMetadata metadata, String environment, ElasticProfile elasticProfile) {
        return elasticProfile.getPluginId().equals(metadata.elasticPluginId()) && elasticAgentPluginRegistry.shouldAssignWork(pluginManager.getPluginDescriptorFor(metadata.elasticPluginId()), toAgentMetadata(metadata), environment, elasticProfile.getConfigurationAsMap(true));
    }

    @Override
    public void jobStatusChanged(JobInstance job) {
        if (job.isAssignedToAgent()) {
            map.remove(job.getId());
        }
    }
}
