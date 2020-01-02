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
package com.thoughtworks.go.server.service.plugins.processor.elasticagent.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.server.service.plugins.processor.elasticagent.ElasticAgentProcessorConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ElasticAgentProcessorConverterV1 implements ElasticAgentProcessorConverter {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    private static final AgentMetadataConverterV1 agentMetadataConverter = new AgentMetadataConverterV1();

    @Override
    public String listAgentsResponseBody(Collection<AgentMetadata> metadata) {
        final JsonArray array = new JsonArray();
        for (AgentMetadata agentMetadata : metadata) {
            array.add(agentMetadataConverter.toDTO(agentMetadata).toJSON());
        }
        return GSON.toJson(array);
    }

    @Override
    public Collection<AgentMetadata> deleteAndDisableAgentRequestBody(String requestBody) {
        final Collection<AgentMetadataDTO> agentMetadata = AgentMetadataDTO.fromJSONArray(requestBody);
        final List<AgentMetadata> agentMetadataList = new ArrayList<>();

        if (agentMetadata == null) {
            return agentMetadataList;
        }

        for (AgentMetadataDTO metadataDTO : agentMetadata) {
            agentMetadataList.add(agentMetadataConverter.fromDTO(metadataDTO));
        }

        return agentMetadataList;
    }
}
