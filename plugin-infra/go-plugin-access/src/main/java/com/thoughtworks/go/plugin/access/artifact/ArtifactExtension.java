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
package com.thoughtworks.go.plugin.access.artifact;

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.domain.ArtifactPlan;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.artifact.model.PublishArtifactResponse;
import com.thoughtworks.go.plugin.access.artifact.models.FetchArtifactEnvironmentVariable;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.access.artifact.ArtifactExtensionConstants.*;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ARTIFACT_EXTENSION;

@Component
public class ArtifactExtension extends AbstractExtension {
    private final HashMap<String, ArtifactMessageConverter> messageHandlerMap = new HashMap<>();

    @Autowired
    protected ArtifactExtension(PluginManager pluginManager, ExtensionsRegistry extensionsRegistry) {
        super(pluginManager, extensionsRegistry, new PluginRequestHelper(pluginManager, SUPPORTED_VERSIONS, ARTIFACT_EXTENSION), ARTIFACT_EXTENSION);
        addHandler(V1, new ArtifactMessageConverterV1(), new PluginSettingsJsonMessageHandler1_0());
        addHandler(V2, new ArtifactMessageConverterV2(), new PluginSettingsJsonMessageHandler1_0());
    }

    private void addHandler(String version, ArtifactMessageConverter extensionHandler, PluginSettingsJsonMessageHandler pluginSettingsJsonMessageHandler) {
        messageHandlerMap.put(version, extensionHandler);
        registerHandler(version, pluginSettingsJsonMessageHandler);
    }

    public com.thoughtworks.go.plugin.domain.artifact.Capabilities getCapabilities(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_CAPABILITIES, new DefaultPluginInteractionCallback<com.thoughtworks.go.plugin.domain.artifact.Capabilities>() {
            @Override
            public com.thoughtworks.go.plugin.domain.artifact.Capabilities onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getCapabilitiesFromResponseBody(responseBody);
            }
        });
    }

    public List<PluginConfiguration> getArtifactStoreMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_STORE_CONFIG_METADATA, new DefaultPluginInteractionCallback<List<PluginConfiguration>>() {
            @Override
            public List<PluginConfiguration> onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getMetadataResponseFromBody(responseBody);
            }
        });
    }

    public String getArtifactStoreView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_STORE_CONFIG_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getViewFromResponseBody(responseBody, "Artifact store view");
            }
        });
    }

    public ValidationResult validateArtifactStoreConfig(final String pluginId, final Map<String, String> configuration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_STORE_CONFIG_VALIDATE, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).validateConfigurationRequestBody(configuration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getConfigurationValidationResultFromResponseBody(responseBody);
            }
        });
    }

    public List<PluginConfiguration> getPublishArtifactMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_PUBLISH_ARTIFACT_METADATA, new DefaultPluginInteractionCallback<List<PluginConfiguration>>() {
            @Override
            public List<PluginConfiguration> onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getMetadataResponseFromBody(responseBody);
            }
        });
    }

    public String getPublishArtifactView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_PUBLISH_ARTIFACT_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getViewFromResponseBody(responseBody, "Publish artifact view");
            }
        });
    }

    public ValidationResult validatePluggableArtifactConfig(final String pluginId, final Map<String, String> configuration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_PUBLISH_ARTIFACT_VALIDATE, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).validateConfigurationRequestBody(configuration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getConfigurationValidationResultFromResponseBody(responseBody);
            }
        });
    }

    public PublishArtifactResponse publishArtifact(String pluginId, ArtifactPlan artifactPlan, ArtifactStore artifactStore, String agentWorkingDirectory, EnvironmentVariableContext environmentVariableContext) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_PUBLISH_ARTIFACT, new DefaultPluginInteractionCallback<PublishArtifactResponse>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).publishArtifactMessage(artifactPlan, artifactStore, agentWorkingDirectory, environmentVariableContext.getProperties());
            }

            @Override
            public PublishArtifactResponse onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).publishArtifactResponse(responseBody);
            }
        });
    }


    public List<PluginConfiguration> getFetchArtifactMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_FETCH_ARTIFACT_METADATA, new DefaultPluginInteractionCallback<List<PluginConfiguration>>() {
            @Override
            public List<PluginConfiguration> onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getMetadataResponseFromBody(responseBody);
            }
        });
    }

    public String getFetchArtifactView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_FETCH_ARTIFACT_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getViewFromResponseBody(responseBody, "Fetch artifact view");
            }
        });
    }

    public ValidationResult validateFetchArtifactConfig(final String pluginId, final Map<String, String> configuration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_FETCH_ARTIFACT_VALIDATE, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).validateConfigurationRequestBody(configuration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getConfigurationValidationResultFromResponseBody(responseBody);
            }
        });
    }

    com.thoughtworks.go.plugin.domain.common.Image getIcon(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_GET_PLUGIN_ICON, new DefaultPluginInteractionCallback<com.thoughtworks.go.plugin.domain.common.Image>() {
            @Override
            public com.thoughtworks.go.plugin.domain.common.Image onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getImageResponseFromBody(responseBody);
            }
        });
    }

    protected ArtifactMessageConverter getMessageHandler(String version) {
        return messageHandlerMap.get(version);
    }

    @Override
    public List<String> goSupportedVersions() {
        return SUPPORTED_VERSIONS;
    }


    public List<FetchArtifactEnvironmentVariable> fetchArtifact(String pluginId, ArtifactStore artifactStore, Configuration configuration, Map<String, Object> metadata, String agentWorkingDirectory) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_FETCH_ARTIFACT, new DefaultPluginInteractionCallback<List<FetchArtifactEnvironmentVariable>>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).fetchArtifactMessage(artifactStore, configuration, metadata, agentWorkingDirectory);
            }

            @Override
            public List<FetchArtifactEnvironmentVariable> onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getFetchArtifactEnvironmentVariablesFromResponseBody(responseBody);
            }
        });
    }
}
