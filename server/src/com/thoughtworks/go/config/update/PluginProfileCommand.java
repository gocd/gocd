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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginProfileNotFoundException;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

import java.util.Map;

import static com.thoughtworks.go.util.StringUtil.isBlank;

abstract class PluginProfileCommand<T extends PluginProfile, M extends PluginProfiles<T>> implements EntityConfigUpdateCommand<T> {
    protected final GoConfigService goConfigService;
    protected final T profile;
    protected final Username currentUser;
    protected final LocalizedOperationResult result;
    protected T preprocessedProfile;

    public PluginProfileCommand(GoConfigService goConfigService, T profile, Username currentUser, LocalizedOperationResult result) {
        this.goConfigService = goConfigService;
        this.profile = profile;
        this.currentUser = currentUser;
        this.result = result;
    }

    protected abstract M getPluginProfiles(CruiseConfig preprocessedConfig);

    protected abstract ValidationResult validateUsingExtension(final String pluginId, final Map<String, String> configuration);

    protected abstract String getObjectDescriptor();

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(profile);
    }

    @Override
    public T getPreprocessedEntityConfig() {
        return preprocessedProfile;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isAuthorized();
    }

    protected boolean isValidForCreateOrUpdate(CruiseConfig preprocessedConfig) {
        preprocessedProfile = findExistingProfile(preprocessedConfig);
        preprocessedProfile.validate(null);
        ValidationResult result = validateUsingExtension(preprocessedProfile.getPluginId(), profile.getConfigurationAsMap(true));
        if (!result.isSuccessful()) {
            for (ValidationError validationError : result.getErrors()) {
                ConfigurationProperty property = preprocessedProfile.getProperty(validationError.getKey());
                if (property == null) {
                    profile.addNewConfiguration(validationError.getKey(), false);
                    preprocessedProfile.addNewConfiguration(validationError.getKey(), false);
                    property = preprocessedProfile.getProperty(validationError.getKey());
                }
                property.addError(validationError.getKey(), validationError.getMessage());
            }
        }
        if (preprocessedProfile.errors().isEmpty()) {
            getPluginProfiles(preprocessedConfig).validate(null);
            BasicCruiseConfig.copyErrors(preprocessedProfile, profile);
            return preprocessedProfile.getAllErrors().isEmpty() && profile.errors().isEmpty();
        }

        BasicCruiseConfig.copyErrors(preprocessedProfile, profile);
        return false;
    }

    protected final T findExistingProfile(CruiseConfig cruiseConfig) {
        if (profile == null || isBlank(profile.getId())) {
            if (profile != null) {
                profile.addError("id", getObjectDescriptor() + " cannot have a blank id.");
            }
            result.unprocessableEntity(LocalizedMessage.string("ENTITY_ATTRIBUTE_NULL", getObjectDescriptor().toLowerCase(), "id"));
            throw new IllegalArgumentException(getObjectDescriptor() + " id cannot be null.");
        } else {
            T t = getPluginProfiles(cruiseConfig).find(profile.getId());
            if (t == null) {
                result.notFound(LocalizedMessage.string("RESOURCE_NOT_FOUND", getTagName(), profile.getId()), HealthStateType.notFound());
                throw new PluginProfileNotFoundException(getObjectDescriptor() + " `" + profile.getId() + "` does not exist.");
            }
            return t;
        }
    }

    private String getTagName() {
        return profile.getClass().getAnnotation(ConfigTag.class).value();
    }

    protected final boolean isAuthorized() {
        if (!(goConfigService.isUserAdmin(currentUser) || goConfigService.isGroupAdministrator(currentUser.getUsername()))) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT"), HealthStateType.unauthorised());
            return false;
        }
        return true;
    }

}
