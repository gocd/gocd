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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.rules.RuleAwarePluginProfile;
import com.thoughtworks.go.config.rules.RuleAwarePluginProfiles;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static com.thoughtworks.go.i18n.LocalizedMessage.resourceNotFound;
import static com.thoughtworks.go.serverhealth.HealthStateType.notFound;

public abstract class RuleAwarePluginProfileCommand<T extends RuleAwarePluginProfile, M extends RuleAwarePluginProfiles<T>> implements EntityConfigUpdateCommand<T> {
    protected final GoConfigService goConfigService;
    protected final T profile;
    protected final Username currentUser;
    protected final LocalizedOperationResult result;
    protected T preprocessedProfile;

    public RuleAwarePluginProfileCommand(GoConfigService goConfigService, T profile, Username currentUser, LocalizedOperationResult result) {
        this.goConfigService = goConfigService;
        this.profile = profile;
        this.currentUser = currentUser;
        this.result = result;
    }

    protected abstract M getPluginProfiles(CruiseConfig preprocessedConfig);

    public abstract ValidationResult validateUsingExtension(final String pluginId, final Map<String, String> configuration);

    protected abstract EntityType getObjectDescriptor();

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
        preprocessedProfile.validateTree(new ConfigSaveValidationContext(preprocessedConfig));

        if (!preprocessedProfile.hasErrors()) {
            getPluginProfiles(preprocessedConfig).validate(null);
            BasicCruiseConfig.copyErrors(preprocessedProfile, profile);
            return !preprocessedProfile.hasErrors();
        }

        BasicCruiseConfig.copyErrors(preprocessedProfile, profile);
        return false;
    }

    protected final T findExistingProfile(CruiseConfig cruiseConfig) {
        if (profile == null || StringUtils.isBlank(profile.getId())) {
            if (profile != null) {
                profile.addError("id", getObjectDescriptor() + " cannot have a blank id.");
            }
            result.unprocessableEntity("The " + getObjectDescriptor().getEntityNameLowerCase() + " config is invalid. Attribute 'id' cannot be null.");
            throw new IllegalArgumentException(getObjectDescriptor().idCannotBeBlank());
        } else {
            T t = getPluginProfiles(cruiseConfig).find(profile.getId());
            if (t == null) {
                result.notFound(resourceNotFound(getTagName(), profile.getId()), notFound());
                throw new RecordNotFoundException(getObjectDescriptor(), profile.getId());
            }
            return t;
        }
    }

    private String getTagName() {
        return profile.getClass().getAnnotation(ConfigTag.class).value();
    }

    protected abstract boolean isAuthorized();

}
