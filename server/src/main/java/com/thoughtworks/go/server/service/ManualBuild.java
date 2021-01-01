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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @understands operations related to a pipeline that was forced
 */
public class ManualBuild implements BuildType {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManualBuild.class);

    private Username username;

    public ManualBuild(Username username) {
        this.username = username;
    }

    @Override
    public BuildCause onModifications(MaterialRevisions materialRevisions, boolean materialConfigurationChanged, MaterialRevisions previousMaterialRevisions) {
        if (materialRevisions == null || materialRevisions.isEmpty()) {
            throw new RuntimeException("Cannot find modifications, please check your SCM setting or environment.");
        }
        return BuildCause.createManualForced(materialRevisions, username);
    }

    @Override
    public BuildCause onEmptyModifications(PipelineConfig pipelineConfig, MaterialRevisions materialRevisions) {
        return onModifications(materialRevisions, false, null);
    }

    @Override
    public void canProduce(PipelineConfig pipelineConfig, SchedulingCheckerService schedulingChecker,
                           ServerHealthService serverHealthService,
                           OperationResult operationResult) {
        schedulingChecker.canTriggerManualPipeline(pipelineConfig, CaseInsensitiveString.str(username.getUsername()), operationResult);
        if (!operationResult.canContinue()) {
            ServerHealthState serverHealthState = operationResult.getServerHealthState();
            LOGGER.info("'{}'  because '{}'", serverHealthState.getMessage(), serverHealthState.getDescription());
            serverHealthService.update(serverHealthState);
        }
    }

    @Override
    public boolean isValidBuildCause(PipelineConfig pipelineConfig, BuildCause buildCause) {
        return true;
    }

    @Override
    public boolean shouldCheckWhetherOlderRunsHaveRunWithLatestMaterials() {
        return true;
    }

    @Override
    public void notifyPipelineNotScheduled(PipelineConfig pipelineConfig) {
    }
}
