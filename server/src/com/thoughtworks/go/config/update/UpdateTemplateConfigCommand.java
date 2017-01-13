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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

import java.util.ArrayList;
import java.util.List;

public class UpdateTemplateConfigCommand extends TemplateConfigCommand {
    private final PipelineTemplateConfig newTemplateConfig;
    private PipelineTemplateConfig existingTemplateConfig;
    private String md5;
    private EntityHashingService entityHashingService;

    public UpdateTemplateConfigCommand(PipelineTemplateConfig templateConfig, Username currentUser, GoConfigService goConfigService, LocalizedOperationResult result, String md5, EntityHashingService entityHashingService) {
        super(templateConfig, result, currentUser, goConfigService);
        this.newTemplateConfig = templateConfig;
        this.md5 = md5;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public void update(CruiseConfig modifiedConfig) throws Exception {
        this.existingTemplateConfig = findAddedTemplate(modifiedConfig);
        templateConfig.setAuthorization(existingTemplateConfig.getAuthorization());
        TemplatesConfig templatesConfig = modifiedConfig.getTemplates();
        templatesConfig.removeTemplateNamed(existingTemplateConfig.name());
        templatesConfig.add(templateConfig);
        modifiedConfig.setTemplates(templatesConfig);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        boolean isValid = validateStageNameUpdate(preprocessedConfig);
        return isValid && super.isValid(preprocessedConfig, false);
    }

    private boolean validateStageNameUpdate(CruiseConfig preprocessedConfig) {
        ArrayList<CaseInsensitiveString> updatedStageNames = getUpdatedStageNames();
        if (updatedStageNames.isEmpty()) {
            return true;
        }
        ArrayList<String> pipelinesUsingCurrentTemplate = getPipelinesUsingCurrentTemplate(preprocessedConfig);

        for (String pipeline : pipelinesUsingCurrentTemplate) {
            PipelineConfig dependencyPipeline = preprocessedConfig.findPipelineUsingThisPipelineAsADependency(pipeline);
            if (dependencyPipeline != null) {
                DependencyMaterialConfig material = dependencyPipeline.materialConfigs().findDependencyMaterial(new CaseInsensitiveString(pipeline));
                if(templateConfig.findBy(material.getStageName()) == null){
                    String error = String.format("Can not update stage name as it is used as a material `%s` in pipeline `%s`", material.getPipelineStageName(), dependencyPipeline.name());
                    newTemplateConfig.addError("Stage Name", error);
                    return false;
                }
            }
        }
        return true;
    }

    private ArrayList<String> getPipelinesUsingCurrentTemplate(CruiseConfig preprocessedConfig) {
        List<PipelineConfig> allPipelines = preprocessedConfig.allPipelines();
        ArrayList<String> pipelinesUsingCurrentTemplate = new ArrayList<>();
        for (PipelineConfig pipeline : allPipelines) {
            boolean isFromTemplate = pipeline.isCreatedFromTemplate(existingTemplateConfig.name());
            if (isFromTemplate) {
                pipelinesUsingCurrentTemplate.add(pipeline.name().toString());
            }
        }
        return pipelinesUsingCurrentTemplate;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isUserAuthorized() && isRequestFresh(cruiseConfig);
    }

    private boolean isUserAuthorized() {
        if (!goConfigService.isAuthorizedToEditTemplate(templateConfig.name().toString(), currentUser)) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT"), HealthStateType.unauthorised());
            return false;
        }
        return true;
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        PipelineTemplateConfig pipelineTemplateConfig = findAddedTemplate(cruiseConfig);
        boolean freshRequest = entityHashingService.md5ForEntity(pipelineTemplateConfig).equals(md5);
        if (!freshRequest) {
            result.stale(LocalizedMessage.string("STALE_RESOURCE_CONFIG", "Template", templateConfig.name()));
        }

        return freshRequest;
    }

    public ArrayList<CaseInsensitiveString> getUpdatedStageNames() {
        ArrayList<CaseInsensitiveString> modifiedStages = new ArrayList<>();
        for (StageConfig stageConfig : existingTemplateConfig.getStages()) {
            CaseInsensitiveString name = stageConfig.name();
            if (newTemplateConfig.getStage(name) == null) {
                modifiedStages.add(name);
            }
        }
        return modifiedStages;
    }
}

