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
package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.*;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.config.update.ErrorCollector.collectFieldErrors;
import static com.thoughtworks.go.config.update.ErrorCollector.collectGlobalErrors;
import static com.thoughtworks.go.i18n.LocalizedMessage.entityConfigValidationFailed;
import static com.thoughtworks.go.i18n.LocalizedMessage.saveFailedWithReason;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Service
public class PackageRepositoryService {
    private PluginManager pluginManager;
    private GoConfigService goConfigService;
    private SecurityService securityService;
    private EntityHashingService entityHashingService;
    private RepositoryMetadataStore repositoryMetadataStore;
    private PackageRepositoryExtension packageRepositoryExtension;

    public static final Logger LOGGER = LoggerFactory.getLogger(PackageRepositoryService.class);

    @Autowired
    public PackageRepositoryService(PluginManager pluginManager, PackageRepositoryExtension packageRepositoryExtension, GoConfigService goConfigService, SecurityService securityService,
                                    EntityHashingService entityHashingService) {
        this.pluginManager = pluginManager;
        this.packageRepositoryExtension = packageRepositoryExtension;
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.entityHashingService = entityHashingService;
        repositoryMetadataStore = RepositoryMetadataStore.getInstance();
    }

    public void checkConnection(final PackageRepository packageRepository, final LocalizedOperationResult result) {
        try {
            Result checkConnectionResult = packageRepositoryExtension.checkConnectionToRepository(packageRepository.getPluginConfiguration().getId(), populateConfiguration(packageRepository.getConfiguration()));
            String messages = checkConnectionResult.getMessagesForDisplay();
            if (!checkConnectionResult.isSuccessful()) {
                result.connectionError("Could not connect to package repository. Reason(s): " + messages);
                return;
            }
            result.setMessage("Connection OK. " + messages);
            return;
        } catch (Exception e) {
            result.internalServerError("Could not connect to package repository. Reason(s): " + e.getMessage());
        }
    }

    void performPluginValidationsFor(final PackageRepository packageRepository) {
        if (!validatePluginId(packageRepository)) {
            return;
        }
        for (ConfigurationProperty configurationProperty : packageRepository.getConfiguration()) {
            String key = configurationProperty.getConfigurationKey().getName();
            String pluginId = packageRepository.getPluginConfiguration().getId();
            if (repositoryMetadataStore.hasOption(pluginId, key, PackageConfiguration.REQUIRED)) {
                if (configurationProperty.getValue().isEmpty()) {
                    configurationProperty.addErrorAgainstConfigurationValue("This field is required");
                }
            }
        }

        ValidationResult validationResult = packageRepositoryExtension.isRepositoryConfigurationValid(packageRepository.getPluginConfiguration().getId(), populateConfiguration(packageRepository.getConfiguration()));
        for (ValidationError error : validationResult.getErrors()) {
            packageRepository.addConfigurationErrorFor(error.getKey(), error.getMessage());
        }
    }

    public boolean validateRepositoryConfiguration(final PackageRepository packageRepository) {
        if (!packageRepository.doesPluginExist()) {
            throw new RuntimeException(String.format("Plugin with id '%s' is not found.", packageRepository.getPluginConfiguration().getId()));
        }

        ValidationResult validationResult = packageRepositoryExtension.isRepositoryConfigurationValid(packageRepository.getPluginConfiguration().getId(), populateConfiguration(packageRepository.getConfiguration()));
        addErrorsToConfiguration(validationResult, packageRepository);

        return validationResult.isSuccessful();
    }

    private void addErrorsToConfiguration(ValidationResult validationResult, PackageRepository packageRepository) {
        for (ValidationError validationError : validationResult.getErrors()) {
            ConfigurationProperty property = packageRepository.getConfiguration().getProperty(validationError.getKey());

            if (property != null) {
                property.addError(validationError.getKey(), validationError.getMessage());
            } else {
                String validationErrorKey = StringUtils.isBlank(validationError.getKey()) ? PackageRepository.CONFIGURATION : validationError.getKey();
                packageRepository.addError(validationErrorKey, validationError.getMessage());
            }
        }
    }

    public boolean validatePluginId(PackageRepository packageRepository) {
        String pluginId = packageRepository.getPluginConfiguration().getId();
        if (isEmpty(pluginId)) {
            packageRepository.getPluginConfiguration().errors().add(PluginConfiguration.ID, "Please select package repository plugin");
            return false;
        }

        for (String currentPluginId : repositoryMetadataStore.getPlugins()) {
            if (currentPluginId.equals(pluginId)) {
                GoPluginDescriptor pluginDescriptor = pluginManager.getPluginDescriptorFor(pluginId);
                packageRepository.getPluginConfiguration().setVersion(pluginDescriptor.version());
                return true;
            }
        }
        packageRepository.getPluginConfiguration().errors().add(PluginConfiguration.ID, "Invalid plugin id");
        return false;
    }

    public PackageRepository getPackageRepository(String repoId) {
        return goConfigService.getPackageRepository(repoId);
    }

    public PackageRepositories getPackageRepositories() {
        return goConfigService.getPackageRepositories();
    }

    private void update(Username username, HttpLocalizedOperationResult result, EntityConfigUpdateCommand command, PackageRepository repository) {
        try {
            goConfigService.updateConfig(command, username);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException && !result.hasMessage()) {
                result.unprocessableEntity(entityConfigValidationFailed(repository.getClass().getAnnotation(ConfigTag.class).value(), repository.getId(), e.getMessage()));
            } else {
                if (!result.hasMessage()) {
                    LOGGER.error(e.getMessage(), e);
                    result.internalServerError(saveFailedWithReason("An error occurred while saving the package repository config. Please check the logs for more information."));
                }
            }
        }
    }

    //Used only in tests
    public void setPluginManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void deleteRepository(Username username, PackageRepository repository, HttpLocalizedOperationResult result) {
        DeletePackageRepositoryCommand command = new DeletePackageRepositoryCommand(goConfigService, repository, username, result);
        update(username, result, command, repository);
        if (result.isSuccessful()) {
            result.setMessage(EntityType.PackageRepository.deleteSuccessful(repository.getId()));
        }
    }

    public void createPackageRepository(PackageRepository repository, Username username, HttpLocalizedOperationResult result) {
        CreatePackageRepositoryCommand command = new CreatePackageRepositoryCommand(goConfigService, this, repository, username, result);
        update(username, result, command, repository);
    }

    public void updatePackageRepository(PackageRepository newRepo, Username username, String md5, HttpLocalizedOperationResult result, String oldRepoId) {
        UpdatePackageRepositoryCommand command = new UpdatePackageRepositoryCommand(goConfigService, this, newRepo, username, md5, entityHashingService, result, oldRepoId);
        update(username, result, command, newRepo);
    }

    private RepositoryConfiguration populateConfiguration(Configuration configuration) {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        for (ConfigurationProperty configurationProperty : configuration) {
            String value = configurationProperty.getValue();
            repositoryConfiguration.add(new PackageMaterialProperty(configurationProperty.getConfigurationKey().getName(), value));
        }
        return repositoryConfiguration;
    }
}

