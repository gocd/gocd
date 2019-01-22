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

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.dto.DurationBean;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.domain.Agent;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.util.JsonValue;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.thoughtworks.go.util.JsonUtils.from;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class JobControllerTest {

    private JobController jobController;
    private JobInstanceService jobInstanceService;
    private JobInstanceDao jobInstanceDao;
    private GoConfigService jobConfigService;
    private AgentService agentService;
    private StageService stageService;
    private PipelineService pipelineService;
    private MockHttpServletResponse response;
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        jobInstanceService = mock(JobInstanceService.class);
        jobInstanceDao = mock(JobInstanceDao.class);
        jobConfigService = mock(GoConfigService.class);
        agentService = mock(AgentService.class);
        stageService = mock(StageService.class);
        response = new MockHttpServletResponse();
        systemEnvironment = mock(SystemEnvironment.class);
        pipelineService = mock(PipelineService.class);
        jobController = new JobController(jobInstanceService, agentService, jobInstanceDao, jobConfigService, pipelineService, null, null, null, stageService, null, systemEnvironment);
    }

    @Test
    public void shouldFindTheLatestJobWhenJobStatusIsRequested() {
        JobInstance job = JobInstanceMother.buildEndingWithState(JobState.Rescheduled, JobResult.Unknown, "config");
        job.assign("agent", new Date());

        JobInstance newJob = JobInstanceMother.buildEndingWithState(JobState.Building, JobResult.Unknown, "another_config");
        newJob.setId(2);
        newJob.assign("another_agent", new Date());


        String pipelineName = job.getPipelineName();
        String stageName = job.getStageName();


        when(jobInstanceService.buildByIdWithTransitions(job.getId())).thenReturn(job);
        when(jobInstanceDao.mostRecentJobWithTransitions(job.getIdentifier())).thenReturn(newJob);
        when(agentService.findAgentObjectByUuid(newJob.getAgentUuid())).thenReturn(Agent.blankAgent(newJob.getAgentUuid()));
        when(stageService.getBuildDuration(pipelineName, stageName, newJob)).thenReturn(new DurationBean(newJob.getId(), 5l));

        ModelAndView modelAndView = jobController.handleRequest(pipelineName, stageName, job.getId(), response);

        verify(jobInstanceService).buildByIdWithTransitions(job.getId());
        verify(agentService).findAgentObjectByUuid(newJob.getAgentUuid());
        verify(jobInstanceDao).mostRecentJobWithTransitions(job.getIdentifier());
        verify(stageService).getBuildDuration(pipelineName, stageName, newJob);

        JsonValue json = from(((List) modelAndView.getModel().get("json")).get(0));

        JsonValue buildingInfo = json.getObject("building_info");

        assertThat(buildingInfo.getString("id"), is("2"));
        assertThat(buildingInfo.getString("last_build_duration"), is("5"));
    }

    @Test
    public void shouldThrowErrorIfUserPassesANonNumericValueForPipelineCounter() throws Exception {
        try {
            jobController.jobDetail("p1", "some-string", "s1", "1", "job");
            fail("Expected an exception to be thrown");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Expected numeric pipelineCounter or latest keyword, but received 'some-string' for [p1/some-string/s1/1/job]"));
        }
    }

    @Test
    public void shouldThrowErrorIfUserPassesANonNumericValueForStageCounter() throws Exception {
        try {
            when(pipelineService.resolvePipelineCounter("p1", "1")).thenReturn(Optional.of(1));
            jobController.jobDetail("p1", "1", "s1", "some-string", "job");
            fail("Expected an exception to be thrown");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Expected numeric stageCounter or latest keyword, but received 'some-string' for [p1/1/s1/some-string/job]"));
        }
    }
}
