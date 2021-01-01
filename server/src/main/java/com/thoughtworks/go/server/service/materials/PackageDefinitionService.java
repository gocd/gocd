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
package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.CreatePackageConfigCommand;
import com.thoughtworks.go.config.update.DeletePackageConfigCommand;
import com.thoughtworks.go.config.update.UpdatePackageConfigCommand;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.plugin.access.exceptions.SecretResolutionFailureException;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.RulesViolationException;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecretParamResolver;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.thoughtworks.go.i18n.LocalizedMessage.entityConfigValidationFailed;
import static com.thoughtworks.go.i18n.LocalizedMessage.saveFailedWithReason;

@Service
public class PackageDefinitionService {
    private EntityHashingService entityHashingService;
    private GoConfigService goConfigService;
    private final SecretParamResolver secretParamResolver;
    PackageRepositoryExtension packageRepositoryExtension;

    public static final Logger LOGGER = LoggerFactory.getLogger(PackageDefinitionService.class);

    @Autowired
    public PackageDefinitionService(PackageRepositoryExtension packageRepositoryExtension, EntityHashingService entityHashingService, GoConfigService goConfigService, SecretParamResolver secretParamResolver) {
        this.packageRepositoryExtension = packageRepositoryExtension;
        this.entityHashingService = entityHashingService;
        this.goConfigService = goConfigService;
        this.secretParamResolver = secretParamResolver;
    }

    public void performPluginValidationsFor(final PackageDefinition packageDefinition) {
        String pluginId = packageDefinition.getRepository().getPluginConfiguration().getId();


        ValidationResult validationResult = packageRepositoryExtension.isPackageConfigurationValid(pluginId, buildPackageConfigurations(packageDefinition), buildRepositoryConfigurations(packageDefinition.getRepository()));
        for (ValidationError error : validationResult.getErrors()) {
            packageDefinition.addConfigurationErrorFor(error.getKey(), error.getMessage());
        }
        for (ConfigurationProperty configurationProperty : packageDefinition.getConfiguration()) {
            String key = configurationProperty.getConfigurationKey().getName();
            if (PackageMetadataStore.getInstance().hasOption(packageDefinition.getRepository().getPluginConfiguration().getId(), key, PackageConfiguration.REQUIRED)) {
                if (configurationProperty.getValue().isEmpty() && configurationProperty.doesNotHaveErrorsAgainstConfigurationValue()) {
                    configurationProperty.addErrorAgainstConfigurationValue("Field: '" + configurationProperty.getConfigurationKey().getName() + "' is required");
                }
            }
        }
    }

    public boolean validatePackageConfiguration(final PackageDefinition packageDefinition) {
        String pluginId = packageDefinition.getRepository().getPluginConfiguration().getId();
        if (!packageDefinition.getRepository().doesPluginExist()) {
            throw new RuntimeException(String.format("Plugin with id '%s' is not found.", pluginId));
        }
        secretParamResolver.resolve(packageDefinition);
        ValidationResult validationResult = packageRepositoryExtension.isPackageConfigurationValid(pluginId, buildPackageConfigurations(packageDefinition), buildRepositoryConfigurations(packageDefinition.getRepository()));
        addErrorsToConfiguration(validationResult, packageDefinition);

        return validationResult.isSuccessful();
    }

    private void addErrorsToConfiguration(ValidationResult validationResult, PackageDefinition packageDefinition) {
        for (ValidationError validationError : validationResult.getErrors()) {
            ConfigurationProperty property = packageDefinition.getConfiguration().getProperty(validationError.getKey());

            if (property != null) {
                property.addError(validationError.getKey(), validationError.getMessage());
            } else {
                String validationErrorKey = StringUtils.isBlank(validationError.getKey()) ? PackageDefinition.CONFIGURATION : validationError.getKey();
                packageDefinition.addError(validationErrorKey, validationError.getMessage());
            }
        }
    }

    public void checkConnection(final PackageDefinition packageDefinition, final LocalizedOperationResult result) {
        try {
            String pluginId = packageDefinition.getRepository().getPluginConfiguration().getId();
            secretParamResolver.resolve(packageDefinition);
            Result checkConnectionResult = packageRepositoryExtension.checkConnectionToPackage(pluginId, buildPackageConfigurations(packageDefinition), buildRepositoryConfigurations(packageDefinition.getRepository()));
            String messages = checkConnectionResult.getMessagesForDisplay();
            if (!checkConnectionResult.isSuccessful()) {
                result.connectionError("Package check Failed. Reason(s): " + messages);
                return;
            }
            result.setMessage("OK. " + messages);
        } catch (Exception e) {
            if (e instanceof RulesViolationException || e instanceof SecretResolutionFailureException) {
                result.unprocessableEntity("Package check Failed. Reason(s): " + e.getMessage());
            } else {
                result.internalServerError("Package check Failed. Reason(s): " + e.getMessage());
            }
        }
    }

    private RepositoryConfiguration buildRepositoryConfigurations(PackageRepository packageRepository) {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        populateConfiguration(packageRepository.getConfiguration(), repositoryConfiguration);
        return repositoryConfiguration;
    }

    private void update(Username username, PackageDefinition packageDeinition, HttpLocalizedOperationResult result, EntityConfigUpdateCommand command) {
        try {
            goConfigService.updateConfig(command, username);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException && !result.hasMessage()) {
                result.unprocessableEntity(entityConfigValidationFailed(packageDeinition.getClass().getAnnotation(ConfigTag.class).value(), packageDeinition.getId(), e.getMessage()));
            } else if (e instanceof RulesViolationException || e instanceof SecretResolutionFailureException) {
                result.unprocessableEntity(saveFailedWithReason(e.getMessage()));
            } else {
                if (!result.hasMessage()) {
                    LOGGER.error(e.getMessage(), e);
                    result.internalServerError(saveFailedWithReason("An error occurred while saving the package config. Please check the logs for more information."));
                }
            }
        }
    }


    public PackageDefinition find(String packageId) {
        return goConfigService.findPackage(packageId);
    }

    public List<PackageDefinition> getPackages() {
        return goConfigService.getPackages();
    }

    public void deletePackage(PackageDefinition packageDefinition, Username username, HttpLocalizedOperationResult result) {
        DeletePackageConfigCommand command = new DeletePackageConfigCommand(goConfigService, packageDefinition, username, result);
        update(username, packageDefinition, result, command);
        if (result.isSuccessful()) {
            result.setMessage(EntityType.PackageDefinition.deleteSuccessful(packageDefinition.getId()));
        }
    }

    public void createPackage(PackageDefinition packageDefinition, String repositoryId, Username username, HttpLocalizedOperationResult result) {
        CreatePackageConfigCommand command = new CreatePackageConfigCommand(goConfigService, packageDefinition, repositoryId, username, result, this);
        update(username, packageDefinition, result, command);
    }

    public void updatePackage(String oldPackageId, PackageDefinition newPackage, String digest, Username username, HttpLocalizedOperationResult result) {
        UpdatePackageConfigCommand command = new UpdatePackageConfigCommand(goConfigService, oldPackageId, newPackage, username, digest, this.entityHashingService, result, this);
        update(username, this.find(oldPackageId), result, command);
    }

    private com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration buildPackageConfigurations(PackageDefinition packageDefinition) {
        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration = new com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration();
        populateConfiguration(packageDefinition.getConfiguration(), packageConfiguration);
        return packageConfiguration;
    }

    private void populateConfiguration(Configuration configuration, com.thoughtworks.go.plugin.api.config.Configuration pluginConfiguration) {
        for (ConfigurationProperty configurationProperty : configuration) {
            pluginConfiguration.add(new PackageMaterialProperty(configurationProperty.getConfigurationKey().getName(), configurationProperty.getResolvedValue()));
        }
    }

}

