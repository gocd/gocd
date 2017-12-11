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

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.update.CreateTemplateConfigCommand;
import com.thoughtworks.go.config.update.DeleteTemplateConfigCommand;
import com.thoughtworks.go.config.update.ExtractTemplateFromPipelineCommand;
import com.thoughtworks.go.config.update.UpdateTemplateConfigCommand;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.presentation.ConfigForEdit;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.service.tasks.PluggableTaskService;
import com.thoughtworks.go.server.ui.TemplatesViewModel;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TemplateConfigService {
    private final GoConfigService goConfigService;
    private final SecurityService securityService;
    private org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TemplateConfigService.class);
    private Cloner cloner = new Cloner();
    private EntityHashingService entityHashingService;
    private PluggableTaskService pluggableTaskService;

    @Autowired
    public TemplateConfigService(GoConfigService goConfigService, SecurityService securityService, EntityHashingService entityHashingService, PluggableTaskService pluggableTaskService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.entityHashingService = entityHashingService;
        this.pluggableTaskService = pluggableTaskService;
    }

    public Map<CaseInsensitiveString, List<CaseInsensitiveString>> templatesWithPipelinesForUser(CaseInsensitiveString username) {
        HashMap<CaseInsensitiveString, List<CaseInsensitiveString>> templatesToPipelinesMap = new HashMap<>();
        Map<CaseInsensitiveString, Map<CaseInsensitiveString, Authorization>> authMap = goConfigService.getCurrentConfig().templatesWithAssociatedPipelines();
        for (CaseInsensitiveString templateName : authMap.keySet()) {
            if (securityService.isAuthorizedToViewTemplate(templateName, new Username(username))) {
                templatesToPipelinesMap.put(templateName, new ArrayList<>());
                Map<CaseInsensitiveString, Authorization> authorizationMap = authMap.get(templateName);
                for (CaseInsensitiveString pipelineName : authorizationMap.keySet()) {
                    templatesToPipelinesMap.get(templateName).add(pipelineName);
                }
            }

        }
        return templatesToPipelinesMap;
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
        CreateTemplateConfigCommand command = new CreateTemplateConfigCommand(templateConfig, currentUser, securityService, result);
        update(currentUser, result, command, templateConfig);
    }

    public void extractFromPipeline(PipelineTemplateConfig template, String pipelineToExtractFrom, Username currentUser, HttpLocalizedOperationResult result){
        ExtractTemplateFromPipelineCommand command = new ExtractTemplateFromPipelineCommand(template, pipelineToExtractFrom, currentUser, goConfigService, result);
        update(currentUser, result, command, template);
    }

    public void updateTemplateConfig(final Username currentUser, final PipelineTemplateConfig templateConfig, final LocalizedOperationResult result, String md5) {
        validatePluggableTasks(templateConfig);
        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(templateConfig, currentUser, securityService, result, md5, entityHashingService);
        update(currentUser, result, command, templateConfig);
    }

    public void deleteTemplateConfig(final Username currentUser, final PipelineTemplateConfig templateConfig, final LocalizedOperationResult result) {
        DeleteTemplateConfigCommand command = new DeleteTemplateConfigCommand(templateConfig, result, securityService, currentUser);
        update(currentUser, result, command, templateConfig);
        if (result.isSuccessful()) {
            result.setMessage(LocalizedMessage.string("RESOURCE_DELETE_SUCCESSFUL", "template", templateConfig.name().toString()));
        }
    }

    private void update(Username currentUser, LocalizedOperationResult result, EntityConfigUpdateCommand command, PipelineTemplateConfig template) {
        try {
            goConfigService.updateConfig(command, currentUser);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException) {
                result.unprocessableEntity(LocalizedMessage.string("ENTITY_CONFIG_VALIDATION_FAILED", "template", template, e.getMessage()));
            } else {
                if (!result.hasMessage()) {
                    LOGGER.error(e.getMessage(), e);
                    result.internalServerError(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", "An error occurred while saving the template config. Please check the logs for more information."));
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

    public ConfigForEdit<PipelineTemplateConfig> loadForEdit(String templateName, Username username, HttpLocalizedOperationResult result) {
        if (!securityService.isAuthorizedToEditTemplate(new CaseInsensitiveString(templateName), username)) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_TEMPLATE", templateName), HealthStateType.unauthorised());
            return null;
        }
        GoConfigHolder configHolder = goConfigService.getConfigHolder();
        configHolder = cloner.deepClone(configHolder);
        PipelineTemplateConfig template = findTemplate(templateName, result, configHolder);
        return template != null ? new ConfigForEdit<>(template, configHolder) : null;
    }

    public PipelineTemplateConfig loadForView(String templateName, HttpLocalizedOperationResult result) {
        return findTemplate(templateName, result, goConfigService.getConfigHolder());
    }


    private boolean doesTemplateExist(String templateName, CruiseConfig cruiseConfig, HttpLocalizedOperationResult result) {
        TemplatesConfig templates = cruiseConfig.getTemplates();
        if (!templates.hasTemplateNamed(new CaseInsensitiveString(templateName))) {
            result.notFound(LocalizedMessage.string("RESOURCE_NOT_FOUND", "Template", templateName), HealthStateType.general(HealthStateScope.GLOBAL));
            return false;
        }
        return true;
    }

    public List<PipelineConfig> allPipelinesNotUsingTemplates(Username username, LocalizedOperationResult result) {
        if (!(securityService.isUserAdmin(username) || securityService.isUserGroupAdmin(username))) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_ADMINISTER"), HealthStateType.unauthorised());
            return null;
        }
        List<PipelineConfig> allPipelineConfigs = goConfigService.getAllPipelineConfigsForEdit();
        List<PipelineConfig> allPipelinesNotUsingTemplates = new ArrayList<>();
        for (PipelineConfig pipeline : allPipelineConfigs) {
            if (!pipeline.hasTemplate()) {
                allPipelinesNotUsingTemplates.add(pipeline);
            }
        }
        return allPipelinesNotUsingTemplates;
    }

    public List<TemplatesViewModel> getTemplateViewModels(CaseInsensitiveString username) {
        List<TemplatesViewModel> templatesViewModels = new ArrayList<>();
        CruiseConfig cruiseConfig = goConfigService.cruiseConfig();
        for (PipelineTemplateConfig templateConfig : cruiseConfig.getTemplates()) {
            boolean authorizedToViewTemplate = cruiseConfig.isAuthorizedToViewTemplate(templateConfig, username);
            boolean authorizedToEditTemplate = cruiseConfig.isAuthorizedToEditTemplate(templateConfig, username);
            templatesViewModels.add(new TemplatesViewModel(templateConfig, authorizedToViewTemplate, authorizedToEditTemplate));
        }
        return templatesViewModels;
    }

    private PipelineTemplateConfig findTemplate(String templateName, HttpLocalizedOperationResult result, GoConfigHolder configHolder) {
        if (!doesTemplateExist(templateName, configHolder.configForEdit, result)) {
            return null;
        }
        return configHolder.configForEdit.findTemplate(new CaseInsensitiveString(templateName));
    }
}
