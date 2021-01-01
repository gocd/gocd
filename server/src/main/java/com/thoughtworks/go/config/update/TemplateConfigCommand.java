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
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.ExternalArtifactsService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import java.util.List;

import static com.thoughtworks.go.config.exceptions.EntityType.Template;
import static com.thoughtworks.go.serverhealth.HealthStateType.notFound;


public abstract class TemplateConfigCommand implements EntityConfigUpdateCommand<PipelineTemplateConfig> {

    PipelineTemplateConfig preprocessedTemplateConfig;
    protected final LocalizedOperationResult result;
    protected PipelineTemplateConfig templateConfig;
    protected final Username currentUser;
    protected ExternalArtifactsService externalArtifactsService;


    TemplateConfigCommand(PipelineTemplateConfig templateConfig, LocalizedOperationResult result, Username currentUser, ExternalArtifactsService externalArtifactsService) {
        this.templateConfig = templateConfig;
        this.result = result;
        this.currentUser = currentUser;
        this.externalArtifactsService = externalArtifactsService;
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

    @Override
    public void encrypt(CruiseConfig preprocessedConfig) {
        preprocessedTemplateConfig = findAddedTemplate(preprocessedConfig);
        templateConfig.encryptSecureProperties(preprocessedConfig, preprocessedTemplateConfig);
    }

    PipelineTemplateConfig findAddedTemplate(CruiseConfig cruiseConfig) {
        if (templateConfig == null || templateConfig.name() == null || templateConfig.name().isBlank()) {
            result.unprocessableEntity("The template config is invalid. Attribute 'name' cannot be null.");
            throw new IllegalArgumentException("Template name cannot be null.");
        } else {
            PipelineTemplateConfig pipelineTemplateConfig = cruiseConfig.findTemplate(templateConfig.name());
            if (pipelineTemplateConfig == null) {
                result.notFound(Template.notFoundMessage(templateConfig.name()), notFound());
                throw new RecordNotFoundException(Template, templateConfig.name());
            }
            return pipelineTemplateConfig;
        }
    }

    void validatePublishAndFetchExternalConfigs(PipelineTemplateConfig pipelineTemplateConfig, CruiseConfig preprocessedConfig) {
        List<PipelineConfig> associatedPipelineConfigs = preprocessedConfig.pipelineConfigsAssociatedWithTemplate(templateConfig.name());
        for (PipelineConfig associatedPipelineConfig : associatedPipelineConfigs) {
            for (PluggableArtifactConfig pluggableArtifactConfig : associatedPipelineConfig.getExternalArtifactConfigs()) {
                externalArtifactsService.validateExternalArtifactConfig(pluggableArtifactConfig, preprocessedConfig.getArtifactStores().find(pluggableArtifactConfig.getStoreId()), true);
                if (!pluggableArtifactConfig.getAllErrors().isEmpty()) {
                    pipelineTemplateConfig.addError("pipeline", String.format("Error validating publish config on associated pipeline `%s`: %s", associatedPipelineConfig.name(), pluggableArtifactConfig.getAllErrors()));
                }
            }

            for (FetchPluggableArtifactTask fetchPluggableArtifactTask : associatedPipelineConfig.getFetchExternalArtifactTasks()) {
                externalArtifactsService.validateFetchExternalArtifactTask(fetchPluggableArtifactTask, pipelineTemplateConfig, preprocessedConfig);
                if (!fetchPluggableArtifactTask.getAllErrors().isEmpty()) {
                    pipelineTemplateConfig.addError("pipeline", String.format("Error validating publish config on associated pipeline `%s`: %s", associatedPipelineConfig.name(), fetchPluggableArtifactTask.getAllErrors()));
                }
            }
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
