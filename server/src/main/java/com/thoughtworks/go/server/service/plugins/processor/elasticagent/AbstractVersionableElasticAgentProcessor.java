/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.plugins.processor.elasticagent;

import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.AgentConfigService;
import com.thoughtworks.go.server.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.thoughtworks.go.server.service.ElasticAgentPluginService.toAgentMetadata;
import static java.lang.String.format;

public abstract class AbstractVersionableElasticAgentProcessor implements VersionableElasticAgentProcessor {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    protected final AgentService agentService;
    protected final AgentConfigService agentConfigService;

    public AbstractVersionableElasticAgentProcessor(AgentService agentService, AgentConfigService agentConfigService) {
        this.agentService = agentService;
        this.agentConfigService = agentConfigService;
    }

    protected Collection<AgentMetadata> getAgentMetadataForPlugin(String pluginId) {
        LOGGER.debug("Listing agents for plugin {}", pluginId);
        List<ElasticAgentMetadata> elasticAgents = agentService.allElasticAgents().get(pluginId);

        Collection<AgentMetadata> metadata;
        if (elasticAgents == null) {
            metadata = new ArrayList<>();
        } else {
            metadata = elasticAgents.stream().map(new Function<ElasticAgentMetadata, AgentMetadata>() {
                @Override
                public AgentMetadata apply(ElasticAgentMetadata obj) {
                    return toAgentMetadata(obj);
                }
            }).collect(Collectors.toList());
        }
        return metadata;
    }

    public Username usernameFor(String pluginId) {
        return new Username(format("plugin-%s", pluginId));
    }

    protected Collection<AgentInstance> getAgentInstances(String pluginId, Collection<AgentMetadata> agentMetadataListFromRequest) {
        if (agentMetadataListFromRequest.isEmpty()) {
            return Collections.emptyList();
        }

        return agentMetadataListFromRequest.stream().map(new Function<AgentMetadata, AgentInstance>() {
            @Override
            public AgentInstance apply(AgentMetadata input) {
                return agentService.findElasticAgent(input.elasticAgentId(), pluginId);
            }
        }).collect(Collectors.toList());
    }

    protected void deleteAgents(String pluginId, Collection<AgentMetadata> agentsToDelete) {
        final Collection<AgentInstance> agentInstances = getAgentInstances(pluginId, agentsToDelete);

        if (agentInstances.isEmpty()) {
            return;
        }

        LOGGER.debug("Deleting agents from plugin {} {}", pluginId, agentInstances);
        agentConfigService.deleteAgents(usernameFor(pluginId), agentInstances.toArray(new AgentInstance[agentInstances.size()]));
        LOGGER.debug("Done deleting agents from plugin {} {}", pluginId, agentInstances);
    }

    protected void disableAgents(String pluginId, Collection<AgentMetadata> agentsToDisable) {
        Collection<AgentInstance> agentInstances = getAgentInstances(pluginId, agentsToDisable);

        LOGGER.debug("Disabling agents from plugin {} {}", pluginId, agentInstances);
        agentConfigService.disableAgents(usernameFor(pluginId), agentInstances.toArray(new AgentInstance[agentInstances.size()]));
        LOGGER.debug("Done disabling agents from plugin {} {}", pluginId, agentInstances);
    }
}
