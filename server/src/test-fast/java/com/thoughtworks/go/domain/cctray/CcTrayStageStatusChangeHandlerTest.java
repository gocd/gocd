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
package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.NullStage;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.StageMother;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.*;

import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class CcTrayStageStatusChangeHandlerTest {
    @Mock
    private CcTrayCache cache;
    @Mock
    private CcTrayBreakersCalculator breakersCalculator;
    @Mock
    private CcTrayJobStatusChangeHandler jobStatusChangeHandler;
    @Captor
    ArgumentCaptor<List<ProjectStatus>> statusesCaptor;

    private CcTrayStageStatusChangeHandler handler;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        handler = new CcTrayStageStatusChangeHandler(cache, jobStatusChangeHandler, breakersCalculator);
    }

    @Test
    public void shouldGenerateStatusesForStageAndAllJobsWithinIt() throws Exception {
        JobInstance firstJob = JobInstanceMother.building("job1");
        JobInstance secondJob = JobInstanceMother.completed("job2");
        Stage stage = StageMother.custom("stage1", firstJob, secondJob);

        when(jobStatusChangeHandler.statusFor(firstJob, new HashSet<>())).thenReturn(new ProjectStatus("job1_name", null, null, null, null, null));
        when(jobStatusChangeHandler.statusFor(secondJob, new HashSet<>())).thenReturn(new ProjectStatus("job2_name", null, null, null, null, null));

        List<ProjectStatus> statuses = handler.statusesOfStageAndItsJobsFor(stage);

        assertThat(statuses.size(), is(3));
        assertThat(statuses.get(0).name(), is("pipeline :: stage1"));
        assertThat(statuses.get(1).name(), is("job1_name"));
        assertThat(statuses.get(2).name(), is("job2_name"));
    }

    @Test
    public void shouldCalculateBreakersForStage() throws Exception {
        Stage stage = StageMother.custom("stage1", JobInstanceMother.building("job1"));
        when(breakersCalculator.calculateFor(stage)).thenReturn(s("breaker1", "breaker2"));

        List<ProjectStatus> statuses = handler.statusesOfStageAndItsJobsFor(stage);

        assertThat(statuses.get(0).getBreakers(), is(s("breaker1", "breaker2")));
    }

    @Test
    public void shouldCreateJobStatusesWithBreakers_OnlyIfTheyHaveFailed() throws Exception {
        JobInstance job1_building = JobInstanceMother.building("job1");
        JobInstance job2_failed = JobInstanceMother.failed("job2");
        Stage stage = StageMother.custom("stage1", job1_building, job2_failed);
        when(breakersCalculator.calculateFor(stage)).thenReturn(s("breaker1", "breaker2"));

        handler.statusesOfStageAndItsJobsFor(stage);

        verify(jobStatusChangeHandler).statusFor(job1_building, new HashSet<>());
        verify(jobStatusChangeHandler).statusFor(job2_failed, s("breaker1", "breaker2"));
    }

    @Test
    public void shouldCreateStatusesWithStageActivityAndWebUrl() throws Exception {
        Stage stage = StageMother.custom("stage1", JobInstanceMother.building("job1"));
        List<ProjectStatus> statuses = handler.statusesOfStageAndItsJobsFor(stage);

        ProjectStatus statusOfStage = statuses.get(0);
        assertThat(activityOf(statusOfStage), is("Building"));
        assertThat(webUrlOf(statusOfStage), is(webUrlFor("stage1")));
    }

    @Test
    public void shouldLeaveBuildDetailsOfStageSameAsTheDefault_WhenStageIsNotCompleted_AndThereIsNoExistingStageInCache() throws Exception {
        String projectName = "pipeline :: stage1";
        ProjectStatus.NullProjectStatus defaultStatus = new ProjectStatus.NullProjectStatus(projectName);

        Stage stage = StageMother.custom("stage1", JobInstanceMother.building("job1"));
        List<ProjectStatus> statuses = handler.statusesOfStageAndItsJobsFor(stage);

        ProjectStatus statusOfStage = statuses.get(0);
        assertThat(stage.getState().completed(), is(not(true)));
        assertThat(statusOfStage.getLastBuildStatus(), is(defaultStatus.getLastBuildStatus()));
        assertThat(statusOfStage.getLastBuildTime(), is(defaultStatus.getLastBuildTime()));
        assertThat(statusOfStage.getLastBuildLabel(), is(defaultStatus.getLastBuildLabel()));
    }

    @Test
    public void shouldLeaveBuildDetailsOfStageSameAsTheOneInCache_WhenStageIsNotCompleted_AndThereIsAnExistingStageInCache() throws Exception {
        String projectName = "pipeline :: stage1";
        ProjectStatus existingStageStatus = new ProjectStatus(projectName, "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor("stage1"));
        when(cache.get(projectName)).thenReturn(existingStageStatus);

        Stage stage = StageMother.custom("stage1", JobInstanceMother.building("job1"));
        List<ProjectStatus> statuses = handler.statusesOfStageAndItsJobsFor(stage);

        ProjectStatus statusOfStage = statuses.get(0);
        assertThat(stage.getState().completed(), is(not(true)));
        assertThat(statusOfStage.getLastBuildStatus(), is(existingStageStatus.getLastBuildStatus()));
        assertThat(statusOfStage.getLastBuildTime(), is(existingStageStatus.getLastBuildTime()));
        assertThat(statusOfStage.getLastBuildLabel(), is(existingStageStatus.getLastBuildLabel()));
    }

    @Test
    public void shouldUpdateBuildDetailsOfStageWhenStageIsCompleted() throws Exception {
        Stage completedStage = StageMother.createPassedStage("pipeline", 1, "stage1", 1, "job1", new Date());
        completedStage.setCompletedByTransitionId(1L);
        List<ProjectStatus> statuses = handler.statusesOfStageAndItsJobsFor(completedStage);

        ProjectStatus statusOfStage = statuses.get(0);
        assertThat(completedStage.isCompleted(), is(true));
        assertThat(statusOfStage.getLastBuildStatus(), is("Success"));
        assertThat(statusOfStage.getLastBuildTime(), is(completedStage.completedDate()));
        assertThat(statusOfStage.getLastBuildLabel(), is("LABEL-1"));
    }

    @Test
    public void shouldReuseViewersListFromExistingStatusWhenCreatingNewStatus() throws Exception {
        Users viewers = viewers(Collections.singleton(new PluginRoleConfig("admin", "ldap")),"viewer1", "viewer2");

        String projectName = "pipeline :: stage1";
        ProjectStatus existingStageStatus = new ProjectStatus(projectName, "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor("stage1"));
        existingStageStatus.updateViewers(viewers);
        when(cache.get(projectName)).thenReturn(existingStageStatus);

        Stage stage = StageMother.custom("stage1", JobInstanceMother.building("job1"));
        List<ProjectStatus> statuses = handler.statusesOfStageAndItsJobsFor(stage);

        ProjectStatus statusOfStage = statuses.get(0);
        assertThat(statusOfStage.viewers(), is(viewers));
    }

    @Test
    public void shouldNotUpdateCacheWhenStageWhichHasChangedIsANullStage() throws Exception {
        handler.call(new NullStage("some-name"));

        verifyZeroInteractions(cache);
        verifyZeroInteractions(breakersCalculator);
        verifyZeroInteractions(jobStatusChangeHandler);
    }

    @Test
    public void shouldUpdateCacheWhenStageWhichHasChangedIsNotANullStage() throws Exception {
        Stage completedStage = StageMother.createPassedStage("pipeline", 1, "stage1", 1, "job1", new Date());
        ProjectStatus jobStatus = new ProjectStatus("job1_name", "activity1", "lastBuildStatus1", "lastBuildLabel1", new Date(), "webUrl1");
        when(jobStatusChangeHandler.statusFor(completedStage.getJobInstances().first(), new HashSet<>())).thenReturn(jobStatus);

        handler.call(completedStage);

        verify(breakersCalculator).calculateFor(completedStage);
        verify(cache).putAll(statusesCaptor.capture());

        List<ProjectStatus> statusesWhichWereCached = statusesCaptor.getValue();
        assertThat(statusesWhichWereCached.size(), is(2));
        assertThat(statusesWhichWereCached.get(0).name(), is("pipeline :: stage1"));
        assertThat(statusesWhichWereCached.get(0).getLastBuildStatus(), is("Success"));
        assertThat(activityOf(statusesWhichWereCached.get(0)), is("Sleeping"));
        assertThat(statusesWhichWereCached.get(1), is(jobStatus));
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

    private Users viewers(Set<PluginRoleConfig> roles, String... users) {
        return new AllowedUsers(s(users), roles);
    }
}
