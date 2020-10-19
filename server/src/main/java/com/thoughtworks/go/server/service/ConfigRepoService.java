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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.update.CreateConfigRepoCommand;
import com.thoughtworks.go.config.update.DeleteConfigRepoCommand;
import com.thoughtworks.go.config.update.UpdateConfigRepoCommand;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.service.materials.PackageDefinitionService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Predicate;

import static com.thoughtworks.go.i18n.LocalizedMessage.entityConfigValidationFailed;
import static com.thoughtworks.go.i18n.LocalizedMessage.saveFailedWithReason;
import static java.lang.String.format;

@Service
public class ConfigRepoService {
    public static final Logger LOGGER = LoggerFactory.getLogger(PackageDefinitionService.class);
    private final GoConfigService goConfigService;
    private final SecurityService securityService;
    private final EntityHashingService entityHashingService;
    private final ConfigRepoExtension configRepoExtension;

    @Autowired
    public ConfigRepoService(GoConfigService goConfigService, SecurityService securityService, EntityHashingService entityHashingService, ConfigRepoExtension configRepoExtension,
                             MaterialUpdateService materialUpdateService, MaterialConfigConverter converter) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.entityHashingService = entityHashingService;
        this.configRepoExtension = configRepoExtension;
        goConfigService.register(new EntityConfigChangedListener<ConfigRepoConfig>() {
            @Override
            public void onEntityConfigChange(ConfigRepoConfig entity) {
                if (getConfigRepo(entity.getId()) != null) {
                    materialUpdateService.updateMaterial(converter.toMaterial(entity.getRepo()));
                }
            }
        });
    }

    public PartialConfig partialConfigDefinedBy(ConfigRepoConfig repo) {
        return goConfigService.cruiseConfig().getPartials().parallelStream().
                filter(definedByRepo(repo)).findFirst().orElseThrow(
                () -> new RecordNotFoundException(format("Repository `%s` does not define any configurations", repo.getId()))
        );
    }

    private Predicate<PartialConfig> definedByRepo(ConfigRepoConfig repo) {
        return (part) -> part.getOrigin() instanceof RepoConfigOrigin &&
                ((RepoConfigOrigin) part.getOrigin()).getConfigRepo().equals(repo);
    }

    public GoConfigService getGoConfigService() {
        return goConfigService;
    }

    public ConfigRepoConfig getConfigRepo(String repoId) {
        return goConfigService.getConfigForEditing().getConfigRepos().getConfigRepo(repoId);
    }

    public ConfigReposConfig getConfigRepos() {
        return goConfigService.getConfigForEditing().getConfigRepos();
    }

    public void deleteConfigRepo(String repoId, Username username, HttpLocalizedOperationResult result) {
        DeleteConfigRepoCommand command = new DeleteConfigRepoCommand(securityService, repoId, username, result);

        update(username, repoId, result, command);
        if (result.isSuccessful()) {
            result.setMessage(EntityType.ConfigRepo.deleteSuccessful(repoId));
        }
    }

    public void createConfigRepo(ConfigRepoConfig configRepo, Username username, HttpLocalizedOperationResult result) {
        CreateConfigRepoCommand command = new CreateConfigRepoCommand(securityService, configRepo, username, result, configRepoExtension);
        update(username, configRepo.getId(), result, command);
    }

    public Boolean hasConfigRepoByFingerprint(String fingerprint) {
        return null != findByFingerprint(fingerprint);
    }

    public ConfigRepoConfig findByFingerprint(String fingerprint) {
        return getConfigRepos().stream()
                .filter(configRepo -> configRepo.getRepo().getFingerprint().equalsIgnoreCase(fingerprint))
                .findFirst().orElse(null);

    }

    public void updateConfigRepo(String repoIdToUpdate, ConfigRepoConfig newConfigRepo, String digestOfExistingConfigRepo,
                                 Username username, HttpLocalizedOperationResult result) {
        UpdateConfigRepoCommand command = new UpdateConfigRepoCommand(securityService, entityHashingService, repoIdToUpdate,
                newConfigRepo, digestOfExistingConfigRepo, username, result, configRepoExtension);

        update(username, newConfigRepo.getId(), result, command);
        if (result.isSuccessful()) {
            result.setMessage("The config repo '" + repoIdToUpdate + "' was updated successfully.");
        }
    }

    private void update(Username username, String repoId, HttpLocalizedOperationResult result, EntityConfigUpdateCommand<ConfigRepoConfig> command) {
        try {
            goConfigService.updateConfig(command, username);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException && !result.hasMessage()) {
                result.unprocessableEntity(entityConfigValidationFailed(ConfigRepoConfig.class.getAnnotation(ConfigTag.class).value(), repoId, e.getMessage()));
            } else {
                if (!result.hasMessage()) {
                    LOGGER.error(e.getMessage(), e);
                    result.internalServerError(saveFailedWithReason("An error occurred while saving the package config. Please check the logs for more information."));
                }
            }
        }
    }
}
