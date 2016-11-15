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

package com.thoughtworks.go.plugin.access.elastic;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.access.common.handler.JSONResultMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.Image;
import com.thoughtworks.go.plugin.api.config.Configuration;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Type;
import java.util.*;

public class ElasticAgentExtensionConverterV1 implements ElasticAgentMessageConverter {

    public static final String VERSION = "1.0";

    @Override
    public String createAgentRequestBody(String autoRegisterKey, String environment, Map<String, String> configuration) {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("auto_register_key", autoRegisterKey);
        JsonObject properties = new JsonObject();
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            properties.addProperty(entry.getKey(), entry.getValue());
        }
        jsonObject.add("properties", properties);
        jsonObject.addProperty("environment", environment);
        return gson.toJson(jsonObject);
    }

    @Override
    public String shouldAssignWorkRequestBody(AgentMetadata elasticAgent, String environment, Map<String, String> configuration) {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        JsonObject properties = new JsonObject();
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            properties.addProperty(entry.getKey(), entry.getValue());
        }
        jsonObject.add("properties", properties);
        jsonObject.addProperty("environment", environment);
        jsonObject.add("agent", elasticAgent.toJSON());
        return gson.toJson(jsonObject);
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
    public Configuration getProfileMetadataResponseFromBody(String responseBody) {
        List<Map<String, Object>> list = new Gson().fromJson(responseBody, List.class);

        Configuration configuration = new Configuration();
        for (Map<String, Object> map : list) {
            String key = (String) map.get("key");
            Property property = new Property(key);

            Map<String, Boolean> metadata = (Map<String, Boolean>) map.get("metadata");
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            if (metadata.containsKey("required") && metadata.get("required")) {
                property.with(Property.REQUIRED, true);
            } else {
                property.with(Property.REQUIRED, false);
            }

            if (metadata.containsKey("secure") && metadata.get("secure")) {
                property.with(Property.SECURE, true);
            } else {
                property.with(Property.SECURE, false);
            }
            configuration.add(property);
        }

        return configuration;
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
    public Image getImageResponseFromBody(String responseBody) {
        Map<String, String> json = new Gson().fromJson(responseBody, Map.class);
        if (json != null && json.containsKey("content-type") && json.containsKey("data")) {
            return new Image(json.get("content-type"), json.get("data"));
        }
        return null;
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

}

