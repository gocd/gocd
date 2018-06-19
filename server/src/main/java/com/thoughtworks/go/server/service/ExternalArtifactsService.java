/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.PluginNotFoundException;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/*
    handles publish and fetch configuration of external artifacts
*/
@Service
public class ExternalArtifactsService {

    private ArtifactExtension artifactExtension;

    @Autowired
    public ExternalArtifactsService(ArtifactExtension artifactExtension) {
        this.artifactExtension = artifactExtension;
    }

    public void validateExternalArtifactConfig(PluggableArtifactConfig pluggableArtifactConfig, ArtifactStore artifactStore, ValidationContext validationContext) {
        if (pluggableArtifactConfig.hasValidPluginAndStore(validationContext)) {
            try {
                ValidationResult validationResult = artifactExtension.validatePluggableArtifactConfig(artifactStore.getPluginId(), pluggableArtifactConfig.getConfiguration().getConfigurationAsMap(true));
                mapErrorsToConfiguration(validationResult, pluggableArtifactConfig.getConfiguration(), pluggableArtifactConfig);

            } catch (PluginNotFoundException e) {
                pluggableArtifactConfig.addError("pluginId", String.format("Plugin with id `%s` is not found.", artifactStore.getPluginId()));
            }
        }
        else {
            pluggableArtifactConfig.addError("storeId", String.format("Could not find the artifact store `%s` to do plugin validations", pluggableArtifactConfig.getStoreId()));;
        }
    }

    public void validateFetchExternalArtifactTask(FetchPluggableArtifactTask fetchPluggableArtifactTask, ValidationContext validationContext, PipelineConfig pipelineConfig) {
        PluggableArtifactConfig specifiedExternalArtifact = fetchPluggableArtifactTask.getSpecifiedExternalArtifact(validationContext.getCruiseConfig(), pipelineConfig);
        if (specifiedExternalArtifact.hasValidPluginAndStore(validationContext)) {
            try {
                ValidationResult validationResult = artifactExtension.validateFetchArtifactConfig(specifiedExternalArtifact.getArtifactStore().getPluginId(), fetchPluggableArtifactTask.getConfiguration().getConfigurationAsMap(true));
                mapErrorsToConfiguration(validationResult, fetchPluggableArtifactTask.getConfiguration(), fetchPluggableArtifactTask);

            } catch (PluginNotFoundException e) {
                fetchPluggableArtifactTask.addError("pluginId", String.format("Plugin with id `%s` is not found.", specifiedExternalArtifact.getArtifactStore().getPluginId()));
            }
        }
    }

    private void mapErrorsToConfiguration(ValidationResult result, Configuration configuration, Validatable validatableConfig) {
        for (ValidationError validationError : result.getErrors()) {
            ConfigurationProperty property = configuration.getProperty(validationError.getKey());

            if (property == null) {
                validatableConfig.addError(validationError.getKey(), validationError.getMessage());
            } else {
                property.addError(validationError.getKey(), validationError.getMessage());
            }
        }
    }
}
