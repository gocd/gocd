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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.ExternalArtifactsService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class UpdateTemplateAuthConfigCommand extends UpdateTemplateConfigCommand {

    private final Authorization authorization;

    public UpdateTemplateAuthConfigCommand(PipelineTemplateConfig templateConfig, Authorization authorization, Username currentUser, SecurityService securityService, LocalizedOperationResult result, String md5, EntityHashingService entityHashingService, ExternalArtifactsService externalArtifactsService) {
        super(templateConfig, currentUser, securityService, result, md5, entityHashingService, externalArtifactsService);
        this.authorization = authorization;
    }

    @Override
    public void update(CruiseConfig modifiedConfig) {
        PipelineTemplateConfig existingTemplateConfig = findAddedTemplate(modifiedConfig);
        existingTemplateConfig.setAuthorization(authorization);
    }

    @Override
    public void encrypt(CruiseConfig preProcessedConfig) {
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        TemplatesConfig templates = preprocessedConfig.getTemplates();
        preprocessedTemplateConfig = findAddedTemplate(preprocessedConfig);
        preprocessedTemplateConfig.getAuthorization().validateTree(new DelegatingValidationContext(ConfigSaveValidationContext.forChain(preprocessedConfig, templates)) {
            @Override
            public boolean shouldNotCheckRole() {
                return false;
            }
        });

        if (!preprocessedTemplateConfig.getAuthorization().getAllErrors().isEmpty()) {
            BasicCruiseConfig.copyErrors(preprocessedTemplateConfig.getAuthorization(), authorization);
            return false;
        }
        return true;

    }
}
