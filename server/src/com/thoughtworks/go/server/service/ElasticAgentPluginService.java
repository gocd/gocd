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

import com.google.common.collect.Sets;
import com.thoughtworks.go.config.Resource;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.plugin.access.elastic.AgentMetadata;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.messaging.elasticagents.CreateAgentMessage;
import com.thoughtworks.go.server.messaging.elasticagents.CreateAgentQueueHandler;
import com.thoughtworks.go.server.messaging.elasticagents.ServerPingMessage;
import com.thoughtworks.go.server.messaging.elasticagents.ServerPingQueueHandler;
import com.thoughtworks.go.server.messaging.elasticagents.JobStatusPluginMessage;
import com.thoughtworks.go.server.messaging.elasticagents.JobStatusPluginQueueHandler;
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
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    private final JobStatusPluginQueueHandler jobStatusPluginQueue;
    private final ServerConfigService serverConfigService;
    private final TimeProvider timeProvider;
    private final ServerHealthService serverHealthService;
    private final ConcurrentHashMap<Long, Long> map = new ConcurrentHashMap<>();

    @Autowired
    public ElasticAgentPluginService(
            PluginManager pluginManager, ElasticAgentPluginRegistry elasticAgentPluginRegistry,
            AgentService agentService, EnvironmentConfigService environmentConfigService,
            CreateAgentQueueHandler createAgentQueue,
            ServerPingQueueHandler serverPingQueue,
            JobStatusPluginQueueHandler jobStatusPluginQueue,
            ServerConfigService serverConfigService, TimeProvider timeProvider, ServerHealthService serverHealthService) {
        this.pluginManager = pluginManager;
        this.elasticAgentPluginRegistry = elasticAgentPluginRegistry;
        this.agentService = agentService;
        this.environmentConfigService = environmentConfigService;
        this.createAgentQueue = createAgentQueue;
        this.serverPingQueue = serverPingQueue;
        this.jobStatusPluginQueue = jobStatusPluginQueue;
        this.serverConfigService = serverConfigService;
        this.timeProvider = timeProvider;
        this.serverHealthService = serverHealthService;
    }

    public void heartbeat() {
        LinkedMultiValueMap<String, AgentInstance> elasticAgentsOfMissingPlugins = agentService.allElasticAgents();

        for (PluginDescriptor descriptor : elasticAgentPluginRegistry.getPlugins()) {
            serverPingQueue.post(new ServerPingMessage(descriptor.id()));
            elasticAgentsOfMissingPlugins.remove(descriptor.id());
            serverHealthService.removeByScope(scope(descriptor.id()));
        }

        if (!elasticAgentsOfMissingPlugins.isEmpty()) {
            for (String pluginId : elasticAgentsOfMissingPlugins.keySet()) {
                Collection<String> uuids = ListUtil.map(elasticAgentsOfMissingPlugins.get(pluginId), new ListUtil.Transformer<AgentInstance, String>() {
                    @Override
                    public String transform(AgentInstance input) {
                        return input.getUuid();
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

    public static AgentMetadata toAgentMetadata(AgentInstance eam) {
        return new AgentMetadata(eam.elasticAgentId(), eam.getRuntimeStatus().agentState().toString(), eam.getRuntimeStatus().buildState().toString(), eam.getAgentConfigStatus().toString());
    }

    public static AgentMetadata toAgentMetadata(AgentInstance eam, Collection<String> environments) {
        return new AgentMetadata(eam.elasticAgentId(), eam.getRuntimeStatus().agentState().toString(), eam.getRuntimeStatus().buildState().toString(), eam.getAgentConfigStatus().toString(), eam.getResources().resourceNames(), environments);
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

        ArrayList<JobPlan> plansThatRequireElasticAgent = ListUtil.filterInto(new ArrayList<JobPlan>(), jobsThatRequireAgent, isElasticAgent());

        for (JobPlan plan : plansThatRequireElasticAgent) {
            String environment = environmentConfigService.envForPipeline(plan.getPipelineName());
            map.put(plan.getJobId(), timeProvider.currentTimeMillis());
            createAgentQueue.post(new CreateAgentMessage(serverConfigService.getAutoregisterKey(), environment, plan.getElasticProfile()));
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

    public boolean shouldAssignWork(AgentInstance agent, String environment, ElasticProfile elasticProfile) {
        return elasticAgentPluginRegistry.shouldAssignWork(pluginManager.getPluginDescriptorFor(agent.elasticPluginId()), toAgentMetadata(agent), environment, elasticProfile.getConfigurationAsMap(true));
    }

    @Override
    public void jobStatusChanged(JobInstance job) {
        postToJobStatusPluginQueue(job);
        if (job.isAssignedToAgent()) {
            map.remove(job.getId());
        }
    }

    private void postToJobStatusPluginQueue(JobInstance job) {
        String environment = environmentConfigService.envForPipeline(job.getPipelineName());
        List<String> resources;
        if (!job.isCompleted()) {
            resources = ListUtil.map(job.getPlan().getResources(), new ListUtil.Transformer<Resource, String>() {
                @Override
                public String transform(Resource obj) {
                    return obj.getName();
                }
            });
        } else {
            resources = Collections.emptyList();
        }

        LOGGER.info(String.format("jobStatusChanged(%s) for environment %s with resources %s", job.toString(), environment, ListUtil.join(resources)));
        for (PluginDescriptor descriptor : elasticAgentPluginRegistry.getPlugins()) {
            JobStatusPluginMessage message = new JobStatusPluginMessage(job.getIdentifier(), job.getState(), job.getAgentUuid(), descriptor.id(), environment, resources);
            jobStatusPluginQueue.post(message);
        }
    }
}
