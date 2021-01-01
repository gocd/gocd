/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
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
    public void update(CruiseConfig cruiseConfig) {
        preprocessedPipelineConfig = cruiseConfig.getPipelineConfigByName(pipelineConfig.name());
        cruiseConfig.deletePipeline(pipelineConfig);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        for (PipelineConfig pipeline : preprocessedConfig.getAllPipelineConfigs()) {
            if(pipeline.materialConfigs().hasDependencyMaterial(pipelineConfig)){
                this.result.unprocessableEntity("Cannot delete pipeline '" + pipelineConfig.name() + "' as pipeline '" + String.format("%s (%s)", pipeline.name(), pipeline.getOriginDisplayName()) + "' depends on it");
                return false;
            }
        }

        for (EnvironmentConfig environment : preprocessedConfig.getEnvironments()) {
            if(environment.getPipelineNames().contains(pipelineConfig.name())){
                this.result.unprocessableEntity("Cannot delete pipeline '" + pipelineConfig.name() + "' as it is present in environment '" + environment.name() + "'.");
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
            result.forbidden(EntityType.Pipeline.forbiddenToDelete(pipelineConfig.getName(), currentUser.getUsername()), HealthStateType.forbidden());
            return false;
        }
        return true;
    }
}

