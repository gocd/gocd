/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.NullJobInstance;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.remote.work.InvalidAgentException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class BuildRepositoryServiceTest {
    private BuildRepositoryService buildRepositoryService;
    @Mock
    private JobInstanceService jobInstanceService;
    private String agentUuid = "uuid";
    @Mock
    private ScheduleService scheduleService;
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private JobInstance jobInstance;

    @Before
    public void setup() {
        initMocks(this);
        buildRepositoryService = new BuildRepositoryService(jobInstanceService, scheduleService);
        jobInstance = JobInstanceMother.assignAgent(JobInstanceMother.building("job"), agentUuid);
        when(jobInstanceService.buildByIdWithTransitions(jobInstance.getIdentifier().getBuildId())).thenReturn(jobInstance);
    }

    @Test
    public void shouldNotUpdateStatusFromWrongAgent() throws Exception {
        thrown.expect(InvalidAgentException.class);
        thrown.expectMessage("AgentUUID has changed in the middle of a job. AgentUUID:");
        buildRepositoryService.updateStatusFromAgent(jobInstance.getIdentifier(), JobState.Assigned, "wrongId");
        verify(scheduleService, never()).updateJobStatus(eq(jobInstance.getIdentifier()), any());
    }

    @Test
    public void shouldUpdateStatusFromAssignedAgent() throws Exception {
        JobState state = JobState.Completing;
        buildRepositoryService.updateStatusFromAgent(jobInstance.getIdentifier(), state, agentUuid);
        verify(scheduleService).updateJobStatus(jobInstance.getIdentifier(), state);
    }

    @Test
    public void shouldUpdateResult() {
        final JobResult result = JobResult.Passed;
        buildRepositoryService.completing(jobInstance.getIdentifier(), result, agentUuid);
        verify(scheduleService).jobCompleting(jobInstance.getIdentifier(), result, agentUuid);
    }

    @Test
    public void shouldNotUpdateResultFromWrongAgent() {
        final JobResult result = JobResult.Passed;
        thrown.expect(InvalidAgentException.class);
        thrown.expectMessage("AgentUUID has changed in the middle of a job. AgentUUID:");
        buildRepositoryService.completing(jobInstance.getIdentifier(), result, "wrong-agent");
        verify(scheduleService, never()).jobCompleting(eq(jobInstance.getIdentifier()), any(), any());
    }

    @Test
    public void shouldReturnFalseIfBuildInstanceDoesNotExist() {
        when(jobInstanceService.buildByIdWithTransitions(123L)).thenReturn(NullJobInstance.NULL);
        assertThat(buildRepositoryService.isCancelledOrRescheduled(123L), is(false));
    }
}
