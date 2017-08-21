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

package com.thoughtworks.go.plugin.access.elastic.v2;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.access.common.handler.JSONResultMessageHandler;
import com.thoughtworks.go.plugin.access.common.models.ImageDeserializer;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMessageConverter;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ElasticAgentExtensionConverterV2 implements ElasticAgentMessageConverter {
    private static final Gson GSON = new Gson();

    public static final String VERSION = "2.0";

    @Override
    public String createAgentRequestBody(String autoRegisterKey, String environment, Map<String, String> configuration) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("auto_register_key", autoRegisterKey);
        JsonObject properties = new JsonObject();
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            properties.addProperty(entry.getKey(), entry.getValue());
        }
        jsonObject.add("properties", properties);
        jsonObject.addProperty("environment", environment);
        return GSON.toJson(jsonObject);
    }

    @Override
    public String shouldAssignWorkRequestBody(AgentMetadata elasticAgent, String environment, Map<String, String> configuration) {
        JsonObject jsonObject = new JsonObject();
        JsonObject properties = new JsonObject();
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            properties.addProperty(entry.getKey(), entry.getValue());
        }
        jsonObject.add("properties", properties);
        jsonObject.addProperty("environment", environment);
        jsonObject.add("agent", elasticAgent.toJSON());
        return GSON.toJson(jsonObject);
    }

    @Override
    public String listAgentsResponseBody(Collection<AgentMetadata> metadata) {
        Gson gson = new Gson();
        JsonArray array = new JsonArray();
        for (AgentMetadata agentMetadata : metadata) {
            array.add(agentMetadata.toJSON());
        }
        return gson.toJson(array);
    }

    @Override
    public Collection<AgentMetadata> deleteAndDisableAgentRequestBody(String requestBody) {
        Type AGENT_METADATA_LIST_TYPE = new TypeToken<ArrayList<AgentMetadata>>() {
        }.getType();
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        return gson.fromJson(requestBody, AGENT_METADATA_LIST_TYPE);
    }

    @Override
    public List<PluginConfiguration> getProfileMetadataResponseFromBody(String responseBody) {
        return PluginProfileMetadataKeys.fromJSON(responseBody).toPluginConfigurations();
    }

    @Override
    public String getProfileViewResponseFromBody(String responseBody) {
        String template = (String) new Gson().fromJson(responseBody, Map.class).get("template");
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
    public ValidationResult getValidationResultResponseFromBody(String responseBody) {
        return new JSONResultMessageHandler().toValidationResult(responseBody);
    }

    @Override
    public String validateRequestBody(Map<String, String> configuration) {
        JsonObject properties = new JsonObject();
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            properties.addProperty(entry.getKey(), entry.getValue());
        }
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

    public String getStatusReportView(String responseBody) {
        String statusReportView = (String) new Gson().fromJson(responseBody, Map.class).get("view");
        if (StringUtils.isBlank(statusReportView)) {
            throw new RuntimeException("Status Report is blank!");
        }
        return statusReportView;
    }

    public Capabilities getCapabilitiesFromResponseBody(String responseBody) {
        return com.thoughtworks.go.plugin.access.elastic.models.Capabilities.fromJSON(responseBody).toCapabilites();
    }
}

