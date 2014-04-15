/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.NullJobInstance;
import com.thoughtworks.go.util.ClassMockery;
import static org.hamcrest.core.Is.is;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class BuildRepositoryServiceTest {
    private Mockery context = new ClassMockery();
    private BuildRepositoryService buildRepositoryService;
    private JobInstanceService jobInstanceService;
    private String agentUuid = "uuid";
    private JobIdentifier jobIdendifier = new JobIdentifier("pipeline", "#1", "stage", "LATEST", "build", 1L);
    private ScheduleService scheduleService;

    @Before
    public void setup() {
        jobInstanceService = context.mock(JobInstanceService.class);
        StageService stageService = context.mock(StageService.class);
        scheduleService = context.mock(ScheduleService.class);
        buildRepositoryService = new BuildRepositoryService(stageService, jobInstanceService, scheduleService);
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotUpdateStatusFromWrongAgent() throws Exception {
        final JobState state = JobState.Assigned;
        final JobInstance instance = context.mock(JobInstance.class);
        context.checking(new Expectations() {
            {
                one(instance).isNull();
                will(returnValue(false));
                one(instance).getResult();
                will(returnValue(JobResult.Unknown));
                one(jobInstanceService).buildByIdWithTransitions(jobIdendifier.getBuildId());
                will(returnValue(instance));
                one(instance).getState();
                one(instance).changeState(with(equal(state)));
                one(jobInstanceService).updateStateAndResult(instance);
                atLeast(1).of(instance).getAgentUuid();
                will(returnValue(agentUuid));
            }
        });
        buildRepositoryService.updateStatusFromAgent(jobIdendifier, state, "wrongId");
    }

    @Test
    public void shouldUpdateResult() {
        checkUpdateResult(agentUuid);
    }

    private void checkUpdateResult(final String uuid) {
        final JobResult result = JobResult.Passed;
        context.checking(new Expectations() {
            {
                one(scheduleService).jobCompleting(jobIdendifier, result, uuid);
            }
        });
        buildRepositoryService.completing(jobIdendifier, result, uuid);
    }

    @Test
    public void shouldReturnFalseIfBuildInstanceDoesNotExist() {
        context.checking(new Expectations() {
            {
                one(jobInstanceService).buildByIdWithTransitions((long) with(any(Integer.class)));
                will(returnValue(NullJobInstance.NULL));
            }
        });
        assertThat(buildRepositoryService.isCancelledOrRescheduled(123L), is(false));
    }
}
