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
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.ExternalArtifactsService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import static com.thoughtworks.go.config.update.PipelineConfigErrorCopier.copyErrors;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;

public class UpdatePipelineConfigCommand extends PipelineConfigCommand {
    private final EntityHashingService entityHashingService;
    private final String newGroupName;
    private final Username currentUser;
    private final String digest;
    private final LocalizedOperationResult result;
    public String existingGroupName;

    public UpdatePipelineConfigCommand(GoConfigService goConfigService, EntityHashingService entityHashingService, PipelineConfig pipelineConfig, String newGroupName,
                                       Username currentUser, String digest, LocalizedOperationResult result, ExternalArtifactsService externalArtifactsService) {
        super(pipelineConfig, goConfigService, externalArtifactsService);
        this.entityHashingService = entityHashingService;
        this.newGroupName = newGroupName;
        this.currentUser = currentUser;
        this.digest = digest;
        this.result = result;
    }

    private String getExistingPipelineGroupName() {
        if (existingGroupName == null) {
            this.existingGroupName = goConfigService.findGroupNameByPipeline(pipelineConfig.name());
        }
        return existingGroupName;
    }

    @Override
    public void update(CruiseConfig cruiseConfig) {
        cruiseConfig.update(getExistingPipelineGroupName(), pipelineConfig.name().toString(), pipelineConfig);
        if (!existingGroupName.equalsIgnoreCase(newGroupName)) {
            PipelineConfigs group = cruiseConfig.findGroup(this.existingGroupName);
            group.remove(pipelineConfig);
            cruiseConfig.getGroups().addPipeline(newGroupName, pipelineConfig);
        }
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedPipelineConfig = preprocessedConfig.getPipelineConfigByName(pipelineConfig.name());
        PipelineConfigSaveValidationContext validationContext = PipelineConfigSaveValidationContext.forChain(false, getExistingPipelineGroupName(), preprocessedConfig, preprocessedPipelineConfig);
        validatePublishAndFetchExternalConfigs(preprocessedPipelineConfig, preprocessedConfig);
        boolean isValid = preprocessedPipelineConfig.validateTree(validationContext)
                && preprocessedPipelineConfig.getAllErrors().isEmpty();
        if (!isValid) {
            copyErrors(preprocessedPipelineConfig, pipelineConfig);
        }
        return isValid;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return canEditPipeline() && canAccessGroups() && isRequestFresh(cruiseConfig);
    }

    private boolean canAccessGroups() {
        if (!existingGroupName.equalsIgnoreCase(newGroupName) && goConfigService.groups().hasGroup(newGroupName)) {
            if (!goConfigService.isUserAdminOfGroup(currentUser.getUsername(), newGroupName)) {
                result.forbidden(EntityType.PipelineGroup.forbiddenToEdit(newGroupName, currentUser.getUsername()), forbidden());
                return false;
            }
        }

        return true;
    }

    private boolean canEditPipeline() {
        if (!goConfigService.canEditPipeline(pipelineConfig.name().toString(), currentUser, result, getExistingPipelineGroupName())) {
            result.forbidden(EntityType.Pipeline.forbiddenToEdit(pipelineConfig.name().toString(), currentUser.getUsername()), forbidden());
            return false;
        }

        return true;
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        boolean freshRequest = entityHashingService.hashForEntity(cruiseConfig.getPipelineConfigByName(pipelineConfig.name()), getExistingPipelineGroupName()).equals(digest);

        if (!freshRequest) {
            result.stale(EntityType.Pipeline.staleConfig(pipelineConfig.name()));
        }

        return freshRequest;
    }
}

