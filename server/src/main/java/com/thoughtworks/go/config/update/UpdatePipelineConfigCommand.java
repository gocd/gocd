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
import org.apache.commons.lang3.StringUtils;

import static com.thoughtworks.go.config.update.PipelineConfigErrorCopier.copyErrors;

public class UpdatePipelineConfigCommand extends PipelineConfigCommand {
    private final EntityHashingService entityHashingService;
    private final String updatedGroupName;
    private final Username currentUser;
    private final String md5;
    private final LocalizedOperationResult result;
    public String group;

    public UpdatePipelineConfigCommand(GoConfigService goConfigService, EntityHashingService entityHashingService, PipelineConfig pipelineConfig, String updatedGroupName,
                                       Username currentUser, String md5, LocalizedOperationResult result, ExternalArtifactsService externalArtifactsService) {
        super(pipelineConfig, goConfigService, externalArtifactsService);
        this.entityHashingService = entityHashingService;
        this.updatedGroupName = updatedGroupName;
        this.currentUser = currentUser;
        this.md5 = md5;
        this.result = result;
    }

    private String getPipelineGroup() {
        if (group == null) {
            this.group = goConfigService.findGroupNameByPipeline(pipelineConfig.name());
        }
        return group;
    }

    @Override
    public void update(CruiseConfig cruiseConfig) {
        cruiseConfig.update(getPipelineGroup(), pipelineConfig.name().toString(), pipelineConfig);
        if (!StringUtils.equalsIgnoreCase(group, updatedGroupName)) {
            PipelineConfigs group = cruiseConfig.findGroup(this.group);
            group.remove(pipelineConfig);
            cruiseConfig.getGroups().addPipeline(updatedGroupName, pipelineConfig);
        }
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedPipelineConfig = preprocessedConfig.getPipelineConfigByName(pipelineConfig.name());
        PipelineConfigSaveValidationContext validationContext = PipelineConfigSaveValidationContext.forChain(false, getPipelineGroup(), preprocessedConfig, preprocessedPipelineConfig);
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
        return canEditPipeline() && isRequestFresh(cruiseConfig);
    }

    private boolean canEditPipeline() {
        return goConfigService.canEditPipeline(pipelineConfig.name().toString(), currentUser, result, getPipelineGroup());
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        boolean freshRequest = entityHashingService.md5ForEntity(cruiseConfig.getPipelineConfigByName(pipelineConfig.name()), getPipelineGroup()).equals(md5);

        if (!freshRequest) {
            result.stale(EntityType.Pipeline.staleConfig(pipelineConfig.name()));
        }

        return freshRequest;
    }
}

