/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.apache.commons.lang3.StringUtils;

import static com.thoughtworks.go.i18n.LocalizedMessage.forbiddenToEdit;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;

abstract class ConfigRepoCommand implements EntityConfigUpdateCommand<ConfigRepoConfig> {

    private final SecurityService securityService;
    private ConfigRepoConfig preprocessedConfigRepo;
    private ConfigRepoConfig configRepo;
    private final Username username;
    private final HttpLocalizedOperationResult result;

    public ConfigRepoCommand(SecurityService securityService, ConfigRepoConfig configRepo, Username username, HttpLocalizedOperationResult result) {
        this.securityService = securityService;
        this.configRepo = configRepo;
        this.username = username;
        this.result = result;
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        if (StringUtils.isBlank(this.configRepo.getId())) {
            this.configRepo.addError("id", "Configuration repository id not specified");
            return false;
        }

        preprocessedConfigRepo = preprocessedConfig.getConfigRepos().getConfigRepo(this.configRepo.getId());

        if (!preprocessedConfigRepo.validateTree(new ConfigSaveValidationContext(preprocessedConfig))) {
            BasicCruiseConfig.copyErrors(preprocessedConfigRepo, configRepo);
            return false;
        }

        return true;
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(preprocessedConfigRepo);
    }

    @Override
    public ConfigRepoConfig getPreprocessedEntityConfig() {
        return preprocessedConfigRepo;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isUserAuthorized();
    }

    private boolean isUserAuthorized() {
        if (!securityService.isUserAdmin(username)) {
            result.forbidden(forbiddenToEdit(), forbidden());
            return false;
        }
        return true;
    }
}
