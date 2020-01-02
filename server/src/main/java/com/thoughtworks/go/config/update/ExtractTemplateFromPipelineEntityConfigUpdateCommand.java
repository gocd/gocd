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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.ConflictException;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;

public class ExtractTemplateFromPipelineEntityConfigUpdateCommand implements EntityConfigUpdateCommand<PipelineConfig> {

    private final SecurityService securityService;
    private final String pipelineName;
    private final PipelineTemplateConfig newTemplate;
    private final Username currentUser;

    private PipelineConfig preprocessedPipelineConfig;

    public ExtractTemplateFromPipelineEntityConfigUpdateCommand(SecurityService securityService,
                                                                String pipelineName,
                                                                String templateName,
                                                                Username currentUser) {
        this.securityService = securityService;
        this.pipelineName = pipelineName;
        this.newTemplate = new PipelineTemplateConfig(new CaseInsensitiveString(templateName));
        this.currentUser = currentUser;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        // set authorization on new template
        if (securityService.isUserGroupAdmin(currentUser)) {
            newTemplate.setAuthorization(new Authorization(new AdminsConfig(new AdminUser(currentUser.getUsername()))));
        }
        // set the stages on the new template
        preprocessedPipelineConfig = preprocessedConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        newTemplate.copyStages(preprocessedPipelineConfig);
        // change the pointer on the pipeline
        preprocessedPipelineConfig.templatize(this.newTemplate.name());

        // add newly created template
        preprocessedConfig.getTemplates().add(this.newTemplate);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return true;
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(preprocessedPipelineConfig);
    }

    @Override
    public PipelineConfig getPreprocessedEntityConfig() {
        return preprocessedPipelineConfig;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        PipelineConfig pipelineConfig = cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString(pipelineName));
        if (pipelineConfig == null) {
            throw new RecordNotFoundException(EntityType.Pipeline, pipelineName);
        }

        if (pipelineConfig.hasTemplate()) {
            throw new ConflictException(EntityType.Pipeline.alreadyUsesATemplate(pipelineName));
        }

        PipelineTemplateConfig templateByName = cruiseConfig.getTemplates().templateByName(this.newTemplate.name());
        if (templateByName != null) {
            throw new ConflictException(EntityType.Template.alreadyExists(this.newTemplate.name()));
        }

        if (new NameTypeValidator().isNameInvalid(this.newTemplate.name().toString())) {
            throw new BadRequestException(NameTypeValidator.errorMessage("template", this.newTemplate.name()));
        }

        return true;
    }
}
