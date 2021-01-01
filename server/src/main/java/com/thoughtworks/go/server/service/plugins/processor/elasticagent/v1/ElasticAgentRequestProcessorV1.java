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
package com.thoughtworks.go.server.service.plugins.processor.elasticagent.v1;

import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.plugins.processor.elasticagent.AbstractVersionableElasticAgentProcessor;
import com.thoughtworks.go.server.service.plugins.processor.elasticagent.ElasticAgentProcessorConverter;

import java.util.Collection;

import static com.thoughtworks.go.server.service.plugins.processor.elasticagent.v1.ElasticAgentProcessorRequestsV1.*;

public class ElasticAgentRequestProcessorV1 extends AbstractVersionableElasticAgentProcessor {
    public static final String VERSION = "1.0";
    private ElasticAgentProcessorConverter elasticAgentProcessorConverterV1;

    public ElasticAgentRequestProcessorV1(AgentService agentService) {
        this(agentService, new ElasticAgentProcessorConverterV1());
    }

    ElasticAgentRequestProcessorV1(AgentService agentService, ElasticAgentProcessorConverterV1 converterV1) {
        super(agentService);
        elasticAgentProcessorConverterV1 = converterV1;
    }

    @Override
    public GoApiResponse process(final GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        switch (goPluginApiRequest.api()) {
            case REQUEST_DISABLE_AGENTS:
                return processDisableAgent(pluginDescriptor, goPluginApiRequest);
            case REQUEST_DELETE_AGENTS:
                return processDeleteAgent(pluginDescriptor, goPluginApiRequest);
            case REQUEST_SERVER_LIST_AGENTS:
                return processListAgents(pluginDescriptor, goPluginApiRequest);
            default:
                return DefaultGoApiResponse.error("Illegal api request");
        }
    }

    @Override
    public GoApiResponse processListAgents(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        final Collection<AgentMetadata> metadata = getAgentMetadataForPlugin(pluginDescriptor.id());
        String responseBody = elasticAgentProcessorConverterV1.listAgentsResponseBody(metadata);
        return DefaultGoApiResponse.success(responseBody);
    }

    @Override
    public GoApiResponse processDeleteAgent(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        final Collection<AgentMetadata> agentsToDelete = elasticAgentProcessorConverterV1.deleteAndDisableAgentRequestBody(goPluginApiRequest.requestBody());
        deleteAgents(pluginDescriptor.id(), agentsToDelete);
        return new DefaultGoApiResponse(200);
    }

    @Override
    public GoApiResponse processDisableAgent(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        Collection<AgentMetadata> agentsToDisable = elasticAgentProcessorConverterV1.deleteAndDisableAgentRequestBody(goPluginApiRequest.requestBody());
        final String pluginId = pluginDescriptor.id();
        disableAgents(pluginId, agentsToDisable);
        return new DefaultGoApiResponse(200);
    }

    @Override
    public String version() {
        return VERSION;
    }
}
