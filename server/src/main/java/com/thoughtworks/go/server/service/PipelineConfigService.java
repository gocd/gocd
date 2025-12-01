/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.update.*;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.service.tasks.PluggableTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.thoughtworks.go.config.CaseInsensitiveString.str;
import static com.thoughtworks.go.i18n.LocalizedMessage.entityConfigValidationFailed;
import static com.thoughtworks.go.i18n.LocalizedMessage.saveFailedWithReason;

/**
 * Understands providing services around a pipeline configuration
 */
@Service
public class PipelineConfigService {
    private final GoConfigService goConfigService;
    private final SecurityService securityService;
    private final PluggableTaskService pluggableTaskService;
    private final EntityHashingService entityHashingService;
    private final ExternalArtifactsService externalArtifactsService;
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineConfigService.class);

    @Autowired
    public PipelineConfigService(GoConfigService goConfigService,
                                 SecurityService securityService,
                                 PluggableTaskService pluggableTaskService,
                                 EntityHashingService entityHashingService,
                                 ExternalArtifactsService externalArtifactsService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.pluggableTaskService = pluggableTaskService;
        this.entityHashingService = entityHashingService;
        this.externalArtifactsService = externalArtifactsService;
    }

    public PipelineConfig pipelineConfigNamed(String pipelineName) {
        return goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName));
    }

    public PipelineConfig getPipelineConfig(String pipelineName) {
        return goConfigService.getMergedConfigForEditing().getPipelineConfigByName(new CaseInsensitiveString(pipelineName));
    }

    private void update(Username currentUser,
                        PipelineConfig pipelineConfig,
                        LocalizedOperationResult result,
                        EntityConfigUpdateCommand<?> command) {
        try {
            goConfigService.updateConfig(command, currentUser);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException) {
                if (!result.hasMessage()) {
                    result.unprocessableEntity(entityConfigValidationFailed(pipelineConfig.getClass().getAnnotation(ConfigTag.class).value(), str(pipelineConfig.name()), e.getMessage()));
                }
            } else if (!(e instanceof ConfigUpdateCheckFailedException)) {
                LOGGER.error(e.getMessage(), e);
                result.internalServerError(saveFailedWithReason(e.getMessage()));
            }
        }
    }

    public void updatePipelineConfig(final Username currentUser,
                                     final PipelineConfig pipelineConfig,
                                     String updatedGroupName,
                                     final String md5,
                                     final LocalizedOperationResult result) {
        validatePluggableTasks(pipelineConfig);
        UpdatePipelineConfigCommand updatePipelineConfigCommand = new UpdatePipelineConfigCommand(goConfigService, entityHashingService, pipelineConfig, updatedGroupName, currentUser, md5, result, externalArtifactsService);
        update(currentUser, pipelineConfig, result, updatePipelineConfigCommand);
    }

    public PipelineGroups viewableGroupsFor(Username username) {
        return groupsMatchingFilter(goConfigService.cruiseConfig(), pipelineConfigs -> securityService.hasViewPermissionForGroup(str(username.getUsername()), pipelineConfigs.getGroup()));
    }

    public PipelineGroups adminGroupsForIncludingConfigRepos(Username username) {
        return groupsMatchingFilter(goConfigService.cruiseConfig(), pipelineConfigs -> securityService.isUserAdminOfGroup(username.getUsername(), pipelineConfigs.getGroup()));
    }

    public PipelineGroups viewableOrOperatableGroupsForIncludingConfigRepos(Username username) {
        return groupsMatchingFilter(goConfigService.cruiseConfig(), pipelineConfigs -> securityService.hasOperatePermissionForGroup(username.getUsername(), pipelineConfigs.getGroup()));
    }

    public PipelineGroups viewableGroupsForUserIncludingConfigRepos(Username username) {
        return groupsMatchingFilter(goConfigService.getMergedConfigForEditing(), pipelineConfigs -> securityService.hasViewPermissionForGroup(str(username.getUsername()), pipelineConfigs.getGroup()));
    }

    private PipelineGroups groupsMatchingFilter(CruiseConfig cruiseConfig, Predicate<PipelineConfigs> predicate) {
        return cruiseConfig
                .getGroups()
                .stream()
                .filter(predicate)
                .collect(Collectors.toCollection(PipelineGroups::new));
    }

    public void createPipelineConfig(final Username currentUser,
                                     final PipelineConfig pipelineConfig,
                                     final LocalizedOperationResult result,
                                     final String groupName) {
        CreatePipelineConfigCommand createPipelineConfigCommand = createPipelineConfigCommand(currentUser, pipelineConfig, result, groupName);
        update(currentUser, pipelineConfig, result, createPipelineConfigCommand);
    }

    public CreatePipelineConfigCommand createPipelineConfigCommand(Username currentUser,
                                                                   PipelineConfig pipelineConfig,
                                                                   LocalizedOperationResult result,
                                                                   String groupName) {
        validatePluggableTasks(pipelineConfig);
        return new CreatePipelineConfigCommand(goConfigService, pipelineConfig, currentUser, result, groupName, externalArtifactsService);
    }

    public void deletePipelineConfig(final Username currentUser,
                                     final PipelineConfig pipelineConfig,
                                     final LocalizedOperationResult result) {
        DeletePipelineConfigCommand deletePipelineConfigCommand = new DeletePipelineConfigCommand(goConfigService, pipelineConfig, currentUser, result);
        update(currentUser, pipelineConfig, result, deletePipelineConfigCommand);
        if (result.isSuccessful()) {
            result.setMessage(EntityType.Pipeline.deleteSuccessful(pipelineConfig.name()));
        }
    }

    private void validatePluggableTasks(PipelineConfig config) {
        for (PluggableTask task : StageConfig.allPluggableTasks(config.getStages())) {
            pluggableTaskService.isValid(task);
        }
    }

    public int totalPipelinesCount() {
        return goConfigService.getAllPipelineConfigs().size();
    }

    public void extractTemplateFromPipeline(String pipelineName,
                                            String templateName,
                                            Username currentUser) {
        goConfigService.updateConfig(new ExtractTemplateFromPipelineEntityConfigUpdateCommand(securityService, pipelineName, templateName, currentUser), currentUser);
    }
}
