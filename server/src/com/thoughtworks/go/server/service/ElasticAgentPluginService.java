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

import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.plugin.access.elastic.AgentMetadata;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.messaging.elasticagents.CreateAgentMessage;
import com.thoughtworks.go.server.messaging.elasticagents.CreateAgentQueueHandler;
import com.thoughtworks.go.server.messaging.elasticagents.ServerPingMessage;
import com.thoughtworks.go.server.messaging.elasticagents.ServerPingQueue;
import com.thoughtworks.go.util.ListUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class ElasticAgentPluginService implements JobStatusListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticAgentPluginService.class);

    private final PluginManager pluginManager;
    private final ElasticAgentPluginRegistry elasticAgentPluginRegistry;
    private final AgentService agentService;
    private final EnvironmentConfigService environmentConfigService;
    private final CreateAgentQueueHandler createAgentQueue;
    private final ServerPingQueue serverPingQueue;
    private final ServerConfigService serverConfigService;

    @Autowired
    public ElasticAgentPluginService(
            PluginManager pluginManager, ElasticAgentPluginRegistry elasticAgentPluginRegistry,
            AgentService agentService, EnvironmentConfigService environmentConfigService,
            CreateAgentQueueHandler createAgentQueue,
            ServerPingQueue serverPingQueue,
            ServerConfigService serverConfigService) {
        this.pluginManager = pluginManager;
        this.elasticAgentPluginRegistry = elasticAgentPluginRegistry;
        this.agentService = agentService;
        this.environmentConfigService = environmentConfigService;
        this.createAgentQueue = createAgentQueue;
        this.serverPingQueue = serverPingQueue;
        this.serverConfigService = serverConfigService;
    }

    public void heartbeat() {
        LinkedMultiValueMap<String, ElasticAgentMetadata> elasticAgents = agentService.allElasticAgents();
        Collection<AgentMetadata> agents = Collections.emptyList();
        for (PluginDescriptor descriptor : elasticAgentPluginRegistry.getPlugins()) {
            if (elasticAgents.containsKey(descriptor.id())) {
                List<ElasticAgentMetadata> elasticAgentMetadatas = elasticAgents.remove(descriptor.id());
                agents = ListUtil.map(elasticAgentMetadatas, new ListUtil.Transformer<ElasticAgentMetadata, AgentMetadata>() {
                    @Override
                    public AgentMetadata transform(ElasticAgentMetadata input) {
                        return toAgentMetadata(input);
                    }
                });
            }
            serverPingQueue.post(new ServerPingMessage(descriptor.id(), agents));
        }

        if (!elasticAgents.isEmpty()) {
            for (String pluginId : elasticAgents.keySet()) {

                Collection<String> uuids = ListUtil.map(elasticAgents.get(pluginId), new ListUtil.Transformer<ElasticAgentMetadata, String>() {
                    @Override
                    public String transform(ElasticAgentMetadata input) {
                        return input.uuid();
                    }
                });
                LOGGER.warn("Elastic agent plugin with identifier {} has gone missing, but left behind {} agent(s) with UUIDs {}.", pluginId, elasticAgents.get(pluginId).size(), uuids);
            }
        }
    }

    public static AgentMetadata toAgentMetadata(ElasticAgentMetadata obj) {
        return new AgentMetadata(obj.elasticAgentId(), obj.agentState().toString(), obj.buildState().toString(), obj.configStatus().toString());
    }

    public void createAgentsFor(Collection<JobPlan> plans) {
        for (JobPlan plan : plans) {
            String environment = environmentConfigService.envForPipeline(plan.getPipelineName());
            if (plan.requiresElasticAgent()){
                createAgentQueue.post(plan.getJobAgentConfig().getPluginId(), new CreateAgentMessage(serverConfigService.getAutoregisterKey(), null, environment, plan.getJobAgentConfig()));
            }
        }
    }

    public boolean shouldAssignWork(ElasticAgentMetadata metadata, List<String> resources, String environment) {
        return elasticAgentPluginRegistry.shouldAssignWork(pluginManager.getPluginDescriptorFor(metadata.elasticPluginId()), toAgentMetadata(metadata), resources, environment);
    }

    public void notifyAgentBusy(ElasticAgentMetadata metadata) {
        elasticAgentPluginRegistry.notifyAgentBusy(pluginManager.getPluginDescriptorFor(metadata.elasticPluginId()), toAgentMetadata(metadata));
    }

    @Override
    public void jobStatusChanged(JobInstance job) {
        if (job.getAgentUuid() == null) {
            return;
        }
        AgentInstance agent = agentService.findAgent(job.getAgentUuid());
        if (job.isCompleted() && agent.isElastic()) {
            ElasticAgentMetadata metadata = agent.elasticAgentMetadata();
            elasticAgentPluginRegistry.notifyAgentIdle(pluginManager.getPluginDescriptorFor(metadata.elasticPluginId()), toAgentMetadata(metadata));
        }
    }
}
