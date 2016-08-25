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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.config.TemplatesConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;


public abstract class TemplateConfigCommand implements EntityConfigUpdateCommand<PipelineTemplateConfig> {

    private PipelineTemplateConfig preprocessedTemplateConfig;
    protected final LocalizedOperationResult result;
    protected PipelineTemplateConfig templateConfig;


    public TemplateConfigCommand(PipelineTemplateConfig templateConfig, LocalizedOperationResult result) {
        this.templateConfig = templateConfig;
        this.result = result;
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        TemplatesConfig templatesConfig = preprocessedConfig.getTemplates();
        preprocessedTemplateConfig = findAddedTemplate(preprocessedConfig);
        preprocessedTemplateConfig.validate(null);
        if(preprocessedTemplateConfig.getAllErrors().isEmpty()) {
            templatesConfig.validate(null);
            BasicCruiseConfig.copyErrors(preprocessedTemplateConfig, templateConfig);
            return preprocessedTemplateConfig.getAllErrors().isEmpty() && templatesConfig.errors().isEmpty();
        }
        BasicCruiseConfig.copyErrors(preprocessedTemplateConfig, templateConfig);
        return false;
    }

    protected PipelineTemplateConfig findAddedTemplate(CruiseConfig cruiseConfig) {
        try {
            PipelineTemplateConfig pipelineTemplateConfig = cruiseConfig.findTemplate(templateConfig.name());
            if(pipelineTemplateConfig == null) {
                result.notFound(LocalizedMessage.string("RESOURCE_NOT_FOUND"), HealthStateType.notFound());
                throw new NullPointerException(String.format("The template with name '%s' is not found or should not be null.", templateConfig.name()));
            }
            return pipelineTemplateConfig;
        }catch (NullPointerException e) {
            throw new NullPointerException(String.format("The template with name '%s' is not found or should not be null.", templateConfig.name()));
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
