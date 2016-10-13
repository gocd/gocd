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
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.elastic.ElasticProfiles;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.ElasticProfileNotFoundException;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

import static com.thoughtworks.go.util.StringUtil.isBlank;

abstract class ElasticAgentProfileCommand implements EntityConfigUpdateCommand<ElasticProfile> {

    protected ElasticProfile preprocessedElasticProfile;
    protected final ElasticProfile elasticProfile;
    protected final GoConfigService goConfigService;
    protected final Username currentUser;
    protected final LocalizedOperationResult result;

    public ElasticAgentProfileCommand(ElasticProfile elasticProfile, GoConfigService goConfigService, Username currentUser, LocalizedOperationResult result) {
        this.elasticProfile = elasticProfile;
        this.goConfigService = goConfigService;
        this.currentUser = currentUser;
        this.result = result;
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(elasticProfile);
    }

    @Override
    public ElasticProfile getPreprocessedEntityConfig() {
        return preprocessedElasticProfile;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isAuthorized();
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedElasticProfile = findExistingProfile(preprocessedConfig);
        preprocessedElasticProfile.validate(null);

        if (preprocessedElasticProfile.errors().isEmpty()) {
            ElasticProfiles profiles = preprocessedConfig.server().getElasticConfig().getProfiles();
            profiles.validate(null);
            BasicCruiseConfig.copyErrors(preprocessedElasticProfile, elasticProfile);
            return preprocessedElasticProfile.getAllErrors().isEmpty() && elasticProfile.errors().isEmpty();
        }

        BasicCruiseConfig.copyErrors(preprocessedElasticProfile, elasticProfile);
        return false;
    }

    protected boolean isAuthorized() {
        if (!(goConfigService.isUserAdmin(currentUser) || goConfigService.isGroupAdministrator(currentUser.getUsername()))) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT"), HealthStateType.unauthorised());
            return false;
        }
        return true;
    }

    protected ElasticProfile findExistingProfile(CruiseConfig cruiseConfig) {
        if (elasticProfile == null || isBlank(elasticProfile.getId())) {
            result.unprocessableEntity(LocalizedMessage.string("ENTITY_ATTRIBUTE_NULL", "elastic agent profile", "id"));
            throw new IllegalArgumentException("Elastic profile id cannot be null.");
        } else {
            ElasticProfile profile = cruiseConfig.server().getElasticConfig().getProfiles().find(elasticProfile.getId());
            if (profile == null) {
                result.notFound(LocalizedMessage.string("RESOURCE_NOT_FOUND", "profile", elasticProfile.getId()), HealthStateType.notFound());
                throw new ElasticProfileNotFoundException();
            }
            return profile;
        }
    }
}
