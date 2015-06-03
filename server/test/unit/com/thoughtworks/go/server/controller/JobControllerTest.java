package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.dto.DurationBean;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.util.JsonValue;
import com.thoughtworks.go.util.json.JsonList;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;

import static com.thoughtworks.go.util.JsonUtils.from;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by poojar on 29/05/15.
 */
public class JobControllerTest {

    private JobController jobController;
    private JobInstanceService jobInstanceService;
    private JobDetailService jobDetailService;
    private GoConfigService jobConfigService;
    private StageService stageService;
    private MockHttpServletResponse response;
    private ScheduleService scheduleService;

    @Before
    public void setUp() throws Exception {
        jobInstanceService = mock(JobInstanceService.class);
        jobDetailService = mock(JobDetailService.class);
        jobConfigService = mock(GoConfigService.class);
        scheduleService = mock(ScheduleService.class);
        stageService = mock(StageService.class);
        response = new MockHttpServletResponse();
        jobController = new JobController(jobInstanceService, jobDetailService,jobConfigService,null,null,null,null,stageService,null);
    }

    @Test
    public void shouldFindTheLatestJobWhenJobStatusIsRequested() throws Exception {
        JobInstance job = JobInstanceMother.buildEndingWithState(JobState.Rescheduled, JobResult.Unknown, "config");
        job.assign("agent", new Date());

        JobInstance newJob = JobInstanceMother.buildEndingWithState(JobState.Building, JobResult.Unknown, "another_config");
        newJob.setId(2);
        newJob.assign("another_agent", new Date());


        String pipelineName = job.getPipelineName();
        String stageName = job.getStageName();


        when(jobInstanceService.buildByIdWithTransitions(job.getId())).thenReturn(job);
        when(jobDetailService.findMostRecentBuild(job.getIdentifier())).thenReturn(newJob);
        when(stageService.getBuildDuration(pipelineName, stageName, newJob)).thenReturn(new DurationBean(newJob.getId(), 5l));

        ModelAndView modelAndView = jobController.handleRequest(pipelineName, stageName, job.getId(), response);

        verify(jobInstanceService).buildByIdWithTransitions(job.getId());
        verify(jobDetailService).findMostRecentBuild(job.getIdentifier());
        verify(stageService).getBuildDuration(pipelineName, stageName, newJob);

        JsonValue json = from(((JsonList) modelAndView.getModel().get("json")).getJsonMap(0));

        JsonValue buildingInfo = json.getObject("building_info");

        assertThat(buildingInfo.getString("id"), is("2"));
        assertThat(buildingInfo.getString("last_build_duration"), is("5"));
    }

}
