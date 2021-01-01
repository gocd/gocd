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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.ExternalArtifactsService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import static com.thoughtworks.go.config.update.PipelineConfigErrorCopier.copyErrors;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;

public class CreatePipelineConfigCommand extends PipelineConfigCommand {
    private final Username currentUser;
    private final LocalizedOperationResult result;
    private final String groupName;
    public String group;

    public CreatePipelineConfigCommand(GoConfigService goConfigService, PipelineConfig pipelineConfig, Username currentUser, LocalizedOperationResult result, final String groupName, ExternalArtifactsService externalArtifactsService) {
        super(pipelineConfig, goConfigService, externalArtifactsService);
        this.currentUser = currentUser;
        this.result = result;
        this.groupName = groupName;
    }

    @Override
    public void update(CruiseConfig cruiseConfig) {
        cruiseConfig.addPipelineWithoutValidation(groupName, pipelineConfig);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedPipelineConfig = preprocessedConfig.getPipelineConfigByName(pipelineConfig.name());
        PipelineConfigSaveValidationContext validationContext = PipelineConfigSaveValidationContext.forChain(true, groupName, preprocessedConfig, preprocessedPipelineConfig);
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
        if (goConfigService.groups().hasGroup(groupName) && !goConfigService.isUserAdminOfGroup(currentUser.getUsername(), groupName)) {
            result.forbidden(EntityType.PipelineGroup.forbiddenToEdit(groupName, currentUser.getUsername()), forbidden());
            return false;
        }
        return true;
    }
}
