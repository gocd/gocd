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

package com.thoughtworks.go.plugin.access.artifact;

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.domain.ArtifactPlan;
import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.artifact.model.PublishArtifactResponse;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.access.artifact.ArtifactExtensionConstants.*;
import static com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConstants.REQUEST_GET_AUTH_CONFIG_VIEW;
import static com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConstants.SUPPORTED_VERSIONS;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ARTIFACT_EXTENSION;

@Component
public class ArtifactExtension extends AbstractExtension {
    private final HashMap<String, ArtifactMessageConverter> messageHandlerMap = new HashMap<>();

    @Autowired
    protected ArtifactExtension(PluginManager pluginManager) {
        super(pluginManager, new PluginRequestHelper(pluginManager, SUPPORTED_VERSIONS, ARTIFACT_EXTENSION), ARTIFACT_EXTENSION);
        addHandler(ArtifactMessageConverterV1.VERSION, new ArtifactMessageConverterV1());
        registerHandler("1.0", new PluginSettingsJsonMessageHandler1_0());
    }

    private void addHandler(String version, ArtifactMessageConverter extensionHandler) {
        messageHandlerMap.put(version, extensionHandler);
    }

    List<PluginConfiguration> getArtifactStoreMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_STORE_CONFIG_METADATA, new DefaultPluginInteractionCallback<List<PluginConfiguration>>() {
            @Override
            public List<PluginConfiguration> onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getMetadataResponseFromBody(responseBody);
            }
        });
    }

    String getArtifactStoreView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_STORE_CONFIG_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getViewFromResponseBody(responseBody, "Artifact store view");
            }
        });
    }

    List<PluginConfiguration> getPublishArtifactMetadata(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_PUBLISH_ARTIFACT_METADATA, new DefaultPluginInteractionCallback<List<PluginConfiguration>>() {
            @Override
            public List<PluginConfiguration> onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getMetadataResponseFromBody(responseBody);
            }
        });
    }

    String getPublishArtifactView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_PUBLISH_ARTIFACT_VIEW, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).getViewFromResponseBody(responseBody, "Publish artifact view");
            }
        });
    }

    public PublishArtifactResponse publishArtifact(String pluginId, Map<ArtifactStore, List<ArtifactPlan>> artifactStoreToArtifactPlans) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_PUBLISH_ARTIFACT, new DefaultPluginInteractionCallback<PublishArtifactResponse>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).publishArtifactMessage(artifactStoreToArtifactPlans);
            }

            @Override
            public PublishArtifactResponse onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getMessageHandler(resolvedExtensionVersion).publishArtifactResponse(responseBody);
            }
        });
    }

    protected ArtifactMessageConverter getMessageHandler(String version) {
        return messageHandlerMap.get(version);
    }

    @Override
    protected List<String> goSupportedVersions() {
        return SUPPORTED_VERSIONS;
    }
}
