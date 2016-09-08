/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.update.ConfigUpdateCheckFailedException;
import com.thoughtworks.go.config.update.CreateSCMConfigCommand;
import com.thoughtworks.go.config.update.UpdateSCMConfigCommand;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.plugin.access.scm.*;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.util.StringUtil;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class PluggableScmService {
    private SCMExtension scmExtension;
    private Localizer localizer;
    private GoConfigService goConfigService;
    private org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PluggableScmService.class);
    private EntityHashingService entityHashingService;

    @Autowired
    public PluggableScmService(SCMExtension scmExtension, Localizer localizer, GoConfigService goConfigService, EntityHashingService entityHashingService) {
        this.scmExtension = scmExtension;
        this.localizer = localizer;
        this.goConfigService = goConfigService;
        this.entityHashingService = entityHashingService;
    }

    public void validate(final SCM scmConfig) {
        final String pluginId = scmConfig.getPluginConfiguration().getId();
        final SCMPropertyConfiguration configuration = getScmPropertyConfiguration(scmConfig);
        ValidationResult validationResult = scmExtension.isSCMConfigurationValid(pluginId, configuration);

        if (SCMMetadataStore.getInstance().hasPreferenceFor(pluginId)) {
            SCMConfigurations configurationMetadata = SCMMetadataStore.getInstance().getConfigurationMetadata(pluginId);
            for (SCMConfiguration scmConfiguration : configurationMetadata.list()) {
                String key = scmConfiguration.getKey();
                boolean isRequired = SCMMetadataStore.getInstance().hasOption(pluginId, key, Property.REQUIRED);
                ConfigurationProperty property = scmConfig.getConfiguration().getProperty(key);
                String configValue = property == null ? null : property.getValue();
                if (isRequired && StringUtil.isBlank(configValue)) {
                    validationResult.addError(new ValidationError(key, localizer.localize("MANDATORY_CONFIGURATION_FIELD")));
                }
            }
        }

        for (ValidationError validationError : validationResult.getErrors()) {
            ConfigurationProperty property = scmConfig.getConfiguration().getProperty(validationError.getKey());
            if (property != null) {
                property.addError(validationError.getKey(), validationError.getMessage());
            }
        }
    }

    public boolean isValid(final SCM scmConfig) {
        if (!scmConfig.doesPluginExist()) {
            throw new RuntimeException(String.format("Plugin with id '%s' is not found.", scmConfig.getPluginConfiguration().getId()));
        }

        ValidationResult validationResult = scmExtension.isSCMConfigurationValid(scmConfig.getPluginConfiguration().getId(), getScmPropertyConfiguration(scmConfig));
        addErrorsToConfiguration(validationResult, scmConfig);

        return validationResult.isSuccessful();
    }

    private void addErrorsToConfiguration(ValidationResult validationResult, SCM scmConfig) {
        for (ValidationError validationError : validationResult.getErrors()) {
            ConfigurationProperty property = scmConfig.getConfiguration().getProperty(validationError.getKey());

            if (property != null) {
                property.addError(validationError.getKey(), validationError.getMessage());
            } else {
                scmConfig.addError(validationError.getKey(), validationError.getMessage());
            }
        }
    }


    public ArrayList<SCM> listAllScms() {
        return goConfigService.getSCMs();
    }

    public SCM findPluggableScmMaterial(String materialName) {
        ArrayList<SCM> scms = listAllScms();
        for(SCM scm : scms){
            if(materialName.equals(scm.getName()))  {
                return scm;
            }
        }
        return null;
    }

    public Result checkConnection(final SCM scmConfig) {
        final String pluginId = scmConfig.getPluginConfiguration().getId();
        final SCMPropertyConfiguration configuration = getScmPropertyConfiguration(scmConfig);
        return scmExtension.checkConnectionToSCM(pluginId, configuration);
    }

    public void createPluggableScmMaterial(final Username currentUser, final SCM globalScmConfig, final LocalizedOperationResult result) {
        CreateSCMConfigCommand command = new CreateSCMConfigCommand(globalScmConfig, this, result, currentUser, goConfigService);
        update(currentUser, result, command);
    }

    public void updatePluggableScmMaterial(final Username currentUser, final SCM globalScmConfig, final LocalizedOperationResult result, String md5) {
        UpdateSCMConfigCommand command = new UpdateSCMConfigCommand(globalScmConfig, this, goConfigService, currentUser, result, md5, entityHashingService);
        update(currentUser, result, command);
    }

    private void update(Username currentUser, LocalizedOperationResult result, EntityConfigUpdateCommand command) {
        try {
            goConfigService.updateConfig(command, currentUser);
        } catch (Exception e) {
            if (!result.hasMessage()) {
                result.internalServerError(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", "An error occurred while saving the template config. Please check the logs for more information."));
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private SCMPropertyConfiguration getScmPropertyConfiguration(SCM scmConfig) {
        final SCMPropertyConfiguration configuration = new SCMPropertyConfiguration();
        for (ConfigurationProperty configurationProperty : scmConfig.getConfiguration()) {
            configuration.add(new SCMProperty(configurationProperty.getConfigurationKey().getName(), configurationProperty.getValue()));
        }
        return configuration;
    }
}
