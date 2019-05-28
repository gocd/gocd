/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class UpdatePipelineConfigsAuthCommand extends PipelineConfigsCommand {
    private final Authorization authorization;
    private final String group;
    private final String md5;
    private final EntityHashingService entityHashingService;

    public UpdatePipelineConfigsAuthCommand(String group, Authorization authorization, LocalizedOperationResult result, Username currentUser, String md5,
                                            EntityHashingService entityHashingService, SecurityService securityService) {
        super(result, currentUser, securityService);
        this.group = group;
        this.authorization = authorization;
        this.md5 = md5;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        preprocessedPipelineConfigs = findPipelineConfigs(preprocessedConfig, group);
        preprocessedPipelineConfigs.setAuthorization(authorization);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedPipelineConfigs = findPipelineConfigs(preprocessedConfig, group);
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
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isRequestFresh(cruiseConfig) && isUserAdminOfGroup(group);
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        PipelineConfigs existingPipelineConfigs = findPipelineConfigs(cruiseConfig, group);
        boolean freshRequest = entityHashingService.md5ForEntity(existingPipelineConfigs).equals(md5);
        if (!freshRequest) {
            result.stale(EntityType.PipelineGroup.staleConfig(group));
        }
        return freshRequest;
    }
}
