/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigHolder;
import com.thoughtworks.go.config.DeleteTemplateCommand;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.config.TemplatesConfig;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.presentation.ConfigForEdit;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TemplateConfigService {
    private final GoConfigService goConfigService;
    private final SecurityService securityService;
    private Cloner cloner = new Cloner();

    @Autowired
    public TemplateConfigService(GoConfigService goConfigService, SecurityService securityService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
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

    public ConfigForEdit<PipelineTemplateConfig> loadForEdit(String templateName, Username username, HttpLocalizedOperationResult result) {
        if (!securityService.isAuthorizedToEditTemplate(templateName, username)) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_TEMPLATE", templateName), HealthStateType.unauthorised());
            return null;
        }
        GoConfigHolder configHolder = goConfigService.getConfigHolder();
        configHolder = cloner.deepClone(configHolder);
        PipelineTemplateConfig template = findTemplate(templateName, result, configHolder);
        return template != null ? new ConfigForEdit<PipelineTemplateConfig>(template, configHolder) : null;
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
        List<PipelineConfig> allPipelinesNotUsingTemplates = new ArrayList<PipelineConfig>();
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
