/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.update.ConfigUpdateAjaxResponse;
import com.thoughtworks.go.config.update.ConfigUpdateResponse;
import com.thoughtworks.go.config.update.UpdateConfigFromUI;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.config.update.ErrorCollector.collectFieldErrors;
import static com.thoughtworks.go.config.update.ErrorCollector.collectGlobalErrors;
import static org.apache.commons.lang.StringUtils.isEmpty;

@Service
public class PackageRepositoryService {
    private PluginManager pluginManager;
    private GoConfigService goConfigService;
    private SecurityService securityService;
    private final Localizer localizer;
    private RepositoryMetadataStore repositoryMetadataStore;
    private PackageAsRepositoryExtension packageAsRepositoryExtension;

    @Autowired
    public PackageRepositoryService(PluginManager pluginManager, PackageAsRepositoryExtension packageAsRepositoryExtension, GoConfigService goConfigService, SecurityService securityService,
                                    Localizer localizer) {
        this.pluginManager = pluginManager;
        this.packageAsRepositoryExtension = packageAsRepositoryExtension;
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.localizer = localizer;
        repositoryMetadataStore = RepositoryMetadataStore.getInstance();
    }

    public ConfigUpdateAjaxResponse savePackageRepositoryToConfig(PackageRepository packageRepository, final String md5, Username username) {
        performPluginValidationsFor(packageRepository);
        UpdateConfigFromUI updateCommand = getPackageRepositoryUpdateCommand(packageRepository, username);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigUpdateResponse configUpdateResponse = goConfigService.updateConfigFromUI(updateCommand, md5, username, result);
        if (result.isSuccessful()) {
            ConfigUpdateAjaxResponse response = ConfigUpdateAjaxResponse.success(packageRepository.getId(), result.httpCode(),
                    configUpdateResponse.wasMerged() ? localizer.localize("CONFIG_MERGED") : localizer.localize("SAVED_CONFIGURATION_SUCCESSFULLY"));
            return response;
        } else {
            List<String> globalErrors = globalErrors(configUpdateResponse.getCruiseConfig().getAllErrorsExceptFor(configUpdateResponse.getSubject()));
            HashMap<String, List<String>> fieldErrors = fieldErrors(configUpdateResponse.getSubject(), "package_repository");
            String message = result.message(localizer);
            ConfigUpdateAjaxResponse response = ConfigUpdateAjaxResponse.failure(packageRepository.getId(), result.httpCode(), message, fieldErrors, globalErrors);
            return response;
        }
    }

    public void checkConnection(final PackageRepository packageRepository, final LocalizedOperationResult result) {
        try {
            Result checkConnectionResult = packageAsRepositoryExtension.checkConnectionToRepository(packageRepository.getPluginConfiguration().getId(), populateConfiguration(packageRepository.getConfiguration()));
            String messages = checkConnectionResult.getMessagesForDisplay();
            if (!checkConnectionResult.isSuccessful()) {
                result.connectionError(LocalizedMessage.string("PACKAGE_REPOSITORY_CHECK_CONNECTION_FAILED", messages));
                return;
            }
            result.setMessage(LocalizedMessage.string("CONNECTION_OK", messages));
            return;
        } catch (Exception e) {
            result.internalServerError(LocalizedMessage.string("PACKAGE_REPOSITORY_CHECK_CONNECTION_FAILED", e.getMessage()));
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
                    configurationProperty.addErrorAgainstConfigurationValue(localizer.localize("MANDATORY_CONFIGURATION_FIELD"));
                }
            }
        }

        ValidationResult validationResult = packageAsRepositoryExtension.isRepositoryConfigurationValid(packageRepository.getPluginConfiguration().getId(), populateConfiguration(packageRepository.getConfiguration()));
        for (ValidationError error : validationResult.getErrors()) {
            packageRepository.addConfigurationErrorFor(error.getKey(), error.getMessage());
        }
    }

    private boolean validatePluginId(PackageRepository packageRepository) {
        String pluginId = packageRepository.getPluginConfiguration().getId();
        if (isEmpty(pluginId)) {
            packageRepository.getPluginConfiguration().errors().add(PluginConfiguration.ID, localizer.localize("PLUGIN_ID_REQUIRED"));
            return false;
        }

        for (String currentPluginId : repositoryMetadataStore.getPlugins()) {
            if (currentPluginId.equals(pluginId)) {
                GoPluginDescriptor pluginDescriptor = pluginManager.getPluginDescriptorFor(pluginId);
                packageRepository.getPluginConfiguration().setVersion(pluginDescriptor.version());
                return true;
            }
        }
        packageRepository.getPluginConfiguration().errors().add(PluginConfiguration.ID, localizer.localize("PLUGIN_ID_INVALID"));
        return false;
    }


    UpdateConfigFromUI getPackageRepositoryUpdateCommand(final PackageRepository packageRepository, final Username username) {
        return new UpdateConfigFromUI() {

            @Override
            public void checkPermission(CruiseConfig cruiseConfig, LocalizedOperationResult result) {
                if (!securityService.canViewAdminPage(username)) {
                    result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_ADMINISTER"), null);
                }
            }

            @Override
            public Validatable node(CruiseConfig cruiseConfig) {
                return cruiseConfig;
            }

            @Override
            public Validatable updatedNode(CruiseConfig cruiseConfig) {
                return cruiseConfig;
            }

            @Override
            public void update(Validatable node) {
                ((CruiseConfig) node).savePackageRepository(packageRepository);
            }

            @Override
            public Validatable subject(Validatable node) {
                return ((CruiseConfig) node).getPackageRepositories().find(packageRepository.getRepoId());
            }

            @Override
            public Validatable updatedSubject(Validatable updatedNode) {
                return ((CruiseConfig) updatedNode).getPackageRepositories().find(packageRepository.getRepoId());
            }
        };
    }

    private HashMap<String, List<String>> fieldErrors(Validatable subject, String filedErrorPrefix) {
        HashMap<String, List<String>> filedErrors = new HashMap<>();
        //TODO; Ideally subject should not be null, but when xsd validations fails subject comes as null hence this fix
        if (subject != null) {
            collectFieldErrors(filedErrors, filedErrorPrefix, subject);
        }
        return filedErrors;
    }

    private List<String> globalErrors(List<ConfigErrors> allErrorsExceptSubject) {
        ArrayList<String> globalErrors = new ArrayList<>();
        collectGlobalErrors(globalErrors, allErrorsExceptSubject);
        return globalErrors;
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

