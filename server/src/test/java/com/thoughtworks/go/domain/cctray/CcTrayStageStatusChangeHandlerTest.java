/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.NullStage;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.StageMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @BeforeEach
    public void setUp() {
        handler = new CcTrayStageStatusChangeHandler(cache, jobStatusChangeHandler, breakersCalculator);
    }

    @Test
    public void shouldGenerateStatusesForStageAndAllJobsWithinIt() {
        JobInstance firstJob = JobInstanceMother.building("job1");
        JobInstance secondJob = JobInstanceMother.completed("job2");
        Stage stage = StageMother.custom("stage1", firstJob, secondJob);

        when(jobStatusChangeHandler.statusFor(firstJob, new HashSet<>())).thenReturn(new ProjectStatus("job1_name", null, null, null, null, null));
        when(jobStatusChangeHandler.statusFor(secondJob, new HashSet<>())).thenReturn(new ProjectStatus("job2_name", null, null, null, null, null));

        List<ProjectStatus> statuses = handler.statusesOfStageAndItsJobsFor(stage);

        assertThat(statuses.size()).isEqualTo(3);
        assertThat(statuses.get(0).name()).isEqualTo("pipeline :: stage1");
        assertThat(statuses.get(1).name()).isEqualTo("job1_name");
        assertThat(statuses.get(2).name()).isEqualTo("job2_name");
    }

    @Test
    public void shouldCalculateBreakersForStage() {
        Stage stage = StageMother.custom("stage1", JobInstanceMother.building("job1"));
        when(breakersCalculator.calculateFor(stage)).thenReturn(Set.of("breaker1", "breaker2"));

        List<ProjectStatus> statuses = handler.statusesOfStageAndItsJobsFor(stage);

        assertThat(statuses.getFirst().getBreakers()).isEqualTo(Set.of("breaker1", "breaker2"));
    }

    @Test
    public void shouldCreateJobStatusesWithBreakers_OnlyIfTheyHaveFailed() {
        JobInstance job1_building = JobInstanceMother.building("job1");
        JobInstance job2_failed = JobInstanceMother.failed("job2");
        Stage stage = StageMother.custom("stage1", job1_building, job2_failed);
        when(breakersCalculator.calculateFor(stage)).thenReturn(Set.of("breaker1", "breaker2"));

        handler.statusesOfStageAndItsJobsFor(stage);

        verify(jobStatusChangeHandler).statusFor(job1_building, new HashSet<>());
        verify(jobStatusChangeHandler).statusFor(job2_failed, Set.of("breaker1", "breaker2"));
    }

    @Test
    public void shouldCreateStatusesWithStageActivityAndWebUrl() {
        Stage stage = StageMother.custom("stage1", JobInstanceMother.building("job1"));
        List<ProjectStatus> statuses = handler.statusesOfStageAndItsJobsFor(stage);

        ProjectStatus statusOfStage = statuses.getFirst();
        assertThat(activityOf(statusOfStage)).isEqualTo("Building");
        assertThat(webUrlOf(statusOfStage)).isEqualTo(webUrlFor("stage1"));
    }

    @Test
    public void shouldLeaveBuildDetailsOfStageSameAsTheDefault_WhenStageIsNotCompleted_AndThereIsNoExistingStageInCache() {
        String projectName = "pipeline :: stage1";
        ProjectStatus.NullProjectStatus defaultStatus = new ProjectStatus.NullProjectStatus(projectName);

        Stage stage = StageMother.custom("stage1", JobInstanceMother.building("job1"));
        List<ProjectStatus> statuses = handler.statusesOfStageAndItsJobsFor(stage);

        ProjectStatus statusOfStage = statuses.getFirst();
        assertThat(stage.getState().completed()).isNotEqualTo(true);
        assertThat(statusOfStage.getLastBuildStatus()).isEqualTo(defaultStatus.getLastBuildStatus());
        assertThat(statusOfStage.getLastBuildTime()).isEqualTo(defaultStatus.getLastBuildTime());
        assertThat(statusOfStage.getLastBuildLabel()).isEqualTo(defaultStatus.getLastBuildLabel());
    }

    @Test
    public void shouldLeaveBuildDetailsOfStageSameAsTheOneInCache_WhenStageIsNotCompleted_AndThereIsAnExistingStageInCache() {
        String projectName = "pipeline :: stage1";
        ProjectStatus existingStageStatus = new ProjectStatus(projectName, "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor("stage1"));
        when(cache.get(projectName)).thenReturn(existingStageStatus);

        Stage stage = StageMother.custom("stage1", JobInstanceMother.building("job1"));
        List<ProjectStatus> statuses = handler.statusesOfStageAndItsJobsFor(stage);

        ProjectStatus statusOfStage = statuses.getFirst();
        assertThat(stage.getState().completed()).isNotEqualTo(true);
        assertThat(statusOfStage.getLastBuildStatus()).isEqualTo(existingStageStatus.getLastBuildStatus());
        assertThat(statusOfStage.getLastBuildTime()).isEqualTo(existingStageStatus.getLastBuildTime());
        assertThat(statusOfStage.getLastBuildLabel()).isEqualTo(existingStageStatus.getLastBuildLabel());
    }

    @Test
    public void shouldUpdateBuildDetailsOfStageWhenStageIsCompleted() {
        Stage completedStage = StageMother.createPassedStage("pipeline", 1, "stage1", 1, "job1", Instant.now());
        completedStage.setCompletedByTransitionId(1L);
        List<ProjectStatus> statuses = handler.statusesOfStageAndItsJobsFor(completedStage);

        ProjectStatus statusOfStage = statuses.getFirst();
        assertThat(completedStage.isCompleted()).isTrue();
        assertThat(statusOfStage.getLastBuildStatus()).isEqualTo("Success");
        assertThat(statusOfStage.getLastBuildTime()).isEqualTo(completedStage.completedDate());
        assertThat(statusOfStage.getLastBuildLabel()).isEqualTo("LABEL-1");
    }

    @Test
    public void shouldReuseViewersListFromExistingStatusWhenCreatingNewStatus() {
        Users viewers = viewers(Set.of(new PluginRoleConfig("admin", "ldap")),"viewer1", "viewer2");

        String projectName = "pipeline :: stage1";
        ProjectStatus existingStageStatus = new ProjectStatus(projectName, "OldActivity", "OldStatus", "OldLabel", new Date(), webUrlFor("stage1"));
        existingStageStatus.updateViewers(viewers);
        when(cache.get(projectName)).thenReturn(existingStageStatus);

        Stage stage = StageMother.custom("stage1", JobInstanceMother.building("job1"));
        List<ProjectStatus> statuses = handler.statusesOfStageAndItsJobsFor(stage);

        ProjectStatus statusOfStage = statuses.getFirst();
        assertThat(statusOfStage.viewers()).isEqualTo(viewers);
    }

    @Test
    public void shouldNotUpdateCacheWhenStageWhichHasChangedIsANullStage() {
        handler.call(new NullStage("some-name"));

        verifyNoInteractions(cache);
        verifyNoInteractions(breakersCalculator);
        verifyNoInteractions(jobStatusChangeHandler);
    }

    @Test
    public void shouldUpdateCacheWhenStageWhichHasChangedIsNotANullStage() {
        Stage completedStage = StageMother.createPassedStage("pipeline", 1, "stage1", 1, "job1", Instant.now());
        ProjectStatus jobStatus = new ProjectStatus("job1_name", "activity1", "lastBuildStatus1", "lastBuildLabel1", new Date(), "webUrl1");
        when(jobStatusChangeHandler.statusFor(completedStage.getJobInstances().getFirst(), new HashSet<>())).thenReturn(jobStatus);

        handler.call(completedStage);

        verify(breakersCalculator).calculateFor(completedStage);
        verify(cache).putAll(statusesCaptor.capture());

        List<ProjectStatus> statusesWhichWereCached = statusesCaptor.getValue();
        assertThat(statusesWhichWereCached.size()).isEqualTo(2);
        assertThat(statusesWhichWereCached.getFirst().name()).isEqualTo("pipeline :: stage1");
        assertThat(statusesWhichWereCached.getFirst().getLastBuildStatus()).isEqualTo("Success");
        assertThat(activityOf(statusesWhichWereCached.getFirst())).isEqualTo("Sleeping");
        assertThat(statusesWhichWereCached.getLast()).isEqualTo(jobStatus);
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
        return new AllowedUsers(Set.of(users), roles);
    }
}
