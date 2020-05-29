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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.config.TemplatesConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.ExternalArtifactsService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;


public class UpdateTemplateConfigCommand extends TemplateConfigCommand {
    private SecurityService securityService;
    private String digest;
    private EntityHashingService entityHashingService;

    public UpdateTemplateConfigCommand(PipelineTemplateConfig templateConfig,
                                       Username currentUser,
                                       SecurityService securityService,
                                       LocalizedOperationResult result,
                                       String digest,
                                       EntityHashingService entityHashingService,
                                       ExternalArtifactsService externalArtifactsService) {
        super(templateConfig, result, currentUser, externalArtifactsService);
        this.securityService = securityService;
        this.digest = digest;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public void update(CruiseConfig modifiedConfig) {
        PipelineTemplateConfig existingTemplateConfig = findAddedTemplate(modifiedConfig);
        templateConfig.setAuthorization(existingTemplateConfig.getAuthorization());
        TemplatesConfig templatesConfig = modifiedConfig.getTemplates();
        templatesConfig.removeTemplateNamed(existingTemplateConfig.name());
        templatesConfig.add(templateConfig);
        modifiedConfig.setTemplates(templatesConfig);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedTemplateConfig = findAddedTemplate(preprocessedConfig);

        if (!preprocessedConfig.getAllErrors().isEmpty()) {
            templateConfig.errors().addAll(preprocessedConfig.getAllErrors().get(0));
            return false;
        }

        validatePublishAndFetchExternalConfigs(preprocessedTemplateConfig, preprocessedConfig);
        return super.isValid(preprocessedConfig, false);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isRequestFresh(cruiseConfig) && isUserAuthorized();
    }

    private boolean isUserAuthorized() {
        if (!securityService.isAuthorizedToEditTemplate(templateConfig.name(), currentUser)) {
            result.forbidden(EntityType.Template.forbiddenToEdit(templateConfig.name(), currentUser.getUsername()), forbidden());
            return false;
        }
        return true;
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        PipelineTemplateConfig pipelineTemplateConfig = findAddedTemplate(cruiseConfig);
        boolean freshRequest = entityHashingService.hashForEntity(pipelineTemplateConfig).equals(digest);
        if (!freshRequest) {
            result.stale(EntityType.Template.staleConfig(templateConfig.name()));
        }
        return freshRequest;
    }
}

