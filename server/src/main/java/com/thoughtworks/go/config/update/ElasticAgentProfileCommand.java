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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.elastic.ElasticProfiles;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static com.thoughtworks.go.i18n.LocalizedMessage.resourceNotFound;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;
import static com.thoughtworks.go.serverhealth.HealthStateType.notFound;

public abstract class ElasticAgentProfileCommand implements EntityConfigUpdateCommand<ElasticProfile> {
    private final GoConfigService goConfigService;
    private final ElasticAgentExtension extension;
    private final Username currentUser;
    final LocalizedOperationResult result;
    final ElasticProfile elasticProfile;
    ElasticProfile preprocessedProfile;

    public ElasticAgentProfileCommand(GoConfigService goConfigService, ElasticProfile profile, ElasticAgentExtension extension, Username currentUser, LocalizedOperationResult result) {
        this.goConfigService = goConfigService;
        this.elasticProfile = profile;
        this.extension = extension;
        this.currentUser = currentUser;
        this.result = result;
    }

    protected ElasticProfiles getPluginProfiles(CruiseConfig preprocessedConfig) {
        return preprocessedConfig.getElasticConfig().getProfiles();
    }

    public ValidationResult validateUsingExtension(String pluginId, Map<String, String> configuration) {
        return extension.validate(pluginId, configuration);
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(elasticProfile);
    }

    @Override
    public ElasticProfile getPreprocessedEntityConfig() {
        return preprocessedProfile;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isAuthorized();
    }

    protected EntityType getObjectDescriptor() {
        return EntityType.ElasticProfile;
    }

    protected final boolean isAuthorized() {
        return true;
    }

    protected boolean isValidForCreateOrUpdate(CruiseConfig preprocessedConfig) {
        preprocessedProfile = findExistingProfile(preprocessedConfig);
        preprocessedProfile.validateTree(new ConfigSaveValidationContext(preprocessedConfig));

        if (preprocessedProfile.getAllErrors().isEmpty()) {
            getPluginProfiles(preprocessedConfig).validate(null);
            BasicCruiseConfig.copyErrors(preprocessedProfile, elasticProfile);
            return preprocessedProfile.getAllErrors().isEmpty();
        }

        BasicCruiseConfig.copyErrors(preprocessedProfile, elasticProfile);
        return false;
    }

    protected final ElasticProfile findExistingProfile(CruiseConfig cruiseConfig) {
        if (elasticProfile == null || StringUtils.isBlank(elasticProfile.getId())) {
            if (elasticProfile != null) {
                elasticProfile.addError("id", getObjectDescriptor() + " cannot have a blank id.");
            }
            result.unprocessableEntity("The " + getObjectDescriptor().getEntityNameLowerCase() + " config is invalid. Attribute 'id' cannot be null.");
            throw new IllegalArgumentException(getObjectDescriptor().idCannotBeBlank());
        } else {
            ElasticProfile profile = getPluginProfiles(cruiseConfig).find(this.elasticProfile.getId());
            if (profile == null) {
                result.notFound(resourceNotFound(getTagName(), elasticProfile.getId()), notFound());
                throw new RecordNotFoundException(getObjectDescriptor(), elasticProfile.getId());
            }
            return profile;
        }
    }

    private String getTagName() {
        return elasticProfile.getClass().getAnnotation(ConfigTag.class).value();
    }
}
