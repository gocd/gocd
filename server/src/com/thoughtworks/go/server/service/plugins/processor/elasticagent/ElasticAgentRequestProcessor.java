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

package com.thoughtworks.go.server.service.plugins.processor.elasticagent;

import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.plugin.access.elastic.AgentMetadata;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.AgentConfigService;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.util.ListUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.thoughtworks.go.plugin.access.elastic.Constants.*;
import static com.thoughtworks.go.server.service.ElasticAgentPluginService.toAgentMetadata;
import static java.lang.String.format;

@Component
public class ElasticAgentRequestProcessor implements GoPluginApiRequestProcessor {

    private static Logger LOGGER = LoggerFactory.getLogger(ElasticAgentRequestProcessor.class);

    private final AgentService agentService;
    private final AgentConfigService agentConfigService;
    private final ElasticAgentExtension elasticAgentExtension;

    @Autowired
    public ElasticAgentRequestProcessor(PluginRequestProcessorRegistry registry, AgentService agentService, AgentConfigService agentConfigService, ElasticAgentExtension elasticAgentExtension) {
        this.elasticAgentExtension = elasticAgentExtension;
        this.agentConfigService = agentConfigService;
        this.agentService = agentService;

        registry.registerProcessorFor(PROCESS_DISABLE_AGENTS, this);
        registry.registerProcessorFor(PROCESS_DELETE_AGENTS, this);
        registry.registerProcessorFor(REQUEST_SERVER_LIST_AGENTS, this);
    }

    @Override
    public GoApiResponse process(final GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        switch (goPluginApiRequest.api()) {
            case PROCESS_DISABLE_AGENTS:
                Collection<AgentMetadata> agentsToDisable = elasticAgentExtension.getElasticAgentMessageConverter(goPluginApiRequest.apiVersion()).deleteAndDisableAgentRequestBody(goPluginApiRequest.requestBody());
                if (agentsToDisable.isEmpty()) {
                    return new DefaultGoApiResponse(200);
                }
                return processDisableAgent(pluginDescriptor, goPluginApiRequest);
            case PROCESS_DELETE_AGENTS:
                Collection<AgentMetadata> agentsToDelete = elasticAgentExtension.getElasticAgentMessageConverter(goPluginApiRequest.apiVersion()).deleteAndDisableAgentRequestBody(goPluginApiRequest.requestBody());
                if (agentsToDelete.isEmpty()) {
                    return new DefaultGoApiResponse(200);
                }
                return processDeleteAgent(pluginDescriptor, goPluginApiRequest);
            case REQUEST_SERVER_LIST_AGENTS:
                return processListAgents(pluginDescriptor, goPluginApiRequest);
            default:
                return DefaultGoApiResponse.error("Illegal api request");
        }
    }

    private GoApiResponse processListAgents(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        LOGGER.debug("Listing agents for plugin {}", pluginDescriptor.id());
        List<ElasticAgentMetadata> elasticAgents = agentService.allElasticAgents().get(pluginDescriptor.id());

        Collection<AgentMetadata> metadata;
        if (elasticAgents == null) {
            metadata = new ArrayList<>();
        } else {
            metadata = ListUtil.map(elasticAgents, new ListUtil.Transformer<ElasticAgentMetadata, AgentMetadata>() {
                @Override
                public AgentMetadata transform(ElasticAgentMetadata obj) {
                    return toAgentMetadata(obj);
                }
            });
        }

        String responseBody = elasticAgentExtension.getElasticAgentMessageConverter(goPluginApiRequest.apiVersion()).listAgentsResponseBody(metadata);
        return DefaultGoApiResponse.success(responseBody);
    }

    private GoApiResponse processDeleteAgent(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        Collection<AgentInstance> agentInstances = getAgentInstances(pluginDescriptor, goPluginApiRequest);

        if (agentInstances.isEmpty()) {
            return new DefaultGoApiResponse(200);
        }

        LOGGER.debug("Deleting agents from plugin {} {}", pluginDescriptor.id(), agentInstances);
        agentConfigService.deleteAgents(usernameFor(pluginDescriptor), agentInstances.toArray(new AgentInstance[agentInstances.size()]));
        LOGGER.debug("Done deleting agents from plugin {} {}", pluginDescriptor.id(), agentInstances);
        return new DefaultGoApiResponse(200);
    }

    private GoApiResponse processDisableAgent(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        Collection<AgentInstance> agentInstances = getAgentInstances(pluginDescriptor, goPluginApiRequest);

        if (agentInstances.isEmpty()) {
            return new DefaultGoApiResponse(200);
        }

        LOGGER.debug("Disabling agents from plugin {} {}", pluginDescriptor.id(), agentInstances);
        agentConfigService.disableAgents(usernameFor(pluginDescriptor), agentInstances.toArray(new AgentInstance[agentInstances.size()]));
        LOGGER.debug("Done disabling agents from plugin {} {}", pluginDescriptor.id(), agentInstances);
        return new DefaultGoApiResponse(200);
    }

    Username usernameFor(GoPluginDescriptor pluginDescriptor) {
        return new Username(format("plugin-%s", pluginDescriptor.id()));
    }

    private Collection<AgentInstance> getAgentInstances(final GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        Collection<AgentMetadata> agents = elasticAgentExtension.getElasticAgentMessageConverter(goPluginApiRequest.apiVersion()).deleteAndDisableAgentRequestBody(goPluginApiRequest.requestBody());

        return ListUtil.map(agents, new ListUtil.Transformer<AgentMetadata, AgentInstance>() {
            @Override
            public AgentInstance transform(AgentMetadata input) {
                return agentService.findElasticAgent(input.elasticAgentId(), pluginDescriptor.id());
            }
        });
    }

}
