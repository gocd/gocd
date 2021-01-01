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

import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;

public class StageActiveChecker implements SchedulingChecker {
    private String pipelineName;
    private String stageName;
    private StageService stageService;

    public StageActiveChecker(String pipelineName, String stageName, StageService stageService) {
        this.pipelineName = pipelineName;
        this.stageName = stageName;
        this.stageService = stageService;
    }

    @Override
    public void check(OperationResult result) {
        HealthStateType healthStateType = HealthStateType.general(HealthStateScope.forPipeline(pipelineName));
        if (stageService.isStageActive(pipelineName, stageName)) {
            String message = String.format("Failed to trigger pipeline [%s]", pipelineName);
            result.conflict(message, String.format("Stage [%s] in pipeline [%s] is still in progress", stageName, pipelineName), healthStateType);
        } else {
            result.success(healthStateType);
        }
    }
}
