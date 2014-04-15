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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.update.ErrorCollector;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProvider;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PackageDefinitionService {
    private PluginManager defaultPluginManager;
    private final Localizer localizer;

    @Autowired
    public PackageDefinitionService(PluginManager defaultPluginManager, Localizer localizer) {
        this.defaultPluginManager = defaultPluginManager;
        this.localizer = localizer;
    }

    public void performPluginValidationsFor(final PackageDefinition packageDefinition) {
        String pluginId = packageDefinition.getRepository().getPluginConfiguration().getId();

        defaultPluginManager.doOn(PackageMaterialProvider.class, pluginId, new ActionWithReturn<PackageMaterialProvider, Object>() {
            @Override
            public Object execute(PackageMaterialProvider packageMaterialProvider, GoPluginDescriptor pluginDescriptor) {
                ValidationResult validationResult = packageMaterialProvider.getConfig().isPackageConfigurationValid(buildPackageConfigurations(packageDefinition),
                        buildRepositoryConfigurations(packageDefinition.getRepository()));
                for (ValidationError error : validationResult.getErrors()) {
                    packageDefinition.addConfigurationErrorFor(error.getKey(), error.getMessage());
                }
                return null;
            }
        });

        for (ConfigurationProperty configurationProperty : packageDefinition.getConfiguration()) {
            String key = configurationProperty.getConfigurationKey().getName();
            if (PackageMetadataStore.getInstance().hasOption(packageDefinition.getRepository().getPluginConfiguration().getId(), key, PackageConfiguration.REQUIRED)) {
                if (configurationProperty.getValue().isEmpty() && configurationProperty.doesNotHaveErrorsAgainstConfigurationValue()) {
                    configurationProperty.addErrorAgainstConfigurationValue(localizer.localize("MANDATORY_CONFIGURATION_FIELD_WITH_NAME", configurationProperty.getConfigurationKey().getName()));
                }
            }
        }
    }

    private HashMap<String, List<String>> fieldErrors(Validatable subject, String filedErrorPrefix) {
        HashMap<String, List<String>> filedErrors = new HashMap<String, List<String>>();
        ErrorCollector.collectFieldErrors(filedErrors, filedErrorPrefix, subject);
        return filedErrors;
    }

    private List<String> globalErrors(List<ConfigErrors> allErrorsExceptSubject) {
        ArrayList<String> globalErrors = new ArrayList<String>();
        ErrorCollector.collectGlobalErrors(globalErrors, allErrorsExceptSubject);
        return globalErrors;
    }

    public void checkConnection(final PackageDefinition packageDefinition, final LocalizedOperationResult result) {
        try {
            defaultPluginManager.doOn(PackageMaterialProvider.class, packageDefinition.getRepository().getPluginConfiguration().getId(), new ActionWithReturn<PackageMaterialProvider, Object>() {
                @Override
                public Object execute(PackageMaterialProvider packageMaterialProvider, GoPluginDescriptor pluginDescriptor) {
                    Result checkConnectionResult = packageMaterialProvider.getPoller().checkConnectionToPackage(buildPackageConfigurations(packageDefinition),
                            buildRepositoryConfigurations(packageDefinition.getRepository()));
                    String messages = checkConnectionResult.getMessagesForDisplay();
                    if (!checkConnectionResult.isSuccessful()) {
                        result.connectionError(LocalizedMessage.string("PACKAGE_CHECK_FAILED", messages));
                        return result;
                    }
                    result.setMessage(LocalizedMessage.string("PACKAGE_CHECK_OK", messages));
                    return result;
                }
            });
        } catch (Exception e) {
            result.internalServerError(LocalizedMessage.string("PACKAGE_CHECK_FAILED", e.getMessage()));
        }
    }

    private RepositoryConfiguration buildRepositoryConfigurations(PackageRepository packageRepository) {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        populateConfiguration(packageRepository.getConfiguration(), repositoryConfiguration);
        return repositoryConfiguration;
    }

    private com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration buildPackageConfigurations(PackageDefinition packageDefinition) {
        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration = new com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration();
        populateConfiguration(packageDefinition.getConfiguration(), packageConfiguration);
        return packageConfiguration;
    }

    private void populateConfiguration(Configuration configuration, com.thoughtworks.go.plugin.api.config.Configuration pluginConfiguration) {
        for (ConfigurationProperty configurationProperty : configuration) {
            pluginConfiguration.add(new PackageMaterialProperty(configurationProperty.getConfigurationKey().getName(), configurationProperty.getValue()));
        }
    }

}

