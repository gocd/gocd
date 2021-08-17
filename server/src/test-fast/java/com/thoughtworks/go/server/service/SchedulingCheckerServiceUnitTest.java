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

import com.thoughtworks.go.config.Approval;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.cronjob.GoDiskSpaceMonitor;
import com.thoughtworks.go.server.scheduling.TriggerMonitor;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SchedulingCheckerServiceUnitTest {

    private SchedulingCheckerService schedulingChecker;
    @Mock(lenient = true)
    private GoConfigService goConfigService;
    @Mock
    private CompositeChecker compositeChecker;
    @Mock(lenient = true)
    private OperationResult operationResult;
    @Captor
    private ArgumentCaptor<List<SchedulingChecker>> argumentCaptor;

    @BeforeEach
    public void setUp() throws Exception {
        schedulingChecker = spy(new SchedulingCheckerService(goConfigService, mock(StageService.class),
                mock(SecurityService.class), mock(PipelineLockService.class), mock(TriggerMonitor.class), mock(PipelineScheduleQueue.class), mock(PipelinePauseService.class), mock(PipelineService.class), new OutOfDiskSpaceChecker(mock(GoDiskSpaceMonitor.class))));

        lenient().doReturn(compositeChecker).when(schedulingChecker).buildScheduleCheckers(any());
        when(operationResult.getServerHealthState()).thenReturn(ServerHealthState.success(HealthStateType.general(HealthStateScope.GLOBAL)));
    }

    @Test
    public void shouldCheckIfScheduleCheckersCalledOnStageRerun() {
        schedulingChecker.canRerunStage(new PipelineIdentifier("pipeline_name", 1), "stage_name", "user", operationResult);

        verify(schedulingChecker).buildScheduleCheckers(argumentCaptor.capture());
        verify(compositeChecker).check(operationResult);

        assertFor(argumentCaptor.getValue(), StageAuthorizationChecker.class);
        assertFor(argumentCaptor.getValue(), PipelinePauseChecker.class);
        assertFor(argumentCaptor.getValue(), PipelineActiveChecker.class);
        assertFor(argumentCaptor.getValue(), StageActiveChecker.class);
        assertFor(argumentCaptor.getValue(), OutOfDiskSpaceChecker.class);
    }

    @Test
    public void shouldCheckIfScheduleCheckersCalledOnManuallyTrigger() {

        schedulingChecker.canManuallyTrigger(PipelineConfigMother.pipelineConfig("p1"), "user", operationResult);

        verify(schedulingChecker).buildScheduleCheckers(argumentCaptor.capture());
        verify(compositeChecker).check(operationResult);

        assertFor(argumentCaptor.getValue(), AboutToBeTriggeredChecker.class);
        assertFor(argumentCaptor.getValue(), StageAuthorizationChecker.class);
        assertFor(argumentCaptor.getValue(), PipelinePauseChecker.class);
        assertFor(argumentCaptor.getValue(), PipelineLockChecker.class);
        assertFor(argumentCaptor.getValue(), StageActiveChecker.class);
        assertFor(argumentCaptor.getValue(), OutOfDiskSpaceChecker.class);
    }

    @Test
    public void shouldCheckIfScheduleCheckersCalledOnScheduleStage() {
        schedulingChecker.canScheduleStage(new PipelineIdentifier("name", 1), "stage", "user", operationResult);

        verify(schedulingChecker).buildScheduleCheckers(argumentCaptor.capture());
        verify(compositeChecker).check(operationResult);

        assertFor(argumentCaptor.getValue(), StageAuthorizationChecker.class);
        assertFor(argumentCaptor.getValue(), StageLockChecker.class);
        assertFor(argumentCaptor.getValue(), PipelinePauseChecker.class);
        assertFor(argumentCaptor.getValue(), PipelineActiveChecker.class);
        assertFor(argumentCaptor.getValue(), StageActiveChecker.class);
        assertFor(argumentCaptor.getValue(), OutOfDiskSpaceChecker.class);
    }

    @Test
    public void shouldCheckIfScheduleCheckersCalledOnSchedule() {
        schedulingChecker.canSchedule(operationResult);

        verify(schedulingChecker).buildScheduleCheckers(argumentCaptor.capture());
        verify(compositeChecker).check(operationResult);

        assertFor(argumentCaptor.getValue(), OutOfDiskSpaceChecker.class);
    }

    @Test
    public void shouldCheckIfScheduleCheckersCalledOnCanTriggerPipelineWithTimer() {
        schedulingChecker.canTriggerPipelineWithTimer(PipelineConfigMother.pipelineConfig("sample"), operationResult);

        verify(schedulingChecker).buildScheduleCheckers(argumentCaptor.capture());
        verify(compositeChecker).check(operationResult);

        assertFor(argumentCaptor.getValue(), AboutToBeTriggeredChecker.class);
        assertFor(argumentCaptor.getValue(), PipelinePauseChecker.class);
        assertFor(argumentCaptor.getValue(), StageActiveChecker.class);
        assertFor(argumentCaptor.getValue(), PipelineLockChecker.class);
        assertFor(argumentCaptor.getValue(), OutOfDiskSpaceChecker.class);
    }

    @Test
    public void shouldCheckIfScheduleCheckersCalledOnTriggerManualPipeline() {
        schedulingChecker.canTriggerManualPipeline(PipelineConfigMother.pipelineConfig("sample"), "user", operationResult);

        verify(schedulingChecker).buildScheduleCheckers(argumentCaptor.capture());
        verify(compositeChecker).check(operationResult);

        assertFor(argumentCaptor.getValue(), AboutToBeTriggeredChecker.class);
        assertFor(argumentCaptor.getValue(), PipelinePauseChecker.class);
        assertFor(argumentCaptor.getValue(), StageActiveChecker.class);
        assertFor(argumentCaptor.getValue(), PipelineLockChecker.class);
        assertFor(argumentCaptor.getValue(), OutOfDiskSpaceChecker.class);
    }

    @Test
    public void shouldCheckIfScheduleCheckersCalledOnAutoTriggerProducer() {
        schedulingChecker.canAutoTriggerProducer(PipelineConfigMother.pipelineConfig("sample"), operationResult);

        verify(schedulingChecker).buildScheduleCheckers(argumentCaptor.capture());
        verify(compositeChecker).check(operationResult);

        assertFor(argumentCaptor.getValue(), PipelineLockChecker.class);
        assertFor(argumentCaptor.getValue(), ManualPipelineChecker.class);
        assertFor(argumentCaptor.getValue(), PipelinePauseChecker.class);
        assertFor(argumentCaptor.getValue(), StageActiveChecker.class);
    }

    @Test
    public void verifyScheduleCheckersCalledOnPipelineCanBeTriggeredManually() {
        schedulingChecker.pipelineCanBeTriggeredManually(PipelineConfigMother.pipelineConfig("sample"));

        verify(schedulingChecker).buildScheduleCheckers(argumentCaptor.capture());
        verify(compositeChecker).check(any());

        assertFor(argumentCaptor.getValue(), AboutToBeTriggeredChecker.class);
        assertFor(argumentCaptor.getValue(), PipelinePauseChecker.class);
        assertFor(argumentCaptor.getValue(), StageActiveChecker.class);
        assertFor(argumentCaptor.getValue(), OutOfDiskSpaceChecker.class);
    }

    @Nested
    class AllowSchedulingStage {
        private String stageName;
        private String pipelineName;
        private CaseInsensitiveString previousStageName;
        @Mock(lenient = true)
        private Pipeline pipeline;
        @Mock(lenient = true)
        private StageConfig previousStageConfig;
        @Mock(lenient = true)
        private StageConfig nextStageConfig;
        @Mock(lenient = true)
        private Approval approval;
        @Mock(lenient = true)
        private Stage previousStage;

        @BeforeEach
        void setUp() {
            pipelineName = "pipeline";
            stageName = "current_stage";
            Stages stages = mock(Stages.class);
            previousStageName = new CaseInsensitiveString("previous_stage");

            when(pipeline.getName()).thenReturn(pipelineName);
            when(pipeline.hasStageBeenRun(stageName)).thenReturn(false);
            when(goConfigService.hasPreviousStage(pipelineName, stageName)).thenReturn(true);

            when(goConfigService.previousStage(pipelineName, stageName)).thenReturn(previousStageConfig);
            when(previousStageConfig.name()).thenReturn(previousStageName);
            when(pipeline.hasStageBeenRun(previousStageName.toString())).thenReturn(true);
            when(goConfigService.nextStage(pipelineName, previousStageName.toString())).thenReturn(nextStageConfig);
            when(nextStageConfig.getApproval()).thenReturn(approval);

            when(pipeline.getStages()).thenReturn(stages);
            lenient().when(stages.byName(previousStageName.toString())).thenReturn(previousStage);
        }

        @Test
        void shouldReturnPipelineNotFoundIfPipelineSpecifiedIsNull() {
            ScheduleStageResult scheduleStageResult = schedulingChecker.shouldAllowSchedulingStage(null, stageName);

            assertEquals(ScheduleStageResult.PipelineNotFound, scheduleStageResult);
        }

        @Test
        void shouldNotAllowToScheduleIfPreviousStageHasNotRanYet() {
            when(pipeline.hasStageBeenRun(previousStageName.toString())).thenReturn(false);
            ScheduleStageResult scheduleStageResult = schedulingChecker.shouldAllowSchedulingStage(pipeline, stageName);

            assertEquals(ScheduleStageResult.PreviousStageNotRan, scheduleStageResult);
        }

        @Test
        void shouldNotAllowToScheduleIfPreviousStageFailedAndAllowOnlyOnSuccessWasSet() {
            when(approval.isAllowOnlyOnSuccess()).thenReturn(true);
            when(previousStage.getResult()).thenReturn(StageResult.Failed);

            ScheduleStageResult scheduleStageResult = schedulingChecker.shouldAllowSchedulingStage(pipeline, stageName);

            assertEquals(ScheduleStageResult.PreviousStageNotPassed, scheduleStageResult);
        }

        @Nested
        class CanSchedule {
            @Test
            void shouldAllowScheduleIfTheStageHasAlreadyRan() {
                when(pipeline.hasStageBeenRun(stageName)).thenReturn(true);

                ScheduleStageResult scheduleStageResult = schedulingChecker.shouldAllowSchedulingStage(pipeline, stageName);

                assertEquals(ScheduleStageResult.CanSchedule, scheduleStageResult);
            }

            @Test
            void shouldAllowScheduleIfTheStageIsTheFirstStage() {
                when(goConfigService.hasPreviousStage(pipelineName, stageName)).thenReturn(false);

                ScheduleStageResult scheduleStageResult = schedulingChecker.shouldAllowSchedulingStage(pipeline, stageName);

                assertEquals(ScheduleStageResult.CanSchedule, scheduleStageResult);
            }

            @Test
            void shouldAllowScheduleIfPreviousStageRanAndAllowOnlyOnSuccessWasNotSet() {
                when(approval.isAllowOnlyOnSuccess()).thenReturn(false);
                ScheduleStageResult scheduleStageResult = schedulingChecker.shouldAllowSchedulingStage(pipeline, stageName);

                assertEquals(ScheduleStageResult.CanSchedule, scheduleStageResult);
            }

            @Test
            void shouldAllowScheduleEvenIfPreviousStageRanUnSuccessfullyAndAllowOnlyOnSuccessWasNotSet() {
                when(approval.isAllowOnlyOnSuccess()).thenReturn(false);
                when(previousStage.getResult()).thenReturn(StageResult.Failed);

                ScheduleStageResult scheduleStageResult = schedulingChecker.shouldAllowSchedulingStage(pipeline, stageName);

                assertEquals(ScheduleStageResult.CanSchedule, scheduleStageResult);
            }

            @Test
            void shouldAllowScheduleIfPreviousStageRanSuccessfullyAndAllowOnlyOnSuccessWasSet() {
                when(approval.isAllowOnlyOnSuccess()).thenReturn(true);
                when(previousStage.getResult()).thenReturn(StageResult.Passed);

                ScheduleStageResult scheduleStageResult = schedulingChecker.shouldAllowSchedulingStage(pipeline, stageName);

                assertEquals(ScheduleStageResult.CanSchedule, scheduleStageResult);
            }
        }
    }

    private void assertFor(List<SchedulingChecker> checkerList, Class typeOfScheduleChecker) {
        ArrayList<SchedulingChecker> containerForAllCheckers = new ArrayList<>();
        flatten(checkerList, containerForAllCheckers);
        for (Object o : containerForAllCheckers) {
            if (o.getClass().equals(typeOfScheduleChecker)) {
                return;
            }
        }

        fail("could not find " + typeOfScheduleChecker);
    }

    private void flatten(List<SchedulingChecker> value, ArrayList<SchedulingChecker> containerForAllCheckers) {
        for (SchedulingChecker checker : value) {
            if (checker instanceof CompositeChecker) {
                List<SchedulingChecker> schedulingCheckers = Arrays.asList(((CompositeChecker) checker).getCheckers());
                flatten(schedulingCheckers, containerForAllCheckers);
            } else {
                containerForAllCheckers.add(checker);

            }
        }
    }
}
