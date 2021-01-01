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

import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;

public class StageManualTriggerChecker implements SchedulingChecker {

    private final String pipelineName;
    private final Integer pipelineCounter;
    private final String stageName;
    private final SchedulingCheckerService schedulingCheckerService;
    private final PipelineService pipelineService;

    public StageManualTriggerChecker(String pipelineName, Integer pipelineCounter, String stageName, SchedulingCheckerService schedulingCheckerService, PipelineService pipelineService) {
        this.pipelineName = pipelineName;
        this.pipelineCounter = pipelineCounter;
        this.stageName = stageName;
        this.schedulingCheckerService = schedulingCheckerService;
        this.pipelineService = pipelineService;
    }

    @Override
    public void check(OperationResult result) {
        HealthStateType healthStateType = HealthStateType.general(HealthStateScope.forPipeline(pipelineName));
        Pipeline pipeline = pipelineService.fullPipelineByCounter(pipelineName, pipelineCounter);
        ScheduleStageResult scheduleStageResult = schedulingCheckerService.shouldAllowSchedulingStage(pipeline, stageName);
        if (scheduleStageResult == ScheduleStageResult.PreviousStageNotPassed) {
            String errorMessage = String.format("Cannot schedule %s as the previous stage %s has %s!", stageName, scheduleStageResult.getPreviousStageName(), scheduleStageResult.getPreviousStageResult());

            healthStateType = HealthStateType.forbidden();
            result.notAcceptable(errorMessage, healthStateType);
            return;
        }
        result.success(healthStateType);
    }
}
