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
package com.thoughtworks.go.plugin.access.elastic.v5;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.thoughtworks.go.domain.ClusterProfilesChangedStatus;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.common.handler.JSONResultMessageHandler;
import com.thoughtworks.go.plugin.access.common.models.ImageDeserializer;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.access.elastic.models.ElasticAgentInformation;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

class ElasticAgentExtensionConverterV5 {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    private ElasticAgentInformationConverterV5 elasticAgentInformationConverterV5 = new ElasticAgentInformationConverterV5();
    private CapabilitiesConverterV5 capabilitiesConverterV5 = new CapabilitiesConverterV5();
    private AgentMetadataConverterV5 agentMetadataConverterV5 = new AgentMetadataConverterV5();

    String createAgentRequestBody(String autoRegisterKey, String environment, Map<String, String> configuration, Map<String, String> clusterProfileProperties, JobIdentifier jobIdentifier) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("auto_register_key", autoRegisterKey);
        jsonObject.add("elastic_agent_profile_properties", mapToJsonObject(configuration));
        jsonObject.add("cluster_profile_properties", mapToJsonObject(clusterProfileProperties));
        jsonObject.addProperty("environment", environment);
        jsonObject.add("job_identifier", jobIdentifierJson(jobIdentifier));

        return GSON.toJson(jsonObject);
    }

    String shouldAssignWorkRequestBody(AgentMetadata elasticAgent, String environment, Map<String, String> configuration, Map<String, String> clusterProfileProperties, JobIdentifier identifier) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("environment", environment);
        jsonObject.add("elastic_agent_profile_properties", mapToJsonObject(configuration));
        jsonObject.add("cluster_profile_properties", mapToJsonObject(clusterProfileProperties));
        jsonObject.add("agent", agentMetadataConverterV5.toDTO(elasticAgent).toJSON());
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

    String getAgentStatusReportRequestBody(JobIdentifier identifier, String elasticAgentId, Map<String, String> clusterProfile) {
        JsonObject jsonObject = new JsonObject();
        if (identifier != null) {
            jsonObject.add("job_identifier", jobIdentifierJson(identifier));
        }
        jsonObject.add("cluster_profile_properties", mapToJsonObject(clusterProfile));
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
        return capabilitiesConverterV5.fromDTO(capabilitiesDTO);
    }

    public ElasticAgentInformation getElasticAgentInformationFromResponseBody(String responseBody) {
        final ElasticAgentInformationDTO elasticAgentInformationDTO = GSON.fromJson(responseBody, ElasticAgentInformationDTO.class);
        return elasticAgentInformationConverterV5.fromDTO(elasticAgentInformationDTO);
    }

    private JsonObject mapToJsonObject(Map<String, String> configuration) {
        final JsonObject properties = new JsonObject();
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            properties.addProperty(entry.getKey(), entry.getValue());
        }
        return properties;
    }

    private JsonArray mapToJsonArray(List<Map<String, String>> configurations) {
        JsonArray jsonArray = new JsonArray();

        for (Map<String, String> configuration : configurations) {
            jsonArray.add(mapToJsonObject(configuration));
        }

        return jsonArray;
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

    public String getJobCompletionRequestBody(String elasticAgentId, JobIdentifier jobIdentifier, Map<String, String> elasticProfileConfiguration, Map<String, String> clusterProfileConfiguration) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("elastic_agent_id", elasticAgentId);
        jsonObject.add("job_identifier", jobIdentifierJson(jobIdentifier));
        jsonObject.add("elastic_agent_profile_properties", mapToJsonObject(elasticProfileConfiguration));
        jsonObject.add("cluster_profile_properties", mapToJsonObject(clusterProfileConfiguration));

        return GSON.toJson(jsonObject);
    }

    public String serverPingRequestBody(List<Map<String, String>> clusterProfileConfigurations) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("all_cluster_profile_properties", mapToJsonArray(clusterProfileConfigurations));
        return GSON.toJson(jsonObject);
    }

    public ElasticAgentInformationDTO getElasticAgentInformationDTO(ElasticAgentInformation elasticAgentInformation) {
        return elasticAgentInformationConverterV5.toDTO(elasticAgentInformation);
    }

    public String getClusterStatusReportRequestBody(Map<String, String> clusterProfile) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("cluster_profile_properties", mapToJsonObject(clusterProfile));
        return GSON.toJson(jsonObject);
    }

    public String getPluginStatusReportRequestBody(List<Map<String, String>> clusterProfiles) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("all_cluster_profiles_properties", mapToJsonArray(clusterProfiles));
        return GSON.toJson(jsonObject);
    }

    public String getClusterProfileChangedRequestBody(ClusterProfilesChangedStatus status, Map<String, String> oldClusterProfile, Map<String, String> newClusterProfile) {
        switch (status) {
            case CREATED:
                return getClusterCreatedRequestBody(newClusterProfile);
            case UPDATED:
                return getClusterUpdatedRequestBody(oldClusterProfile, newClusterProfile);
            case DELETED:
                return getClusterDeletedRequestBody(oldClusterProfile);
            default:
                throw new RuntimeException("Invalid status specified for cluster profiles changed. Can not construct request body.");
        }
    }

    private String getClusterCreatedRequestBody(Map<String, String> clusterProfile) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("status", ClusterProfilesChangedStatus.CREATED.getStatus());
        jsonObject.add("cluster_profiles_properties", mapToJsonObject(clusterProfile));

        return GSON.toJson(jsonObject);
    }

    private String getClusterDeletedRequestBody(Map<String, String> clusterProfile) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("status", ClusterProfilesChangedStatus.DELETED.getStatus());
        jsonObject.add("cluster_profiles_properties", mapToJsonObject(clusterProfile));

        return GSON.toJson(jsonObject);
    }

    private String getClusterUpdatedRequestBody(Map<String, String> oldClusterProfile, Map<String, String> newClusterProfile) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("status", ClusterProfilesChangedStatus.UPDATED.getStatus());
        jsonObject.add("old_cluster_profiles_properties", mapToJsonObject(oldClusterProfile));
        jsonObject.add("cluster_profiles_properties", mapToJsonObject(newClusterProfile));

        return GSON.toJson(jsonObject);
    }
}

