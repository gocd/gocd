/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.plugins.processor.elasticagent.v1.ElasticAgentRequestProcessorV1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.server.service.plugins.processor.elasticagent.v1.ElasticAgentProcessorRequestsV1.*;

@Component
public class ElasticAgentRequestProcessor implements GoPluginApiRequestProcessor {
    private Map<String, VersionableElasticAgentProcessor> versionableProcessorMap = new HashMap<>();

    @Autowired
    public ElasticAgentRequestProcessor(PluginRequestProcessorRegistry registry, AgentService agentService) {
        this(registry, new HashMap<String, VersionableElasticAgentProcessor>() {{
            put("1.0", new ElasticAgentRequestProcessorV1(agentService));
        }});
    }

    ElasticAgentRequestProcessor(PluginRequestProcessorRegistry registry, Map<String, VersionableElasticAgentProcessor> versionableElasticAgentProcessors) {
        if (versionableElasticAgentProcessors != null) {
            versionableProcessorMap.putAll(versionableElasticAgentProcessors);
        }

        registry.registerProcessorFor(REQUEST_DISABLE_AGENTS, this);
        registry.registerProcessorFor(REQUEST_DELETE_AGENTS, this);
        registry.registerProcessorFor(REQUEST_SERVER_LIST_AGENTS, this);
    }

    @Override
    public GoApiResponse process(final GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        return elasticAgentProcessorForVersion(goPluginApiRequest.apiVersion())
                .process(pluginDescriptor, goPluginApiRequest);
    }

    private VersionableElasticAgentProcessor elasticAgentProcessorForVersion(String resolvedApiVersion) {
        return versionableProcessorMap.get(resolvedApiVersion);
    }
}
