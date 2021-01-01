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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.update.CreateTemplateConfigCommand;
import com.thoughtworks.go.config.update.DeleteTemplateConfigCommand;
import com.thoughtworks.go.config.update.UpdateTemplateAuthConfigCommand;
import com.thoughtworks.go.config.update.UpdateTemplateConfigCommand;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.service.tasks.PluggableTaskService;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.thoughtworks.go.i18n.LocalizedMessage.saveFailedWithReason;
import static com.thoughtworks.go.serverhealth.HealthStateScope.GLOBAL;
import static com.thoughtworks.go.serverhealth.HealthStateType.general;

@Service
public class TemplateConfigService {
    private final GoConfigService goConfigService;
    private final SecurityService securityService;
    private org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TemplateConfigService.class);
    private GoConfigCloner cloner = new GoConfigCloner();
    private EntityHashingService entityHashingService;
    private PluggableTaskService pluggableTaskService;
    private ExternalArtifactsService externalArtifactsService;

    @Autowired
    public TemplateConfigService(GoConfigService goConfigService, SecurityService securityService, EntityHashingService entityHashingService, PluggableTaskService pluggableTaskService, ExternalArtifactsService externalArtifactsService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.entityHashingService = entityHashingService;
        this.pluggableTaskService = pluggableTaskService;
        this.externalArtifactsService = externalArtifactsService;
    }

    public List<TemplateToPipelines> getTemplatesList(Username username) {
        List<TemplateToPipelines> templateToPipelinesForUser = new ArrayList<>();
        List<Role> roles = goConfigService.rolesForUser(username.getUsername());
        Map<CaseInsensitiveString, Map<CaseInsensitiveString, Authorization>> allTemplatesAssociatedWithPipelines = goConfigService.getCurrentConfig().templatesWithAssociatedPipelines();
        for (CaseInsensitiveString templateName : allTemplatesAssociatedWithPipelines.keySet()) {
            if (securityService.isAuthorizedToViewTemplate(templateName, username)) {
                Map<CaseInsensitiveString, Authorization> pipelinesWithAuthorization = allTemplatesAssociatedWithPipelines.get(templateName);
                TemplateToPipelines templateToPipelines = new TemplateToPipelines(templateName, securityService.isAuthorizedToEditTemplate(templateName, username), securityService.isUserAdmin(username));
                templateToPipelinesForUser.add(templateToPipelines);
                for (CaseInsensitiveString pipelineName : pipelinesWithAuthorization.keySet()) {
                    templateToPipelines.add(new PipelineEditabilityInfo(pipelineName, canAuthorizedTemplateUserEditPipeline(username, roles, pipelinesWithAuthorization.get(pipelineName)), goConfigService.isPipelineEditable(pipelineName)));
                }
            }
        }
        return templateToPipelinesForUser;
    }

    private boolean canAuthorizedTemplateUserEditPipeline(Username username, List<Role> roles, Authorization pipelineAuthorization) {
        return securityService.isUserAdmin(username) || pipelineAuthorization.isUserAnAdmin(username.getUsername(), roles);
    }

    public void removeTemplate(String templateName, CruiseConfig cruiseConfig, String md5, HttpLocalizedOperationResult result) {
        if (!doesTemplateExist(templateName, cruiseConfig, result)) {
            return;
        }
        goConfigService.updateConfig(new DeleteTemplateCommand(templateName, md5));
    }

    public void createTemplateConfig(final Username currentUser, final PipelineTemplateConfig templateConfig, final LocalizedOperationResult result) {
        validatePluggableTasks(templateConfig);
        CreateTemplateConfigCommand command = new CreateTemplateConfigCommand(templateConfig, currentUser, securityService, result, externalArtifactsService);
        update(currentUser, result, command, templateConfig);
    }

    public void updateTemplateConfig(final Username currentUser, final PipelineTemplateConfig templateConfig, final LocalizedOperationResult result, String md5) {
        validatePluggableTasks(templateConfig);
        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(templateConfig, currentUser, securityService, result, md5, entityHashingService, externalArtifactsService);
        update(currentUser, result, command, templateConfig);
    }

    public void updateTemplateAuthConfig(final Username currentUser, final PipelineTemplateConfig templateConfig, final Authorization authorization, final LocalizedOperationResult result, String md5) {
        UpdateTemplateAuthConfigCommand command = new UpdateTemplateAuthConfigCommand(templateConfig, authorization, currentUser, securityService, result, md5, entityHashingService, externalArtifactsService);
        update(currentUser, result, command, templateConfig);
    }

    public void deleteTemplateConfig(final Username currentUser, final PipelineTemplateConfig templateConfig, final LocalizedOperationResult result) {
        DeleteTemplateConfigCommand command = new DeleteTemplateConfigCommand(templateConfig, result, securityService, currentUser, externalArtifactsService);
        update(currentUser, result, command, templateConfig);
        if (result.isSuccessful()) {
            result.setMessage(EntityType.Template.deleteSuccessful(templateConfig.name().toString()));
        }
    }

    private void update(Username currentUser, LocalizedOperationResult result, EntityConfigUpdateCommand command, PipelineTemplateConfig templateConfig) {
        try {
            goConfigService.updateConfig(command, currentUser);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException) {
                result.unprocessableEntity(EntityType.Template.entityConfigValidationFailed(templateConfig.name(), e.getMessage()));
            } else {
                if (!result.hasMessage()) {
                    LOGGER.error(e.getMessage(), e);
                    result.internalServerError(saveFailedWithReason("An error occurred while saving the template config. Please check the logs for more information."));
                }
            }
        }
    }

    private void validatePluggableTasks(PipelineTemplateConfig templateConfig) {
        for (PluggableTask task : getPluggableTask(templateConfig)) {
            pluggableTaskService.isValid(task);
        }
    }

    private List<PluggableTask> getPluggableTask(PipelineTemplateConfig templateConfig) {
        List<PluggableTask> pluggableTasks = new ArrayList<>();
        for (StageConfig stage : templateConfig.getStages()) {
            for (JobConfig job : stage.getJobs()) {
                for (Task task : job.getTasks()) {
                    if (task instanceof PluggableTask) {
                        pluggableTasks.add((PluggableTask) task);
                    }
                }
            }
        }
        return pluggableTasks;
    }

    public PipelineTemplateConfig loadForView(String templateName, HttpLocalizedOperationResult result) {
        return findTemplate(templateName, result, goConfigService.getConfigHolder());
    }

    private boolean doesTemplateExist(String templateName, CruiseConfig cruiseConfig, HttpLocalizedOperationResult result) {
        TemplatesConfig templates = cruiseConfig.getTemplates();
        if (!templates.hasTemplateNamed(new CaseInsensitiveString(templateName))) {
            result.notFound(EntityType.Template.notFoundMessage(templateName), general(GLOBAL));
            return false;
        }
        return true;
    }

    public List<PipelineConfig> allPipelinesNotUsingTemplates(Username username, LocalizedOperationResult result) {
        if (!(securityService.isUserAdmin(username) || securityService.isUserGroupAdmin(username))) {
            result.forbidden(LocalizedMessage.forbiddenToEdit(), HealthStateType.forbidden());
            return null;
        }
        List<PipelineConfig> allPipelineConfigs = goConfigService.getAllPipelineConfigsForEditForUser(username);
        List<PipelineConfig> allPipelinesNotUsingTemplates = new ArrayList<>();
        for (PipelineConfig pipeline : allPipelineConfigs) {
            if (!pipeline.hasTemplate()) {
                allPipelinesNotUsingTemplates.add(pipeline);
            }
        }
        return allPipelinesNotUsingTemplates;
    }

    private PipelineTemplateConfig findTemplate(String templateName, HttpLocalizedOperationResult result, GoConfigHolder configHolder) {
        if (!doesTemplateExist(templateName, configHolder.configForEdit, result)) {
            return null;
        }
        return configHolder.configForEdit.findTemplate(new CaseInsensitiveString(templateName));
    }

    public TemplatesConfig templateConfigsThatCanBeEditedBy(Username username) {
        CruiseConfig cruiseConfig = goConfigService.cruiseConfig();

        return cruiseConfig.getTemplates()
                .stream()
                .filter(templateConfig -> {
                    return securityService.isAuthorizedToEditTemplate(templateConfig.name(),username);
                })
                .collect(Collectors.toCollection(TemplatesConfig::new));
    }

    public TemplatesConfig templateConfigsThatCanBeViewedBy(Username currentUsername) {
        CruiseConfig cruiseConfig = goConfigService.cruiseConfig();

        return cruiseConfig.getTemplates()
                .stream()
                .filter(templateConfig -> {
                    return securityService.isAuthorizedToViewTemplate(templateConfig.name(),currentUsername);
                })
                .collect(Collectors.toCollection(TemplatesConfig::new));
    }
}
