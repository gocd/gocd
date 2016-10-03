/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

import static com.thoughtworks.go.config.update.PipelineConfigErrorCopier.copyErrors;

public class CreatePipelineConfigCommand implements EntityConfigUpdateCommand<PipelineConfig> {
    private final GoConfigService goConfigService;
    private final PipelineConfig pipelineConfig;
    private final Username currentUser;
    private final LocalizedOperationResult result;
    private final String groupName;
    public String group;
    private PipelineConfig preprocessedPipelineConfig;

    public CreatePipelineConfigCommand(GoConfigService goConfigService, PipelineConfig pipelineConfig, Username currentUser, LocalizedOperationResult result, final String groupName) {
        this.goConfigService = goConfigService;
        this.pipelineConfig = pipelineConfig;
        this.currentUser = currentUser;
        this.result = result;
        this.groupName = groupName;
    }

    @Override
    public void update(CruiseConfig cruiseConfig) throws Exception {
        cruiseConfig.addPipelineWithoutValidation(groupName, pipelineConfig);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedPipelineConfig = preprocessedConfig.getPipelineConfigByName(pipelineConfig.name());
        boolean isValid = preprocessedPipelineConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, groupName, preprocessedConfig, preprocessedPipelineConfig))
                && preprocessedPipelineConfig.getAllErrors().isEmpty();

        if (!isValid) {
            copyErrors(preprocessedPipelineConfig, pipelineConfig);
        }
        return isValid;
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(pipelineConfig);
    }

    @Override
    public PipelineConfig getPreprocessedEntityConfig() {
        return preprocessedPipelineConfig;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        if (goConfigService.groups().hasGroup(groupName) && !goConfigService.isUserAdminOfGroup(currentUser.getUsername(), groupName)) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_GROUP", groupName), HealthStateType.unauthorised());
            return false;
        }
        return true;
    }
}
