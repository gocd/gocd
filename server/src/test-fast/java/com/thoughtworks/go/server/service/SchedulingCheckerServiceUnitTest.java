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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.cronjob.GoDiskSpaceMonitor;
import com.thoughtworks.go.server.scheduling.TriggerMonitor;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class SchedulingCheckerServiceUnitTest {

    private SchedulingCheckerService schedulingChecker;
    private CompositeChecker compositeChecker;
    private OperationResult operationResult;
    @Captor
    private ArgumentCaptor<List<SchedulingChecker>> argumentCaptor;


    @Before
    public void setUp() throws Exception {
        initMocks(this);
        schedulingChecker = spy(new SchedulingCheckerService(mock(GoConfigService.class), mock(StageService.class),
                mock(SecurityService.class), mock(PipelineLockService.class), mock(TriggerMonitor.class), mock(PipelineScheduleQueue.class),
                mock(PipelinePauseService.class), new OutOfDiskSpaceChecker(mock(GoDiskSpaceMonitor.class))));

        compositeChecker = mock(CompositeChecker.class);
        operationResult = mock(OperationResult.class);

        doReturn(compositeChecker).when(schedulingChecker).buildScheduleCheckers(any());
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
        schedulingChecker.canScheduleStage(new PipelineIdentifier("name",1),"stage","user",operationResult );

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
        schedulingChecker.canSchedule(operationResult );

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
