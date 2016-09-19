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
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import static com.thoughtworks.go.config.update.PipelineConfigErrorCopier.copyErrors;

public class UpdatePipelineConfigCommand implements EntityConfigUpdateCommand<PipelineConfig> {
    private final GoConfigService goConfigService;
    private final EntityHashingService entityHashingService;
    private final PipelineConfig pipelineConfig;
    private final Username currentUser;
    private final String md5;
    private final LocalizedOperationResult result;
    public String group;
    private PipelineConfig preprocessedPipelineConfig;

    public UpdatePipelineConfigCommand(GoConfigService goConfigService, EntityHashingService entityHashingService, PipelineConfig pipelineConfig,
                                       Username currentUser, String md5, LocalizedOperationResult result) {
        this.goConfigService = goConfigService;
        this.entityHashingService = entityHashingService;
        this.pipelineConfig = pipelineConfig;
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
    public void update(CruiseConfig cruiseConfig) throws Exception {
        cruiseConfig.update(getPipelineGroup(), pipelineConfig.name().toString(), pipelineConfig);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedPipelineConfig = preprocessedConfig.getPipelineConfigByName(pipelineConfig.name());
        boolean isValid = preprocessedPipelineConfig.validateTree(PipelineConfigSaveValidationContext.forChain(false, getPipelineGroup(), preprocessedConfig, preprocessedPipelineConfig));
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
        return canEditPipeline() && isRequestFresh(cruiseConfig);
    }

    private boolean canEditPipeline() {
        return goConfigService.canEditPipeline(pipelineConfig.name().toString(), currentUser, result, getPipelineGroup());
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        boolean freshRequest = entityHashingService.md5ForEntity(cruiseConfig.getPipelineConfigByName(pipelineConfig.name())).equals(md5);

        if (!freshRequest) {
            result.stale(LocalizedMessage.string("STALE_RESOURCE_CONFIG", "pipeline", pipelineConfig.name().toString()));
        }

        return freshRequest;
    }
}

