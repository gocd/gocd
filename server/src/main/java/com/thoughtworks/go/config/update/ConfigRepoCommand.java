/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

abstract class ConfigRepoCommand implements EntityConfigUpdateCommand<ConfigRepoConfig> {

    private final SecurityService securityService;
    private ConfigRepoConfig preprocessedConfigRepo;
    private ConfigRepoConfig configRepo;
    private final Username username;
    private final HttpLocalizedOperationResult result;
    private ConfigRepoExtension configRepoExtension;

    public ConfigRepoCommand(SecurityService securityService, ConfigRepoConfig configRepo, Username username, HttpLocalizedOperationResult result,
                             ConfigRepoExtension configRepoExtension) {
        this.securityService = securityService;
        this.configRepo = configRepo;
        this.username = username;
        this.result = result;
        this.configRepoExtension = configRepoExtension;
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        validateConfigRepoId(this.configRepo);
        validateConfigRepoPluginId(this.configRepo);

        if (!this.configRepo.errors().isEmpty()) {
            return false;
        }

        preprocessedConfigRepo = preprocessedConfig.getConfigRepos().getConfigRepo(this.configRepo.getId());

        if (!preprocessedConfigRepo.validateTree(new ConfigSaveValidationContext(preprocessedConfig))) {
            BasicCruiseConfig.copyErrors(preprocessedConfigRepo, configRepo);
            return false;
        }

        return true;
    }

    /*
     * Ideally we should have all validations in the entity, validating 'id' and 'pluginId' as it
     * cannot be moved to ConfigRepoConfig
     * */
    private void validateConfigRepoId(ConfigRepoConfig configRepo) {
        if (isBlank(this.configRepo.getId())) {
            this.configRepo.addError("id", "Configuration repository id not specified");
        }
    }

    private void validateConfigRepoPluginId(ConfigRepoConfig configRepo) {
        if (isNotBlank(configRepo.getPluginId()) && !configRepoExtension.canHandlePlugin(configRepo.getPluginId())) {
            this.configRepo.addError("plugin_id", format("Invalid plugin id: %s", configRepo.getPluginId()));
        }
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
        return true;
    }
}
