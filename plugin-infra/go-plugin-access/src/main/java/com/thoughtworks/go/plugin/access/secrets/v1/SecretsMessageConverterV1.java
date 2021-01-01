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
package com.thoughtworks.go.plugin.access.secrets.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.access.common.handler.JSONResultMessageHandler;
import com.thoughtworks.go.plugin.access.common.models.ImageDeserializer;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.access.secrets.SecretsMessageConverter;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.secrets.Secret;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class SecretsMessageConverterV1 implements SecretsMessageConverter {
    public static final String VERSION = "1.0";
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @Override
    public Image getImageFromResponseBody(String responseBody) {
        return new ImageDeserializer().fromJSON(responseBody);
    }

    @Override
    public List<PluginConfiguration> getSecretsConfigMetadataFromResponse(String responseBody) {
        return PluginProfileMetadataKeys.fromJSON(responseBody).toPluginConfigurations();
    }

    @Override
    public String getSecretsConfigViewFromResponse(String responseBody) {
        String statusReportView = (String) new Gson().fromJson(responseBody, Map.class).get("template");
        if (isBlank(statusReportView)) {
            throw new RuntimeException("Template is blank!");
        }

        return statusReportView;
    }

    @Override
    public ValidationResult getSecretsConfigValidationResultFromResponse(String responseBody) {
        return new JSONResultMessageHandler().toValidationResult(responseBody);
    }

    @Override
    public String validatePluginConfigurationRequestBody(Map<String, String> configuration) {
        return GSON.toJson(mapToJsonObject(configuration));
    }

    @Override
    public String lookupSecretsRequestBody(Set<String> lookupStrings, Map<String, String> configurationAsMap) {
        final Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("keys", lookupStrings);
        requestBodyMap.put("configuration", mapToJsonObject(configurationAsMap));

        return GSON.toJson(requestBodyMap);
    }

    @Override
    public List<Secret> getSecretsFromResponse(String responseBody) {
        return SecretDTO.fromJSONList(responseBody).stream().map(SecretDTO::toDomainModel).collect(Collectors.toList());
    }

    @Override
    public String getErrorMessageFromResponse(String responseBody) {
        String errorMessage = (String) new Gson().fromJson(responseBody, Map.class).get("message");

        return errorMessage;
    }

    private JsonObject mapToJsonObject(Map<String, String> configuration) {
        final JsonObject properties = new JsonObject();
        configuration.forEach(properties::addProperty);
        return properties;
    }
}