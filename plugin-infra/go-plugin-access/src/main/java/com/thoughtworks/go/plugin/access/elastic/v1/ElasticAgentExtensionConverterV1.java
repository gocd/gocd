/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.elastic.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.common.handler.JSONResultMessageHandler;
import com.thoughtworks.go.plugin.access.common.models.ImageDeserializer;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMessageConverter;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ElasticAgentExtensionConverterV1 implements ElasticAgentMessageConverter {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    public static final String VERSION = "1.0";

    @Override
    public CapabilitiesConverterV1 capabilitiesConverter() {
        return new CapabilitiesConverterV1();
    }

    @Override
    public AgentMetadataConverterV1 agentMetadataConverter() {
        return new AgentMetadataConverterV1();
    }

    @Override
    public String createAgentRequestBody(String autoRegisterKey, String environment, Map<String, String> configuration, JobIdentifier jobIdentifier) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("auto_register_key", autoRegisterKey);
        jsonObject.add("properties", mapToJsonObject(configuration));
        jsonObject.addProperty("environment", environment);
        return GSON.toJson(jsonObject);
    }

    @Override
    public String shouldAssignWorkRequestBody(AgentMetadata elasticAgent, String environment, Map<String, String> configuration, JobIdentifier identifier) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("properties", mapToJsonObject(configuration));
        jsonObject.addProperty("environment", environment);
        jsonObject.add("agent", agentMetadataConverter().toDTO(elasticAgent).toJSON());
        return GSON.toJson(jsonObject);
    }

    @Override
    public String listAgentsResponseBody(Collection<AgentMetadata> metadata) {
        final AgentMetadataConverterV1 agentMetadataConverterV1 = agentMetadataConverter();
        final JsonArray array = new JsonArray();
        for (AgentMetadata agentMetadata : metadata) {
            array.add(agentMetadataConverterV1.toDTO(agentMetadata).toJSON());
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

        final AgentMetadataConverterV1 agentMetadataConverterV1 = agentMetadataConverter();
        for (AgentMetadataDTO metadataDTO : agentMetadata) {
            agentMetadataList.add(agentMetadataConverterV1.fromDTO(metadataDTO));
        }

        return agentMetadataList;
    }

    @Override
    public List<PluginConfiguration> getProfileMetadataResponseFromBody(String responseBody) {
        return PluginProfileMetadataKeys.fromJSON(responseBody).toPluginConfigurations();
    }

    @Override
    public String getProfileViewResponseFromBody(String responseBody) {
        final String template = (String) new Gson().fromJson(responseBody, Map.class).get("template");
        if (StringUtils.isBlank(template)) {
            throw new RuntimeException("Template was blank!");
        }
        return template;
    }

    @Override
    public com.thoughtworks.go.plugin.domain.common.Image getImageResponseFromBody(String responseBody) {
        return new ImageDeserializer().fromJSON(responseBody);
    }

    @Override
    public String getAgentStatusReportRequestBody(JobIdentifier identifier, String elasticAgentId) {
        throw new UnsupportedOperationException("Agent status report is not supported in elastic-agent extension v1.");
    }

    @Override
    public String getStatusReportView(String responseBody) {
        throw new UnsupportedOperationException("Plugin status report is not supported in elastic-agent extension v1.");
    }

    @Override
    public Capabilities getCapabilitiesFromResponseBody(String responseBody) {
        return capabilitiesConverter().fromDTO(new CapabilitiesDTO());
    }

    @Override
    public ValidationResult getValidationResultResponseFromBody(String responseBody) {
        return new JSONResultMessageHandler().toValidationResult(responseBody);
    }

    @Override
    public String validateRequestBody(Map<String, String> configuration) {
        JsonObject properties = mapToJsonObject(configuration);
        return new GsonBuilder().serializeNulls().create().toJson(properties);
    }

    @Override
    public Boolean canHandlePluginResponseFromBody(String responseBody) {
        return new Gson().fromJson(responseBody, Boolean.class);
    }

    @Override
    public Boolean shouldAssignWorkResponseFromBody(String responseBody) {
        return canHandlePluginResponseFromBody(responseBody);
    }

    private JsonObject mapToJsonObject(Map<String, String> configuration) {
        final JsonObject properties = new JsonObject();
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            properties.addProperty(entry.getKey(), entry.getValue());
        }
        return properties;
    }
}

