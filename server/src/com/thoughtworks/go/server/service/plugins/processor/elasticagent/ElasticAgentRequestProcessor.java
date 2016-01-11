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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.plugin.access.elastic.AgentMetadata;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.AgentConfigService;
import com.thoughtworks.go.server.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static com.thoughtworks.go.plugin.access.elastic.Constants.PROCESS_DELETE_AGENT;
import static com.thoughtworks.go.plugin.access.elastic.Constants.PROCESS_DISABLE_AGENT;

@Component
public class ElasticAgentRequestProcessor implements GoPluginApiRequestProcessor {
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
        switch (goPluginApiRequest.api()) {
            case PROCESS_DISABLE_AGENT:
                agentConfigService.disableAgents(new Username("plugin." + pluginDescriptor.id()), agentsMatching(pluginDescriptor, agents));
                return new DefaultGoApiResponse(200);
            case PROCESS_DELETE_AGENT:
                agentConfigService.deleteAgents(new Username("plugin." + pluginDescriptor.id()), agentsMatching(pluginDescriptor, agents));
                return new DefaultGoApiResponse(200);
            default:
                return DefaultGoApiResponse.error("Illegal api request");
        }
    }

    private AgentInstance[] agentsMatching(final GoPluginDescriptor pluginDescriptor, Collection<AgentMetadata> agents) {
        Collection<AgentInstance> instances = Collections2.transform(agents, new Function<AgentMetadata, AgentInstance>() {
            @Override
            public AgentInstance apply(AgentMetadata input) {
                return agentService.findElasticAgent(input.elasticAgentId(), pluginDescriptor.id());
            }
        });
        return instances.toArray(new AgentInstance[instances.size()]);
    }
}
