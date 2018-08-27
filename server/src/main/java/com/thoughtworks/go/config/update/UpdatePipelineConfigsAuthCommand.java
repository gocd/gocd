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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.PipelineGroupNotFoundException;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.apache.commons.lang.StringUtils;

import static com.thoughtworks.go.i18n.LocalizedMessage.forbiddenToEdit;
import static com.thoughtworks.go.i18n.LocalizedMessage.resourceNotFound;
import static com.thoughtworks.go.i18n.LocalizedMessage.staleResourceConfig;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;
import static com.thoughtworks.go.serverhealth.HealthStateType.notFound;

public class UpdatePipelineConfigsAuthCommand implements EntityConfigUpdateCommand<PipelineConfigs> {
    private PipelineConfigs preprocessedPipelineConfigs;
    private final LocalizedOperationResult result;
    private final Authorization authorization;
    private final String group;
    private final Username currentUser;
    private final String md5;
    private final EntityHashingService entityHashingService;
    private final SecurityService securityService;

    public UpdatePipelineConfigsAuthCommand(String group, Authorization authorization, LocalizedOperationResult result, Username currentUser, String md5,
                                            EntityHashingService entityHashingService, SecurityService securityService) {
        this.group = group;
        this.result = result;
        this.authorization = authorization;
        this.currentUser = currentUser;
        this.md5 = md5;
        this.entityHashingService = entityHashingService;
        this.securityService = securityService;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        preprocessedPipelineConfigs = findPipelineConfigs(preprocessedConfig);
        preprocessedPipelineConfigs.setAuthorization(authorization);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedPipelineConfigs = findPipelineConfigs(preprocessedConfig);
        authorization.validateTree(new DelegatingValidationContext(ConfigSaveValidationContext.forChain(preprocessedConfig, preprocessedPipelineConfigs)) {
            @Override
            public boolean shouldNotCheckRole() {
                return false;
            }
        });

        if (!authorization.getAllErrors().isEmpty()) {
            BasicCruiseConfig.copyErrors(authorization, preprocessedPipelineConfigs.getAuthorization());
            return false;
        }
        return true;
    }

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
        return isRequestFresh(cruiseConfig) && isUserAuthorized();
    }

    private boolean isUserAuthorized() {
        if (!securityService.isUserAdminOfGroup(currentUser, group)) {
            result.forbidden(forbiddenToEdit(), forbidden());
            return false;
        }
        return true;
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        PipelineConfigs existingPipelineConfigs = findPipelineConfigs(cruiseConfig);
        boolean freshRequest = entityHashingService.md5ForEntity(existingPipelineConfigs).equals(md5);
        if (!freshRequest) {
            result.stale(staleResourceConfig("Group", group));
        }
        return freshRequest;
    }

    private PipelineConfigs findPipelineConfigs(CruiseConfig cruiseConfig) {
        if (group == null || StringUtils.isBlank(group)) {
            result.unprocessableEntity("The group is invalid. Attribute 'name' cannot be null.");
            throw new IllegalArgumentException("Group name cannot be null.");
        } else {
            PipelineConfigs existingPipelineConfigs = cruiseConfig.findGroup(group);
            if (existingPipelineConfigs == null) {
                result.notFound(resourceNotFound("Group", group), notFound());
                throw new PipelineGroupNotFoundException();
            }
            return existingPipelineConfigs;
        }
    }

}
