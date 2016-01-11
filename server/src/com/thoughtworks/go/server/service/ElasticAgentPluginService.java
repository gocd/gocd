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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.thoughtworks.go.config.Resources;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.plugin.access.elastic.AgentMetadata;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.domain.JobStatusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class ElasticAgentPluginService implements JobStatusListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticAgentPluginService.class);

    private final PluginManager pluginManager;
    private final ElasticAgentPluginRegistry elasticAgentPluginRegistry;
    private final ElasticAgentExtension elasticAgentExtension;
    private final AgentService agentService;
    private final EnvironmentConfigService environmentConfigService;

    @Autowired
    public ElasticAgentPluginService(
            PluginManager pluginManager, ElasticAgentPluginRegistry elasticAgentPluginRegistry,
            ElasticAgentExtension elasticAgentExtension, AgentService agentService,
            EnvironmentConfigService environmentConfigService) {
        this.pluginManager = pluginManager;
        this.elasticAgentPluginRegistry = elasticAgentPluginRegistry;
        this.elasticAgentExtension = elasticAgentExtension;
        this.agentService = agentService;
        this.environmentConfigService = environmentConfigService;
    }

    public void heartbeat() {
        LinkedMultiValueMap<String, ElasticAgentMetadata> elasticAgents = agentService.allElasticAgents();

        for (PluginDescriptor descriptor : elasticAgentPluginRegistry.getPlugins()) {
            List<ElasticAgentMetadata> elasticAgentMetadatas;
            if (elasticAgents.containsKey(descriptor.id())) {
                elasticAgentMetadatas = elasticAgents.remove(descriptor.id());
            } else {
                elasticAgentMetadatas = new ArrayList<>();
            }
            Collection<AgentMetadata> metadatas = Collections2.transform(elasticAgentMetadatas, new Function<ElasticAgentMetadata, AgentMetadata>() {
                @Override
                public AgentMetadata apply(ElasticAgentMetadata input) {
                    return toAgentMetadata(input);
                }
            });

            elasticAgentPluginRegistry.serverPing(descriptor.id(), metadatas);
        }

        if (!elasticAgents.isEmpty()) {
            for (String pluginId : elasticAgents.keySet()) {

                Collection<String> uuids = Collections2.transform(elasticAgents.get(pluginId), new Function<ElasticAgentMetadata, String>() {
                    @Override
                    public String apply(ElasticAgentMetadata input) {
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

    //    TODO: ketanpkr - async this?
    public void createAgentsFor(Collection<JobPlan> plans) {
        for (JobPlan plan : plans) {
            List<String> resources = new Resources(plan.getResources()).resourceNames();
            String environment = environmentConfigService.envForPipeline(plan.getPipelineName());
            elasticAgentPluginRegistry.createAgent(resources, environment);
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
