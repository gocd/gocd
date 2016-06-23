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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

public class DeletePipelineConfigCommand implements EntityConfigUpdateCommand<PipelineConfig> {
    private final GoConfigService goConfigService;
    private PipelineConfig pipelineConfig;
    private final Username currentUser;
    private final LocalizedOperationResult result;
    private PipelineConfig preprocessedPipelineConfig;

    public DeletePipelineConfigCommand(GoConfigService goConfigService, PipelineConfig pipelineConfig, Username currentUser, LocalizedOperationResult result) {
        this.goConfigService = goConfigService;
        this.pipelineConfig = pipelineConfig;
        this.currentUser = currentUser;
        this.result = result;
    }


    @Override
    public void update(CruiseConfig cruiseConfig) throws Exception {
        preprocessedPipelineConfig = cruiseConfig.getPipelineConfigByName(pipelineConfig.name());
        cruiseConfig.deletePipeline(pipelineConfig);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        for (PipelineConfig pipeline : preprocessedConfig.getAllPipelineConfigs()) {
            if(pipeline.materialConfigs().hasDependencyMaterial(pipelineConfig)){
                Localizable.CurryableLocalizable message = LocalizedMessage.string("CANNOT_DELETE_PIPELINE_USED_AS_MATERIALS", pipelineConfig.name(), String.format("%s (%s)", pipeline.name(), pipeline.getOriginDisplayName()));
                this.result.unprocessableEntity(message);
                return false;
            }
        }

        for (EnvironmentConfig environment : preprocessedConfig.getEnvironments()) {
            if(environment.getPipelineNames().contains(pipelineConfig.name())){
                Localizable.CurryableLocalizable message = LocalizedMessage.string("CANNOT_DELETE_PIPELINE_IN_ENVIRONMENT", pipelineConfig.name(), environment.name());
                this.result.unprocessableEntity(message);
                return false;
            }
        }

        return true;
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
        String groupName = goConfigService.findGroupNameByPipeline(pipelineConfig.name());
        if (goConfigService.groups().hasGroup(groupName) && !goConfigService.isUserAdminOfGroup(currentUser.getUsername(), groupName)) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_DELETE_PIPELINE", groupName), HealthStateType.unauthorised());
            return false;
        }
        return true;
    }
}

