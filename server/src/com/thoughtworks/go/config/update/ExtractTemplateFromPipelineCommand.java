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

import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

public class ExtractTemplateFromPipelineCommand extends TemplateConfigCommand {
    private final PipelineTemplateConfig templateConfig;
    private final String pipelineToExtractFrom;
    private final Username currentUser;
    private final GoConfigService goConfigService;
    private final HttpLocalizedOperationResult result;

    public ExtractTemplateFromPipelineCommand(PipelineTemplateConfig templateConfig, String pipelineToExtractFrom, Username currentUser, GoConfigService goConfigService, HttpLocalizedOperationResult result) {
        super(templateConfig, result, currentUser, goConfigService);
        this.templateConfig = templateConfig;
        this.pipelineToExtractFrom = pipelineToExtractFrom;
        this.currentUser = currentUser;
        this.goConfigService = goConfigService;
        this.result = result;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        if (canExtractTemplateFromPipeline(preprocessedConfig)) {
            PipelineConfig pipelineConfig = preprocessedConfig.find(pipelineToExtractFrom);
            templateConfig.copyStages(pipelineConfig);
            preprocessedConfig.getTemplates().add(templateConfig);
            preprocessedConfig.makePipelineUseTemplate(pipelineConfig.name(), templateConfig.name());
        }
    }

    private boolean canExtractTemplateFromPipeline(CruiseConfig preprocessedConfig) {
        PipelineConfig pipelineConfig = preprocessedConfig.find(pipelineToExtractFrom);
        if (pipelineConfig == null) {
            result.unprocessableEntity(LocalizedMessage.string("PIPELINE_NOT_FOUND", pipelineToExtractFrom));
            return false;
        }
        if (!pipelineConfig.isLocal()) {
            result.unprocessableEntity(LocalizedMessage.string("CANNOT_EXTRACT_TEMPLATE_FROM_REMOTE"));
            return false;
        }
        if (pipelineConfig.hasTemplate()) {
            result.unprocessableEntity(LocalizedMessage.string("CANNOT_EXTRACT_TEMPLATE"));
            return false;
        }
        return true;
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        if (result.isSuccessful()) {
            boolean isValid = super.isValid(preprocessedConfig, true);
            PipelineConfig templatePipeline = preprocessedConfig.find(pipelineToExtractFrom);
            templatePipeline.validate(ConfigSaveValidationContext.forChain(preprocessedConfig));
            return isValid && templatePipeline.errors().isEmpty();

        }
        return false;
    }
}
