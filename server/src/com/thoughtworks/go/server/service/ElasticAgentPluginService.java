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

import com.thoughtworks.go.plugin.access.elastic.AgentMetadata;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.util.ListUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import java.util.ArrayList;
import java.util.List;

@Service
public class ElasticAgentPluginService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticAgentPluginService.class);

    private final PluginService pluginService;
    private final ElasticAgentPluginRegistry elasticAgentPluginRegistry;
    private final ElasticAgentExtension elasticAgentExtension;
    private final AgentService agentService;

    @Autowired
    public ElasticAgentPluginService(PluginService pluginService, ElasticAgentPluginRegistry elasticAgentPluginRegistry, ElasticAgentExtension elasticAgentExtension, AgentService agentService) {
        this.pluginService = pluginService;
        this.elasticAgentPluginRegistry = elasticAgentPluginRegistry;
        this.elasticAgentExtension = elasticAgentExtension;
        this.agentService = agentService;
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

            ArrayList<AgentMetadata> metadatas = ListUtil.map(elasticAgentMetadatas, new ListUtil.Transformer<ElasticAgentMetadata, AgentMetadata>() {
                @Override
                public AgentMetadata transform(ElasticAgentMetadata obj) {
                    return new AgentMetadata(obj.elasticAgentId(), obj.agentState().toString(), obj.buildState().toString(), obj.configStatus().toString());
                }
            });
            elasticAgentPluginRegistry.serverPing(descriptor.id(), metadatas);
        }

        if (!elasticAgents.isEmpty()) {
            for (String pluginId : elasticAgents.keySet()) {

                List<String> uuids = ListUtil.map(elasticAgents.get(pluginId), new ListUtil.Transformer<ElasticAgentMetadata, String>() {
                    @Override
                    public String transform(ElasticAgentMetadata obj) {
                        return obj.uuid();
                    }
                });

                LOGGER.warn("Elastic agent plugin with identifier {} has gone missing, but left behind {} agent(s) with UUIDs {}.", pluginId, elasticAgents.get(pluginId).size(), uuids);
            }
        }

    }

}
