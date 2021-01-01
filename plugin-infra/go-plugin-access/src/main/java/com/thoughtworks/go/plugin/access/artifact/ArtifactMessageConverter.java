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
import com.thoughtworks.go.plugin.access.artifact.model.PublishArtifactResponse;
import com.thoughtworks.go.plugin.access.artifact.models.FetchArtifactEnvironmentVariable;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.artifact.Capabilities;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;

import java.util.List;
import java.util.Map;

public interface ArtifactMessageConverter {
    String publishArtifactMessage(ArtifactPlan artifactPlan, ArtifactStore artifactStore, String agentWorkingDirectory,
                                  Map<String, String> environmentVariables);

    PublishArtifactResponse publishArtifactResponse(String responseBody);

    List<PluginConfiguration> getMetadataResponseFromBody(String responseBody);

    String getViewFromResponseBody(String responseBody, final String viewLabel);

    String validateConfigurationRequestBody(Map<String, String> configuration);

    ValidationResult getConfigurationValidationResultFromResponseBody(String responseBody);

    String fetchArtifactMessage(ArtifactStore artifactStore, Configuration configuration, Map<String, Object> metadata, String agentWorkingDirectory);

    Image getImageResponseFromBody(String responseBody);

    Capabilities getCapabilitiesFromResponseBody(String responseBody);

    List<FetchArtifactEnvironmentVariable> getFetchArtifactEnvironmentVariablesFromResponseBody(String responseBody);
}
