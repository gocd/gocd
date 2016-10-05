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

package com.thoughtworks.go.server.service;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.CreateTemplateConfigCommand;
import com.thoughtworks.go.config.update.DeleteTemplateConfigCommand;
import com.thoughtworks.go.config.update.UpdateTemplateConfigCommand;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.presentation.ConfigForEdit;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TemplateConfigService {
    private final GoConfigService goConfigService;
    private final SecurityService securityService;
    private org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TemplateConfigService.class);
    private Cloner cloner = new Cloner();
    private EntityHashingService entityHashingService;

    @Autowired
    public TemplateConfigService(GoConfigService goConfigService, SecurityService securityService, EntityHashingService entityHashingService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.entityHashingService = entityHashingService;
    }

    public Map<CaseInsensitiveString, List<CaseInsensitiveString>> templatesWithPipelinesForUser(String username) {
        return goConfigService.getCurrentConfig().templatesWithPipelinesForUser(username);
    }

    public void removeTemplate(String templateName, CruiseConfig cruiseConfig, String md5, HttpLocalizedOperationResult result) {
        if (!doesTemplateExist(templateName, cruiseConfig, result)) {
            return;
        }
        goConfigService.updateConfig(new DeleteTemplateCommand(templateName, md5));
    }

    public void createTemplateConfig(final Username currentUser, final PipelineTemplateConfig templateConfig, final LocalizedOperationResult result) {
        CreateTemplateConfigCommand command = new CreateTemplateConfigCommand(templateConfig, currentUser, goConfigService, result);
        update(currentUser, result, command, templateConfig);
    }

    public void updateTemplateConfig(final Username currentUser, final PipelineTemplateConfig templateConfig, final LocalizedOperationResult result, String md5) {
        UpdateTemplateConfigCommand command = new UpdateTemplateConfigCommand(templateConfig, currentUser, goConfigService, result, md5, entityHashingService);
        update(currentUser, result, command, templateConfig);
    }

    public void deleteTemplateConfig(final Username currentUser, final PipelineTemplateConfig templateConfig, final LocalizedOperationResult result) {
        DeleteTemplateConfigCommand command = new DeleteTemplateConfigCommand(templateConfig, result, goConfigService, currentUser);
        update(currentUser, result, command, templateConfig);
        if(result.isSuccessful()) {
            result.setMessage(LocalizedMessage.string("RESOURCE_DELETE_SUCCESSFUL", "template", templateConfig.name().toString()));
        }
    }

    private void update(Username currentUser, LocalizedOperationResult result, EntityConfigUpdateCommand command, PipelineTemplateConfig templateConfig) {
        try {
            goConfigService.updateConfig(command, currentUser);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException) {
                result.unprocessableEntity(LocalizedMessage.string("ENTITY_CONFIG_VALIDATION_FAILED", templateConfig.getClass().getAnnotation(ConfigTag.class).value(), templateConfig.name(), e.getMessage()));
            } else {
                if (!result.hasMessage()) {
                    LOGGER.error(e.getMessage(), e);
                    result.internalServerError(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", "An error occurred while saving the template config. Please check the logs for more information."));
                }
            }
        }
    }

    public ConfigForEdit<PipelineTemplateConfig> loadForEdit(String templateName, Username username, HttpLocalizedOperationResult result) {
        if (!securityService.isAuthorizedToEditTemplate(templateName, username)) {
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
            result.notFound(LocalizedMessage.string("TEMPLATE_NOT_FOUND", templateName), HealthStateType.general(HealthStateScope.GLOBAL));
            return false;
        }
        return true;
    }

    public List<PipelineConfig> allPipelinesNotUsingTemplates(Username username, LocalizedOperationResult result) {
        if (!securityService.isUserAdmin(username)) {
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


    private PipelineTemplateConfig findTemplate(String templateName, HttpLocalizedOperationResult result, GoConfigHolder configHolder) {
        if (!doesTemplateExist(templateName, configHolder.configForEdit, result)) {
            return null;
        }
        return configHolder.configForEdit.findTemplate(new CaseInsensitiveString(templateName));
    }
}
