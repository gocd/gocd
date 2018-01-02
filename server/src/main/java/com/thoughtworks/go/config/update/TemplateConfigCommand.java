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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.NoSuchTemplateException;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;


public abstract class TemplateConfigCommand implements EntityConfigUpdateCommand<PipelineTemplateConfig> {

    PipelineTemplateConfig preprocessedTemplateConfig;
    protected final LocalizedOperationResult result;
    protected PipelineTemplateConfig templateConfig;
    protected final Username currentUser;


    TemplateConfigCommand(PipelineTemplateConfig templateConfig, LocalizedOperationResult result, Username currentUser) {
        this.templateConfig = templateConfig;
        this.result = result;
        this.currentUser = currentUser;
    }


    protected boolean isValid(CruiseConfig preprocessedConfig, boolean isTemplateBeingCreated) {
        TemplatesConfig templatesConfig = preprocessedConfig.getTemplates();
        preprocessedTemplateConfig = findAddedTemplate(preprocessedConfig);
        preprocessedTemplateConfig.validateTree(ConfigSaveValidationContext.forChain(preprocessedConfig, templatesConfig), preprocessedConfig, isTemplateBeingCreated);
        if(preprocessedTemplateConfig.getAllErrors().isEmpty()) {
            templatesConfig.validate(null);
            BasicCruiseConfig.copyErrors(preprocessedTemplateConfig, templateConfig);
            return preprocessedTemplateConfig.getAllErrors().isEmpty() && templatesConfig.errors().isEmpty();
        }
        BasicCruiseConfig.copyErrors(preprocessedTemplateConfig, templateConfig);
        return false;
    }

    PipelineTemplateConfig findAddedTemplate(CruiseConfig cruiseConfig) {
        if (templateConfig == null || templateConfig.name() == null || templateConfig.name().isBlank()) {
            result.unprocessableEntity(LocalizedMessage.string("ENTITY_ATTRIBUTE_NULL", "template", "name"));
            throw new IllegalArgumentException("Template name cannot be null.");
        } else {
            PipelineTemplateConfig pipelineTemplateConfig = cruiseConfig.findTemplate(templateConfig.name());
            if (pipelineTemplateConfig == null) {
                result.notFound(LocalizedMessage.string("RESOURCE_NOT_FOUND", "Template", templateConfig.name()), HealthStateType.notFound());
                throw new NoSuchTemplateException(templateConfig.name());
            }
            return pipelineTemplateConfig;
        }
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(templateConfig);
    }

    @Override
    public PipelineTemplateConfig getPreprocessedEntityConfig() {
        return preprocessedTemplateConfig;
    }
}
