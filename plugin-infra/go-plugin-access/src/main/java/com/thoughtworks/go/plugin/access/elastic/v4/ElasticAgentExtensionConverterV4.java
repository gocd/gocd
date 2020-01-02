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
package com.thoughtworks.go.plugin.access.elastic.v4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.common.handler.JSONResultMessageHandler;
import com.thoughtworks.go.plugin.access.common.models.ImageDeserializer;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

class ElasticAgentExtensionConverterV4 {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    private CapabilitiesConverterV4 capabilitiesConverterV4 = new CapabilitiesConverterV4();
    private AgentMetadataConverterV4 agentMetadataConverterV4 = new AgentMetadataConverterV4();

    String createAgentRequestBody(String autoRegisterKey, String environment, Map<String, String> configuration, JobIdentifier jobIdentifier) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("auto_register_key", autoRegisterKey);
        jsonObject.add("properties", mapToJsonObject(configuration));
        jsonObject.addProperty("environment", environment);
        jsonObject.add("job_identifier", jobIdentifierJson(jobIdentifier));

        return GSON.toJson(jsonObject);
    }

    String shouldAssignWorkRequestBody(AgentMetadata elasticAgent, String environment, Map<String, String> configuration, JobIdentifier identifier) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("properties", mapToJsonObject(configuration));
        jsonObject.addProperty("environment", environment);
        jsonObject.add("agent", agentMetadataConverterV4.toDTO(elasticAgent).toJSON());
        jsonObject.add("job_identifier", jobIdentifierJson(identifier));
        return GSON.toJson(jsonObject);
    }


    List<PluginConfiguration> getElasticProfileMetadataResponseFromBody(String responseBody) {
        return PluginProfileMetadataKeys.fromJSON(responseBody).toPluginConfigurations();
    }


    String getProfileViewResponseFromBody(String responseBody) {
        String template = (String) new Gson().fromJson(responseBody, Map.class).get("template");
        if (StringUtils.isBlank(template)) {
            throw new RuntimeException("Template was blank!");
        }
        return template;
    }


    com.thoughtworks.go.plugin.domain.common.Image getImageResponseFromBody(String responseBody) {
        return new ImageDeserializer().fromJSON(responseBody);
    }

    String getAgentStatusReportRequestBody(JobIdentifier identifier, String elasticAgentId) {
        JsonObject jsonObject = new JsonObject();
        if (identifier != null) {
            jsonObject.add("job_identifier", jobIdentifierJson(identifier));
        }
        jsonObject.addProperty("elastic_agent_id", elasticAgentId);
        return GSON.toJson(jsonObject);
    }


    ValidationResult getElasticProfileValidationResultResponseFromBody(String responseBody) {
        return new JSONResultMessageHandler().toValidationResult(responseBody);
    }


    String validateElasticProfileRequestBody(Map<String, String> configuration) {
        JsonObject properties = mapToJsonObject(configuration);
        return new GsonBuilder().serializeNulls().create().toJson(properties);
    }


    Boolean shouldAssignWorkResponseFromBody(String responseBody) {
        return new Gson().fromJson(responseBody, Boolean.class);
    }

    String getStatusReportView(String responseBody) {
        String statusReportView = (String) new Gson().fromJson(responseBody, Map.class).get("view");
        if (StringUtils.isBlank(statusReportView)) {
            throw new RuntimeException("Status Report is blank!");
        }
        return statusReportView;
    }

    Capabilities getCapabilitiesFromResponseBody(String responseBody) {
        final CapabilitiesDTO capabilitiesDTO = GSON.fromJson(responseBody, CapabilitiesDTO.class);
        return capabilitiesConverterV4.fromDTO(capabilitiesDTO);
    }

    private JsonObject mapToJsonObject(Map<String, String> configuration) {
        final JsonObject properties = new JsonObject();
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            properties.addProperty(entry.getKey(), entry.getValue());
        }
        return properties;
    }

    private JsonObject jobIdentifierJson(JobIdentifier jobIdentifier) {
        JsonObject jobIdentifierJson = new JsonObject();
        jobIdentifierJson.addProperty("pipeline_name", jobIdentifier.getPipelineName());
        jobIdentifierJson.addProperty("pipeline_label", jobIdentifier.getPipelineLabel());
        jobIdentifierJson.addProperty("pipeline_counter", jobIdentifier.getPipelineCounter());
        jobIdentifierJson.addProperty("stage_name", jobIdentifier.getStageName());
        jobIdentifierJson.addProperty("stage_counter", jobIdentifier.getStageCounter());
        jobIdentifierJson.addProperty("job_name", jobIdentifier.getBuildName());
        jobIdentifierJson.addProperty("job_id", jobIdentifier.getBuildId());
        return jobIdentifierJson;
    }

    public String getJobCompletionRequestBody(String elasticAgentId, JobIdentifier jobIdentifier) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("elastic_agent_id", elasticAgentId);
        jsonObject.add("job_identifier", jobIdentifierJson(jobIdentifier));
        return GSON.toJson(jsonObject);
    }
}

