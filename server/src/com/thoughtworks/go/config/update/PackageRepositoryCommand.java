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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.materials.PackageRepositoryService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

import static com.thoughtworks.go.config.ErrorCollector.getAllErrors;

public class PackageRepositoryCommand {
    private PackageRepositoryService packageRepositoryService;
    private final PackageRepository repository;
    private final PluginManager pluginManager;
    private final HttpLocalizedOperationResult result;
    private PackageRepository preprocessedRepository;

    public PackageRepositoryCommand(PackageRepositoryService packageRepositoryService, PackageRepository repository, PluginManager pluginManager, HttpLocalizedOperationResult result) {
        this.packageRepositoryService = packageRepositoryService;
        this.repository = repository;
        this.pluginManager = pluginManager;
        this.result = result;
    }

    public boolean isValid(CruiseConfig preprocessedConfig) {
        PackageRepositories repositories = preprocessedConfig.getPackageRepositories();
        this.preprocessedRepository = repositories.find(this.repository.getRepoId());
        preprocessedRepository.validate(new ConfigSaveValidationContext(repositories));
        repositories.validate(new ConfigSaveValidationContext(preprocessedConfig));
        validatePluginConfiguration(preprocessedRepository.getPluginConfiguration());
        ValidateRepositoryConfiguration();
        BasicCruiseConfig.copyErrors(preprocessedRepository, this.repository);
        return getAllErrors(this.repository).isEmpty() && result.isSuccessful();
    }

    private void ValidateRepositoryConfiguration() {
        PackageAsRepositoryExtension extension = new PackageAsRepositoryExtension(this.pluginManager);
        String pluginId = preprocessedRepository.getPluginConfiguration().getId();
        RepositoryConfiguration repoConfig = getRepoConfiguration(preprocessedRepository.getConfiguration());
        ValidationResult validationResult = extension.isRepositoryConfigurationValid(pluginId, repoConfig);
        Configuration repoConfiguration = this.repository.getConfiguration();
        for (ValidationError error : validationResult.getErrors()) {
            if (!repoConfiguration.listOfConfigKeys().contains(error.getKey())) {
                addConfigProperty(this.repository, error.getKey(), null);
            }
            this.repository.addConfigurationErrorFor(error.getKey(), error.getMessage());
        }
    }

    private void addConfigProperty(PackageRepository repository, String key, String value) {
        Configuration configuration = repository.getConfiguration();
        configuration.add(new ConfigurationProperty(new ConfigurationKey(key), new ConfigurationValue(value)));
        repository.setConfiguration(configuration);
    }

    private RepositoryConfiguration getRepoConfiguration(Configuration configuration) {
        RepositoryConfiguration repoConfig = new RepositoryConfiguration();
        for (ConfigurationProperty configurationProperty : configuration) {
            String value = configurationProperty.getValue();
            repoConfig.add(new PackageMaterialProperty(configurationProperty.getConfigurationKey().getName(), value));
        }
        return repoConfig;
    }

    private void validatePluginConfiguration(PluginConfiguration pluginConfiguration) {
        String pluginId = pluginConfiguration.getId();
        String pluginVersion = pluginConfiguration.getVersion();
        GoPluginDescriptor pluginDescriptor = this.pluginManager.getPluginDescriptorFor(pluginId);
        if (pluginDescriptor == null) {
            result.unprocessableEntity(LocalizedMessage.string("INVALID_PLUGIN_TYPE", pluginId));
        } else if (!pluginDescriptor.version().equals(pluginVersion)) {
            result.unprocessableEntity(LocalizedMessage.string("INVALID_PLUGIN_VERSION", pluginVersion, pluginId));
        }
    }

    public void clearErrors() {
        BasicCruiseConfig.clearErrors(this.preprocessedRepository);
    }

    public PackageRepository getPreprocessedEntityConfig() {
        return this.preprocessedRepository;
    }
}
