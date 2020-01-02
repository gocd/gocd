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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.thoughtworks.go.server.service.ScheduleStageResult.*;
import static org.mockito.Mockito.*;


public class StageManualTriggerCheckerTest {

    private SchedulingCheckerService schedulingCheckerService;
    private PipelineService pipelineService;
    private Pipeline pipeline;
    private String pipelineName;
    private StageManualTriggerChecker checker;
    private String stageName;
    private OperationResult result;
    private int pipelineCounter;

    @BeforeEach
    void setUp() throws Exception {
        schedulingCheckerService = mock(SchedulingCheckerService.class);
        pipelineService = mock(PipelineService.class);
        pipeline = mock(Pipeline.class);
        pipelineName = "cruise";
        stageName = "dev";
        pipelineCounter = 1;
        checker = new StageManualTriggerChecker(pipelineName, pipelineCounter, stageName, schedulingCheckerService, pipelineService);
        result = mock(OperationResult.class);

        when(pipelineService.fullPipelineByCounter(pipelineName, pipelineCounter)).thenReturn(pipeline);
        when(pipeline.hasStageBeenRun(stageName)).thenReturn(false);
    }

    @Test
    void shouldNotThrowExceptionIfNoEntryInDB() {
        when(pipelineService.fullPipelineByCounter(pipelineName, pipelineCounter)).thenReturn(null);
        when(schedulingCheckerService.shouldAllowSchedulingStage(null, stageName)).thenReturn(PipelineNotFound);
        checker.check(result);
        verify(result).success(any(HealthStateType.class));
    }

    @ParameterizedTest
    @MethodSource("testInputs")
    void shouldReturnSuccessIfSchedulingServiceReturnsAnythingOtherThanStageFailed(ScheduleStageResult scheduleStageResult) {
        when(schedulingCheckerService.shouldAllowSchedulingStage(null, stageName)).thenReturn(scheduleStageResult);
        checker.check(result);
        verify(result).success(any(HealthStateType.class));
    }

    @Test
    void shouldReturnNotAcceptableIfThePreviousStageFailed() {
        when(schedulingCheckerService.shouldAllowSchedulingStage(pipeline, stageName)).thenReturn(PreviousStageNotPassed.setPreviousStageValues("build", "Failed"));
        checker.check(result);
        verify(result).notAcceptable(eq("Cannot schedule dev as the previous stage build has Failed!"), any(HealthStateType.class));
    }

    static Stream<Arguments> testInputs() {
        return Stream.of(
                Arguments.of(CanSchedule),
                Arguments.of(PipelineNotFound),
                Arguments.of(PreviousStageNotRan)
        );
    }
}
