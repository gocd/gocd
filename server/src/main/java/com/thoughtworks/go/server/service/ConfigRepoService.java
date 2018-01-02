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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.update.CreateConfigRepoCommand;
import com.thoughtworks.go.config.update.DeleteConfigRepoCommand;
import com.thoughtworks.go.config.update.UpdateConfigRepoCommand;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.materials.PackageDefinitionService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigRepoService {
    private final GoConfigService goConfigService;
    private SecurityService securityService;
    private EntityHashingService entityHashingService;
    public static final Logger LOGGER = LoggerFactory.getLogger(PackageDefinitionService.class);

    @Autowired
    public ConfigRepoService(GoConfigService goConfigService, SecurityService securityService, EntityHashingService entityHashingService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.entityHashingService = entityHashingService;
    }

    public ConfigRepoConfig getConfigRepo(String repoId) {
        return goConfigService.getConfigForEditing().getConfigRepos().getConfigRepo(repoId);
    }

    private void update(Username username, String repoId, HttpLocalizedOperationResult result, EntityConfigUpdateCommand command) {
        try {
            goConfigService.updateConfig(command, username);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException && !result.hasMessage()) {
                result.unprocessableEntity(LocalizedMessage.string("ENTITY_CONFIG_VALIDATION_FAILED", ConfigRepoConfig.class.getAnnotation(ConfigTag.class).value(), repoId, e.getMessage()));
            } else {
                if (!result.hasMessage()) {
                    LOGGER.error(e.getMessage(), e);
                    result.internalServerError(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", "An error occurred while saving the package config. Please check the logs for more information."));
                }
            }
        }
    }

    public ConfigReposConfig getConfigRepos() {
        return goConfigService.getConfigForEditing().getConfigRepos();
    }

    public void deleteConfigRepo(String repoId, Username username, HttpLocalizedOperationResult result) {
        DeleteConfigRepoCommand command = new DeleteConfigRepoCommand(securityService, repoId, username, result);

        update(username, repoId, result, command);
        if (result.isSuccessful()) {
            result.setMessage(LocalizedMessage.string("RESOURCE_DELETE_SUCCESSFUL", "config repo", repoId));
        }
    }

    public void createConfigRepo(ConfigRepoConfig configRepo, Username username, HttpLocalizedOperationResult result) {
        Localizable.CurryableLocalizable actionFailed = LocalizedMessage.string("RESOURCE_ADD_FAILED", "Config repo");
        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, actionFailed, username, result);
        update(username, configRepo.getId(), result, command);
    }

    public void updateConfigRepo(String repoIdToUpdate, ConfigRepoConfig newConfigRepo, String md5, Username username, HttpLocalizedOperationResult result) {
        Localizable.CurryableLocalizable actionFailed = LocalizedMessage.string("RESOURCE_UPDATE_FAILED", "Config repo");
        UpdateConfigRepoCommand command = new UpdateConfigRepoCommand(securityService, entityHashingService, repoIdToUpdate, newConfigRepo, actionFailed, md5, username, result);

        update(username, newConfigRepo.getId(), result, command);
        if (result.isSuccessful()) {
            result.setMessage(LocalizedMessage.string("RESOURCE_UPDATE_SUCCESSFUL", "Config repo", repoIdToUpdate));
        }
    }
}
