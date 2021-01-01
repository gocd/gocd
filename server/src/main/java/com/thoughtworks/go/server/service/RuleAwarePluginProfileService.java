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
package com.thoughtworks.go.server.service;


import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.rules.RuleAwarePluginProfile;
import com.thoughtworks.go.config.rules.RuleAwarePluginProfiles;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.update.RuleAwarePluginProfileCommand;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.GoPluginFrameworkException;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.thoughtworks.go.i18n.LocalizedMessage.entityConfigValidationFailed;
import static com.thoughtworks.go.i18n.LocalizedMessage.saveFailedWithReason;

public abstract class RuleAwarePluginProfileService<M extends RuleAwarePluginProfile> {
    protected final GoConfigService goConfigService;
    protected final EntityHashingService hashingService;
    protected Logger LOGGER = LoggerFactory.getLogger(getClass());

    public RuleAwarePluginProfileService(GoConfigService goConfigService, EntityHashingService hashingService) {
        this.goConfigService = goConfigService;
        this.hashingService = hashingService;
    }

    protected abstract RuleAwarePluginProfiles<M> getPluginProfiles();

    public M findProfile(String id) {
        return getPluginProfiles().find(id);
    }

    private void validatePluginProperties(RuleAwarePluginProfileCommand command, RuleAwarePluginProfile newPluginProfile) {
        try {
            ValidationResult result = command.validateUsingExtension(newPluginProfile.getPluginId(), newPluginProfile.getConfiguration().getConfigurationAsMap(true));
            addErrorsToConfiguration(result, newPluginProfile);
        }catch (RecordNotFoundException e) {
            newPluginProfile.addError("pluginId", String.format("Plugin with id `%s` is not found.", newPluginProfile.getPluginId()));
        } catch (GoPluginFrameworkException e) {
            newPluginProfile.addError("pluginId", e.getMessage());
        }catch (Exception e) {
            //Ignore - it will be the invalid cipher text exception for an encrypted value. This will be validated later during entity update
        }
    }

    private void addErrorsToConfiguration(ValidationResult result, RuleAwarePluginProfile newSecurityAuthConfig) {
        if (!result.isSuccessful()) {
            for (com.thoughtworks.go.plugin.api.response.validation.ValidationError validationError : result.getErrors()) {
                ConfigurationProperty property = newSecurityAuthConfig.getConfiguration().getProperty(validationError.getKey());

                if (property != null) {
                    property.addError(validationError.getKey(), validationError.getMessage());
                } else {
                    newSecurityAuthConfig.addError(validationError.getKey(), validationError.getMessage());
                }
            }
        }
    }

    protected void update(Username currentUser, M pluginProfile, LocalizedOperationResult result, RuleAwarePluginProfileCommand cmd, boolean validatePluginProperties) {
        try {
            if (validatePluginProperties) {
                validatePluginProperties(cmd, pluginProfile);
            }
            goConfigService.updateConfig(cmd, currentUser);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException) {
                result.unprocessableEntity(entityConfigValidationFailed(pluginProfile.getClass().getAnnotation(ConfigTag.class).value(), pluginProfile.getId(), e.getMessage()));
            } else {
                if (!result.hasMessage()) {
                    LOGGER.error(e.getMessage(), e);
                    result.internalServerError(saveFailedWithReason("An error occurred while saving the profile config. Please check the logs for more information."));
                }
            }
        }
    }
}
