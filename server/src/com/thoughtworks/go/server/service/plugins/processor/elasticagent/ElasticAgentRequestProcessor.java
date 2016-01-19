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

import java.util.Collection;

import static com.thoughtworks.go.plugin.access.elastic.Constants.PROCESS_DELETE_AGENT;
import static com.thoughtworks.go.plugin.access.elastic.Constants.PROCESS_DISABLE_AGENT;

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

        registry.registerProcessorFor(PROCESS_DISABLE_AGENT, this);
        registry.registerProcessorFor(PROCESS_DISABLE_AGENT, this);
    }

    @Override
    public GoApiResponse process(final GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        Collection<AgentMetadata> agents = elasticAgentExtension.getElasticAgentMessageConverter(goPluginApiRequest.apiVersion()).deleteAgentRequestBody(goPluginApiRequest.requestBody());
        if (agents.isEmpty()) {
            return new DefaultGoApiResponse(200);
        }
        Collection<AgentInstance> agentInstances = agentsMatching(pluginDescriptor, agents);
        Collection<ElasticAgentMetadata> metadata = ElasticAgentMetadata.from(agentInstances);

        switch (goPluginApiRequest.api()) {
            case PROCESS_DISABLE_AGENT:
                LOGGER.debug("Disabling agents {}", metadata);
                agentConfigService.disableAgents(new Username("plugin." + pluginDescriptor.id()), agentInstances.toArray(new AgentInstance[agentInstances.size()]));
                LOGGER.debug("Done disabling agents {}", metadata);
                return new DefaultGoApiResponse(200);
            case PROCESS_DELETE_AGENT:
                LOGGER.debug("Deleting agents {}", metadata);
                agentConfigService.deleteAgents(new Username("plugin." + pluginDescriptor.id()), agentInstances.toArray(new AgentInstance[agentInstances.size()]));
                LOGGER.debug("Done deleting agents {}", metadata);
                return new DefaultGoApiResponse(200);
            default:
                return DefaultGoApiResponse.error("Illegal api request");
        }
    }

    private Collection<AgentInstance> agentsMatching(final GoPluginDescriptor pluginDescriptor, Collection<AgentMetadata> agents) {
        return ListUtil.map(agents, new ListUtil.Transformer<AgentMetadata, AgentInstance>() {
            @Override
            public AgentInstance transform(AgentMetadata input) {
                return agentService.findElasticAgent(input.elasticAgentId(), pluginDescriptor.id());
            }
        });
    }

}
