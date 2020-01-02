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
package com.thoughtworks.go.plugin.access.artifact;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.domain.ArtifactPlan;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.plugin.access.artifact.model.PublishArtifactResponse;
import com.thoughtworks.go.plugin.access.artifact.models.FetchArtifactEnvironmentVariable;
import com.thoughtworks.go.plugin.access.common.handler.JSONResultMessageHandler;
import com.thoughtworks.go.plugin.access.common.models.ImageDeserializer;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArtifactMessageConverterV2 implements ArtifactMessageConverter {
    public static final String VERSION = ArtifactExtensionConstants.V2;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @Override
    public String publishArtifactMessage(ArtifactPlan artifactPlan, ArtifactStore artifactStore, String agentWorkingDirectory,
                                         Map<String, String> environmentVariables) {
        final Map<String, Object> messageObject = new HashMap<>();
        messageObject.put("artifact_store", getArtifactStore(artifactStore));
        messageObject.put("artifact_plan", artifactPlan.getPluggableArtifactConfiguration());
        messageObject.put("agent_working_directory", agentWorkingDirectory);
        messageObject.put("environment_variables", environmentVariables);
        return GSON.toJson(messageObject);
    }

    private Map getArtifactStore(ArtifactStore artifactStore) {
        final HashMap<String, Object> artifactStoreAndPlans = new HashMap<>();
        artifactStoreAndPlans.put("id", artifactStore.getId());
        artifactStoreAndPlans.put("configuration", artifactStore.getConfigurationAsMap(true));
        return artifactStoreAndPlans;
    }

    @Override
    public PublishArtifactResponse publishArtifactResponse(String responseBody) {
        return PublishArtifactResponse.fromJSON(responseBody);
    }

    @Override
    public List<PluginConfiguration> getMetadataResponseFromBody(String responseBody) {
        return PluginProfileMetadataKeys.fromJSON(responseBody).toPluginConfigurations();
    }

    @Override
    public String getViewFromResponseBody(String responseBody, final String viewLabel) {
        return getTemplateFromResponse(responseBody, String.format("%s `template` was blank!", viewLabel));
    }

    @Override
    public String validateConfigurationRequestBody(Map<String, String> configuration) {
        return GSON.toJson(configuration);
    }

    @Override
    public ValidationResult getConfigurationValidationResultFromResponseBody(String responseBody) {
        return new JSONResultMessageHandler().toValidationResult(responseBody);
    }

    @Override
    public String fetchArtifactMessage(ArtifactStore artifactStore, Configuration configuration, Map<String, Object> metadata, String agentWorkingDirectory) {
        final Map<String, Object> map = new HashMap<>();
        map.put("store_configuration", artifactStore.getConfigurationAsMap(true));
        map.put("fetch_artifact_configuration", configuration.getConfigurationAsMap(true));
        map.put("artifact_metadata", metadata);
        map.put("agent_working_directory", agentWorkingDirectory);
        return GSON.toJson(map);
    }

    @Override
    public Image getImageResponseFromBody(String responseBody) {
        return new ImageDeserializer().fromJSON(responseBody);
    }

    @Override
    public com.thoughtworks.go.plugin.domain.artifact.Capabilities getCapabilitiesFromResponseBody(String responseBody) {
        return com.thoughtworks.go.plugin.access.artifact.models.Capabilities.fromJSON(responseBody).toCapabilities();
    }

    @Override
    public List<FetchArtifactEnvironmentVariable> getFetchArtifactEnvironmentVariablesFromResponseBody(String responseBody) {
        return new Gson().fromJson(responseBody, new TypeToken<List<FetchArtifactEnvironmentVariable>>() {}.getType());
    }

    private String getTemplateFromResponse(String responseBody, String message) {
        String template = (String) new Gson().fromJson(responseBody, Map.class).get("template");
        if (StringUtils.isBlank(template)) {
            throw new RuntimeException(message);
        }
        return template;
    }
}
