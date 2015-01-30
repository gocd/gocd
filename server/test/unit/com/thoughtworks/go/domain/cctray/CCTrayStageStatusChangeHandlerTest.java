/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.NullStage;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.StageMother;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class CCTrayStageStatusChangeHandlerTest {
    @Mock
    private CcTrayCache cache;
    @Mock
    private CcTrayBreakersCalculator breakersCalculator;
    @Mock
    private CcTrayJobStatusChangeHandler jobStatusChangeHandler;
    @Captor
    ArgumentCaptor<ProjectStatus> statusCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldNotUpdateCacheWhenStageWhichHasChangedIsANullStage() throws Exception {
        CCTrayStageStatusChangeHandler handler = new CCTrayStageStatusChangeHandler(cache, jobStatusChangeHandler, breakersCalculator);

        handler.call(new NullStage("some-name"));

        verifyZeroInteractions(cache);
        verifyZeroInteractions(breakersCalculator);
        verifyZeroInteractions(jobStatusChangeHandler);
    }

    @Test
    public void shouldUpdateCacheForStageAndAllJobsWithinIt() throws Exception {
        CCTrayStageStatusChangeHandler handler = new CCTrayStageStatusChangeHandler(cache, jobStatusChangeHandler, breakersCalculator);

        JobInstance firstJob = JobInstanceMother.building("job1");
        JobInstance secondJob = JobInstanceMother.completed("job2");
        Stage stage = StageMother.custom("stage1", firstJob, secondJob);
        handler.call(stage);

        verify(cache).replace(eq("pipeline :: stage1"), Matchers.<ProjectStatus>any());
        verify(jobStatusChangeHandler).updateForJob(eq(firstJob), Matchers.<Set<String>>any());
        verify(jobStatusChangeHandler).updateForJob(eq(secondJob), Matchers.<Set<String>>any());
    }

    @Test
    public void shouldUpdateStageInCacheWithBreakers() throws Exception {
        CCTrayStageStatusChangeHandler handler = new CCTrayStageStatusChangeHandler(cache, jobStatusChangeHandler, breakersCalculator);

        Stage stage = StageMother.custom("stage1", JobInstanceMother.building("job1"));
        when(breakersCalculator.calculateFor(stage)).thenReturn(s("breaker1", "breaker2"));

        handler.call(stage);

        verify(cache).replace(eq("pipeline :: stage1"), statusCaptor.capture());
        assertThat(statusCaptor.getValue().getBreakers(), is(s("breaker1", "breaker2")));
    }

    @Test
    public void shouldUpdateJobsInCacheWithBreakers_OnlyIfTheyHaveFailed() throws Exception {
        CCTrayStageStatusChangeHandler handler = new CCTrayStageStatusChangeHandler(cache, jobStatusChangeHandler, breakersCalculator);

        Stage stage = StageMother.custom("stage1", JobInstanceMother.building("job1"), JobInstanceMother.failed("job2"));
        when(breakersCalculator.calculateFor(stage)).thenReturn(s("breaker1", "breaker2"));

        handler.call(stage);

        verify(jobStatusChangeHandler).updateForJob(eq(stage.getJobInstances().get(0)), eq(Collections.<String>emptySet()));
        verify(jobStatusChangeHandler).updateForJob(eq(stage.getJobInstances().get(1)), eq(s("breaker1", "breaker2")));
    }

    @Test
    public void shouldUpdateStageActivityAndWebUrlInCache() throws Exception {
        CCTrayStageStatusChangeHandler handler = new CCTrayStageStatusChangeHandler(cache, jobStatusChangeHandler, breakersCalculator);

        Stage stage = StageMother.custom("stage1", JobInstanceMother.building("job1"));
        handler.call(stage);

        verify(cache).replace(eq("pipeline :: stage1"), statusCaptor.capture());
        assertThat(activityOf(statusCaptor.getValue()), is("Building"));
        assertThat(webUrlOf(statusCaptor.getValue()), is(webUrlFor("stage1")));
    }

    @Test
    public void shouldLeaveBuildDetailsOfStageSameAsTheDefault_WhenStageIsNotCompleted_AndThereIsNoExistingStageInCache() throws Exception {
        String projectName = "pipeline :: stage1";
        ProjectStatus.NullProjectStatus defaultStatus = new ProjectStatus.NullProjectStatus(projectName);

        Stage stage = StageMother.custom("stage1", JobInstanceMother.building("job1"));
        CCTrayStageStatusChangeHandler handler = new CCTrayStageStatusChangeHandler(cache, jobStatusChangeHandler, breakersCalculator);
        handler.call(stage);

        verify(cache).replace(eq(projectName), statusCaptor.capture());
        assertThat(stage.getState().completed(), is(not(true)));
        assertThat(statusCaptor.getValue().getLastBuildStatus(), is(defaultStatus.getLastBuildStatus()));
        assertThat(statusCaptor.getValue().getLastBuildTime(), is(defaultStatus.getLastBuildTime()));
        assertThat(statusCaptor.getValue().getLastBuildLabel(), is(defaultStatus.getLastBuildLabel()));
    }

    @Test
    public void shouldLeaveBuildDetailsOfStageSameAsTheOneInCache_WhenStageIsNotCompleted_AndThereIsAnExistingStageInCache() throws Exception {
        String projectName = "pipeline :: stage1";
        ProjectStatus existingStageStatus = new ProjectStatus(projectName, "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor("stage1"));
        when(cache.get(projectName)).thenReturn(existingStageStatus);

        Stage stage = StageMother.custom("stage1", JobInstanceMother.building("job1"));
        CCTrayStageStatusChangeHandler handler = new CCTrayStageStatusChangeHandler(cache, jobStatusChangeHandler, breakersCalculator);
        handler.call(stage);

        verify(cache).replace(eq(projectName), statusCaptor.capture());
        assertThat(stage.getState().completed(), is(not(true)));
        assertThat(statusCaptor.getValue().getLastBuildStatus(), is(existingStageStatus.getLastBuildStatus()));
        assertThat(statusCaptor.getValue().getLastBuildTime(), is(existingStageStatus.getLastBuildTime()));
        assertThat(statusCaptor.getValue().getLastBuildLabel(), is(existingStageStatus.getLastBuildLabel()));
    }

    @Test
    public void shouldUpdateBuildDetailsOfStageWhenStageIsCompleted() throws Exception {
        String projectName = "pipeline :: stage1";

        Stage completedStage = StageMother.createPassedStage("pipeline", 1, "stage1", 1, "job1", new Date());
        completedStage.setCompletedByTransitionId(1L);
        CCTrayStageStatusChangeHandler handler = new CCTrayStageStatusChangeHandler(cache, jobStatusChangeHandler, breakersCalculator);
        handler.call(completedStage);

        verify(cache).replace(eq(projectName), statusCaptor.capture());
        assertThat(completedStage.isCompleted(), is(true));

        assertThat(statusCaptor.getValue().getLastBuildStatus(), is("Success"));
        assertThat(statusCaptor.getValue().getLastBuildTime(), is(completedStage.completedDate()));
        assertThat(statusCaptor.getValue().getLastBuildLabel(), is("LABEL-1"));
    }

    private String activityOf(ProjectStatus status) {
        return status.ccTrayXmlElement("some-path").getAttribute("activity").getValue();
    }

    private String webUrlOf(ProjectStatus status) {
        return status.ccTrayXmlElement("some-path").getAttribute("webUrl").getValue();
    }

    private String webUrlFor(final String stageName) {
        return "some-path/pipelines/pipeline/1/" + stageName + "/1";
    }
}