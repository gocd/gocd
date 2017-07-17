/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.DefaultLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

import java.util.List;

public class ConfigRepoCommand {

    private final SecurityService securityService;
    private ConfigRepoConfig preprocessedconfigRepo;
    private ConfigRepoConfig configRepo;
    private final Localizable.CurryableLocalizable actionFailed;
    private final Username username;
    private final HttpLocalizedOperationResult result;

    public ConfigRepoCommand(SecurityService securityService, ConfigRepoConfig configRepo, Localizable.CurryableLocalizable actionFailed, Username username, HttpLocalizedOperationResult result) {
        this.securityService = securityService;
        this.configRepo = configRepo;
        this.actionFailed = actionFailed;
        this.username = username;
        this.result = result;
    }


    public boolean isValid(CruiseConfig preprocessedConfig) {
        ConfigReposConfig configRepos = preprocessedConfig.getConfigRepos();
        preprocessedconfigRepo = preprocessedConfig.getConfigRepos().getConfigRepo(this.configRepo.getId());


        configRepos.validate(new ConfigSaveValidationContext(preprocessedConfig));
        preprocessedconfigRepo.validate(new ConfigSaveValidationContext(preprocessedConfig));

        if (!configRepos.getAllErrors().isEmpty()) {
            List<String> errors = configRepos.getAllErrors().getAll();
            result.unprocessableEntity(actionFailed.addParam(errors));
            return false;
        }

        return this.configRepo.errors().isEmpty();
    }

    public void clearErrors() {
        BasicCruiseConfig.clearErrors(preprocessedconfigRepo);
    }

    public ConfigRepoConfig getPreprocessedEntityConfig() {
        return preprocessedconfigRepo;
    }

    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isUserAuthorized();
    }

    public boolean isUserAuthorized() {
        if (!securityService.isUserAdmin(username)) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT"), HealthStateType.unauthorised());
            return false;
        }
        return true;
    }
}
