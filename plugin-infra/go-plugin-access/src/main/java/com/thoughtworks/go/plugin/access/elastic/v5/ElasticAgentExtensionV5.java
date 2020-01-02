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

import com.thoughtworks.go.domain.ClusterProfilesChangedStatus;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.elastic.VersionedElasticAgentExtension;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.access.elastic.models.ElasticAgentInformation;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;

import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.access.elastic.v5.ElasticAgentPluginConstantsV5.*;

public class ElasticAgentExtensionV5 implements VersionedElasticAgentExtension {
    public static final String VERSION = "5.0";
    private final PluginRequestHelper pluginRequestHelper;
    private final ElasticAgentExtensionConverterV5 elasticAgentExtensionConverterV5;

    public ElasticAgentExtensionV5(PluginRequestHelper pluginRequestHelper) {
        this.pluginRequestHelper = pluginRequestHelper;
        this.elasticAgentExtensionConverterV5 = new ElasticAgentExtensionConverterV5();
    }

    @Override
    public com.thoughtworks.go.plugin.domain.common.Image getIcon(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PLUGIN_SETTINGS_ICON, new DefaultPluginInteractionCallback<com.thoughtworks.go.plugin.domain.common.Image>() {
            @Override
            public com.thoughtworks.go.plugin.domain.common.Image onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getImageResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public Capabilities getCapabilities(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_CAPABILITIES, new DefaultPluginInteractionCallback<Capabilities>() {
            @Override
            public Capabilities onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getCapabilitiesFromResponseBody(responseBody);
            }
        });
    }

    @Override
    public List<PluginConfiguration> getElasticProfileMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_ELASTIC_AGENT_PROFILE_METADATA, new DefaultPluginInteractionCallback<List<PluginConfiguration>>() {
            @Override
            public List<PluginConfiguration> onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getElasticProfileMetadataResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public String getElasticProfileView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_ELASTIC_AGENT_PROFILE_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getProfileViewResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public boolean supportsClusterProfile() {
        return true;
    }

    @Override
    public ValidationResult validateElasticProfile(final String pluginId, final Map<String, String> configuration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_VALIDATE_ELASTIC_AGENT_PROFILE, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.validateElasticProfileRequestBody(configuration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getElasticProfileValidationResultResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public List<PluginConfiguration> getClusterProfileMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_CLUSTER_PROFILE_METADATA, new DefaultPluginInteractionCallback<List<PluginConfiguration>>() {
            @Override
            public List<PluginConfiguration> onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getElasticProfileMetadataResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public String getClusterProfileView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_CLUSTER_PROFILE_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getProfileViewResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public ValidationResult validateClusterProfile(String pluginId, Map<String, String> configuration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_VALIDATE_CLUSTER_PROFILE, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.validateElasticProfileRequestBody(configuration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getElasticProfileValidationResultResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public void createAgent(String pluginId, final String autoRegisterKey, final String environment, final Map<String, String> configuration, Map<String, String> clusterProfileConfiguration, JobIdentifier jobIdentifier) {
        pluginRequestHelper.submitRequest(pluginId, REQUEST_CREATE_AGENT, new DefaultPluginInteractionCallback<Void>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.createAgentRequestBody(autoRegisterKey, environment, configuration, clusterProfileConfiguration, jobIdentifier);
            }
        });
    }

    @Override
    public void serverPing(final String pluginId, List<Map<String, String>> clusterProfileConfigurations) {
        pluginRequestHelper.submitRequest(pluginId, REQUEST_SERVER_PING, new DefaultPluginInteractionCallback<Void>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.serverPingRequestBody(clusterProfileConfigurations);
            }
        });
    }

    @Override
    public boolean shouldAssignWork(String pluginId, final AgentMetadata agent, final String environment, final Map<String, String> configuration, Map<String, String> clusterProfileProperties, JobIdentifier identifier) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_SHOULD_ASSIGN_WORK, new DefaultPluginInteractionCallback<Boolean>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.shouldAssignWorkRequestBody(agent, environment, configuration, clusterProfileProperties, identifier);
            }

            @Override
            public Boolean onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.shouldAssignWorkResponseFromBody(responseBody);
            }
        });
    }

    @Override
    public String getPluginStatusReport(String pluginId, List<Map<String, String>> clusterProfiles) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_PLUGIN_STATUS_REPORT, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getPluginStatusReportRequestBody(clusterProfiles);
            }

            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getStatusReportView(responseBody);
            }
        });
    }

    @Override
    public String getAgentStatusReport(String pluginId, JobIdentifier identifier, String elasticAgentId, Map<String, String> clusterProfile) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_AGENT_STATUS_REPORT, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getAgentStatusReportRequestBody(identifier, elasticAgentId, clusterProfile);
            }

            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getStatusReportView(responseBody);
            }
        });
    }

    @Override
    public String getClusterStatusReport(String pluginId, Map<String, String> clusterProfile) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_CLUSTER_STATUS_REPORT, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getClusterStatusReportRequestBody(clusterProfile);
            }

            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getStatusReportView(responseBody);
            }
        });
    }

    @Override
    public void jobCompletion(String pluginId, String elasticAgentId, JobIdentifier jobIdentifier, Map<String, String> elasticProfileConfiguration, Map<String, String> clusterProfileConfiguration) {
        pluginRequestHelper.submitRequest(pluginId, REQUEST_JOB_COMPLETION, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getJobCompletionRequestBody(elasticAgentId, jobIdentifier, elasticProfileConfiguration, clusterProfileConfiguration);
            }
        });
    }

    @Override
    public ElasticAgentInformation migrateConfig(String pluginId, ElasticAgentInformation elasticAgentInformation) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_MIGRATE_CONFIGURATION, new DefaultPluginInteractionCallback<ElasticAgentInformation>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getElasticAgentInformationDTO(elasticAgentInformation).toJSON().toString();
            }

            @Override
            public ElasticAgentInformation onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getElasticAgentInformationFromResponseBody(responseBody);
            }
        });
    }

    @Override
    public void clusterProfilesChanged(String pluginId, ClusterProfilesChangedStatus status, Map<String, String> oldClusterProfile, Map<String, String> newClusterProfile) {
        pluginRequestHelper.submitRequest(pluginId, REQUEST_CLUSTER_PROFILE_CHANGED, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return elasticAgentExtensionConverterV5.getClusterProfileChangedRequestBody(status, oldClusterProfile, newClusterProfile);
            }
        });
    }
}
