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
import com.thoughtworks.go.config.ErrorCollector;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import java.util.stream.StreamSupport;

public class UpdatePipelineConfigsCommand extends PipelineConfigsCommand {
    private final PipelineConfigs oldPipelineGroup;
    private final PipelineConfigs newPipelineGroup;
    private final String md5;
    private final EntityHashingService entityHashingService;

    public UpdatePipelineConfigsCommand(PipelineConfigs oldPipelineGroup, PipelineConfigs newPipelineGroup, LocalizedOperationResult result, Username currentUser, String md5,
                                        EntityHashingService entityHashingService, SecurityService securityService) {
        super(result, currentUser, securityService);
        this.oldPipelineGroup = oldPipelineGroup;
        this.newPipelineGroup = newPipelineGroup;
        this.md5 = md5;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        preprocessedPipelineConfigs = findPipelineConfigs(preprocessedConfig, oldPipelineGroup.getGroup());
        preprocessedPipelineConfigs.setGroup(newPipelineGroup.getGroup());
        preprocessedPipelineConfigs.setAuthorization(newPipelineGroup.getAuthorization());
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedPipelineConfigs = findPipelineConfigs(preprocessedConfig, newPipelineGroup.getGroup());
        preprocessedPipelineConfigs.getAuthorization().validateTree(new DelegatingValidationContext(ConfigSaveValidationContext.forChain(preprocessedConfig, preprocessedPipelineConfigs)) {
            @Override
            public boolean shouldNotCheckRole() {
                return false;
            }
        });

        if (!ErrorCollector.getAllErrors(preprocessedPipelineConfigs).isEmpty()) {
            BasicCruiseConfig.copyErrors(preprocessedPipelineConfigs, newPipelineGroup);
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isRequestFresh(cruiseConfig) && isUserAdminOfGroup(oldPipelineGroup.getGroup()) && isValidRenameRequest();
    }

    private boolean isValidRenameRequest() {
        if (isRenameAttempt(oldPipelineGroup, newPipelineGroup) && doesPipelineGroupContainsRemotePipelines(oldPipelineGroup)) {
            result.unprocessableEntity(String.format("Can not rename pipeline group '%s' as it contains remote pipelines.", oldPipelineGroup.getGroup()));
        }

        return result.isSuccessful();
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        PipelineConfigs existingPipelineConfigs = findPipelineConfigs(cruiseConfig, oldPipelineGroup.getGroup());
        boolean freshRequest = entityHashingService.md5ForEntity(existingPipelineConfigs).equals(md5);
        if (!freshRequest) {
            result.stale(EntityType.PipelineGroup.staleConfig(oldPipelineGroup.getGroup()));
        }
        return freshRequest;
    }

    private boolean isRenameAttempt(PipelineConfigs fromServer, PipelineConfigs fromRequest) {
        return !fromServer.getGroup().equals(fromRequest.getGroup());
    }

    private boolean doesPipelineGroupContainsRemotePipelines(PipelineConfigs pipelineGroup) {
        return StreamSupport.stream(pipelineGroup.spliterator(), false).anyMatch(PipelineConfig::isConfigDefinedRemotely);
    }
}
