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
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class CreatePipelineConfigsCommand extends PipelineConfigsCommand {
    private final PipelineConfigs pipelineConfigs;

    public CreatePipelineConfigsCommand(PipelineConfigs pipelineConfigs, Username currentUser, LocalizedOperationResult result, SecurityService securityService) {
        super(result, currentUser, securityService);
        this.pipelineConfigs = pipelineConfigs;
    }

    @Override
    public void update(CruiseConfig cruiseConfig) {
        cruiseConfig.getGroups().add(pipelineConfigs);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedPipelineConfigs = pipelineConfigs;
        validateGroupName(pipelineConfigs.getGroup());
        Authorization authorization = preprocessedPipelineConfigs.getAuthorization();
        ConfigSaveValidationContext validationContext = ConfigSaveValidationContext.forChain(preprocessedConfig, preprocessedPipelineConfigs, authorization);

        preprocessedPipelineConfigs.validate(validationContext);
        boolean authorizationIsValid = true;
        if (authorization != null) {
            authorization.validateTree(new DelegatingValidationContext(validationContext) {
                @Override
                public boolean shouldNotCheckRole() {
                    return false;
                }
            });
            authorizationIsValid = authorization.getAllErrors().isEmpty();
        }

        return preprocessedPipelineConfigs.errors().getAll().isEmpty() && authorizationIsValid;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isUserAdmin();
    }
}
