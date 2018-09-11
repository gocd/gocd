/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.PipelineGroupNotEmptyException;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import static com.thoughtworks.go.i18n.LocalizedMessage.forbiddenToEdit;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;

public class DeletePipelineConfigsCommand implements EntityConfigUpdateCommand<PipelineConfigs> {
    private PipelineConfigs preprocessedPipelineConfigs;
    private final LocalizedOperationResult result;
    private final PipelineConfigs group;
    private final Username currentUser;
    private final SecurityService securityService;

    public DeletePipelineConfigsCommand(PipelineConfigs group, LocalizedOperationResult result, Username currentUser,
                                        SecurityService securityService) {
        this.group = group;
        this.result = result;
        this.currentUser = currentUser;
        this.securityService = securityService;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        preprocessedPipelineConfigs = group;
        try {
            preprocessedConfig.deletePipelineGroup(group.getGroup());
        } catch (PipelineGroupNotEmptyException e) {
            result.unprocessableEntity(e.getMessage());
        }
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) { return true; }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(preprocessedPipelineConfigs);
    }

    @Override
    public PipelineConfigs getPreprocessedEntityConfig() {
        return preprocessedPipelineConfigs;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        if (!securityService.isUserAdminOfGroup(currentUser, group.getGroup())) {
            result.forbidden(forbiddenToEdit(), forbidden());
            return false;
        }
        return true;
    }
}
