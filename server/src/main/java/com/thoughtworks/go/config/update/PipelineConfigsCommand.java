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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.apache.commons.lang3.StringUtils;

import static com.thoughtworks.go.i18n.LocalizedMessage.*;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;
import static com.thoughtworks.go.serverhealth.HealthStateType.notFound;

public abstract class PipelineConfigsCommand implements EntityConfigUpdateCommand<PipelineConfigs> {
    protected final LocalizedOperationResult result;
    protected final Username currentUser;
    protected final SecurityService securityService;
    protected PipelineConfigs preprocessedPipelineConfigs;

    public PipelineConfigsCommand(LocalizedOperationResult result, Username currentUser, SecurityService securityService) {
        this.result = result;
        this.currentUser = currentUser;
        this.securityService = securityService;
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(preprocessedPipelineConfigs);
    }

    @Override
    public PipelineConfigs getPreprocessedEntityConfig() {
        return preprocessedPipelineConfigs;
    }

    protected boolean isUserAdminOfGroup(String groupName) {
        if (!securityService.isUserAdminOfGroup(currentUser, groupName)) {
            result.forbidden(EntityType.PipelineGroup.forbiddenToEdit(groupName, currentUser.getUsername()), forbidden());
            return false;
        }
        return true;
    }

    protected boolean isUserAdmin() {
        if (!securityService.isUserAdmin(currentUser)) {
            result.forbidden(forbiddenToEdit(), forbidden());
            return false;
        }
        return true;
    }

    protected PipelineConfigs findPipelineConfigs(CruiseConfig cruiseConfig, String group) {
        validateGroupName(group);
        PipelineConfigs existingPipelineConfigs = cruiseConfig.findGroup(group);
        if (existingPipelineConfigs == null) {
            result.notFound(EntityType.PipelineGroup.notFoundMessage(group), notFound());
            throw new RecordNotFoundException(EntityType.PipelineGroup, group);
        }
        return existingPipelineConfigs;
    }

    protected void validateGroupName(String group) {
        if (group == null || StringUtils.isBlank(group)) {
            result.unprocessableEntity("The group is invalid. Attribute 'name' cannot be null.");
            throw new IllegalArgumentException("Group name cannot be null.");
        }
    }
}
