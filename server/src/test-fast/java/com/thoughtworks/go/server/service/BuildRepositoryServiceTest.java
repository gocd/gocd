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

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.NullJobInstance;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.remote.work.InvalidAgentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BuildRepositoryServiceTest {
    private BuildRepositoryService buildRepositoryService;
    @Mock
    private JobInstanceService jobInstanceService;
    private String agentUuid = "uuid";
    @Mock
    private ScheduleService scheduleService;
    private JobInstance jobInstance;

    @BeforeEach
    public void setup() {
        buildRepositoryService = new BuildRepositoryService(jobInstanceService, scheduleService);
        jobInstance = JobInstanceMother.assignAgent(JobInstanceMother.building("job"), agentUuid);
        lenient().when(jobInstanceService.buildByIdWithTransitions(jobInstance.getIdentifier().getBuildId())).thenReturn(jobInstance);
    }

    @Test
    public void shouldNotUpdateStatusFromWrongAgent() throws Exception {
        assertThatThrownBy(() -> buildRepositoryService.updateStatusFromAgent(jobInstance.getIdentifier(), JobState.Assigned, "wrongId"))
                .isInstanceOf(InvalidAgentException.class)
                .hasMessageContaining("AgentUUID has changed in the middle of a job. AgentUUID:");
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
        assertThatThrownBy(() ->  buildRepositoryService.completing(jobInstance.getIdentifier(), result, "wrong-agent"))
                .isInstanceOf(InvalidAgentException.class)
                .hasMessageContaining("AgentUUID has changed in the middle of a job. AgentUUID:");
        verify(scheduleService, never()).jobCompleting(eq(jobInstance.getIdentifier()), any(), any());
    }

    @Test
    public void shouldReturnFalseIfBuildInstanceDoesNotExist() {
        when(jobInstanceService.buildByIdWithTransitions(123L)).thenReturn(NullJobInstance.NULL);
        assertThat(buildRepositoryService.isCancelledOrRescheduled(123L), is(false));
    }
}
