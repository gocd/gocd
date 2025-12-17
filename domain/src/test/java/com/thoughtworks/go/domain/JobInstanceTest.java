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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.util.Dates;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobInstanceTest {
    public TimeProvider timeProvider;

    @BeforeEach
    public void setup() {
        timeProvider = mock(TimeProvider.class);
    }

    @Test
    public void shouldDetermineMostRecentPassedBeforeWithNullBuildInstances() {
        JobInstance mostRecent = JobInstanceMother.building("mostRecent");
        mostRecent.completing(JobResult.Passed, new Date());
        mostRecent.completed(new Date());
        assertEquals(mostRecent, mostRecent.mostRecentPassed(NullJobInstance.NAMELESS));
        assertEquals(mostRecent, NullJobInstance.NAMELESS.mostRecentPassed(mostRecent));
    }

    @Test
    public void shouldDetermineMostRecentPassed() {
        JobInstance oldestPassed = JobInstanceMother.building("oldestPassed");
        oldestPassed.completing(JobResult.Passed, Dates.from(ZonedDateTime.now().minusHours(1)));
        oldestPassed.completed(Dates.from(ZonedDateTime.now().minusHours(1)));

        JobInstance newestPassed = JobInstanceMother.building("newestPassed");
        newestPassed.completing(JobResult.Passed, new Date());
        newestPassed.completed(new Date());

        assertEquals(newestPassed, newestPassed.mostRecentPassed(oldestPassed));
        assertEquals(newestPassed, oldestPassed.mostRecentPassed(newestPassed));

        JobInstance newestFailed = JobInstanceMother.building("newestFailed");
        newestFailed.completing(JobResult.Failed, Dates.from(ZonedDateTime.now().plusHours(1)));
        newestFailed.completed(Dates.from(ZonedDateTime.now().plusHours(1)));

        assertEquals(newestPassed, newestPassed.mostRecentPassed(newestFailed));
    }

    @Test
    public void shouldGetDisplayStatusFailedIfStateIsCompletedButBadResult() {
        JobInstance job = JobInstanceMother.completed("test", JobResult.Failed);
        assertThat(job.displayStatusWithResult()).isEqualTo("failed");
    }

    @Test
    public void shouldGetDisplayStatusScheduledIfStateIsScheduled() {
        JobInstance job = JobInstanceMother.scheduled("test");
        assertThat(job.displayStatusWithResult()).isEqualTo("scheduled");
    }

    @Test
    public void shouldChangeStatus() {
        JobInstance instance = JobInstanceMother.scheduled("jobConfig1");
        instance.assign("1234", timeProvider.currentUtilDate());
        assertThat(instance.getState()).isEqualTo(JobState.Assigned);
        assertThat(instance.getTransitions().byState(JobState.Assigned)).isNotNull();
    }

    @Test
    public void shouldSetCompletingTimeAndResult() {
        JobInstance instance = JobInstanceMother.scheduled("jobConfig1");
        final Date completionDate = new Date();
        instance.completing(JobResult.Passed, completionDate);

        assertThat(instance.getResult()).isEqualTo(JobResult.Passed);
        assertThat(instance.getState()).isEqualTo(JobState.Completing);
    }

    @Test
    public void shouldSetCompletedTimeOnComplete() {
        JobInstance instance = JobInstanceMother.scheduled("jobConfig1");
        final Date completionDate = new Date();
        instance.completing(JobResult.Passed, completionDate);
        instance.completed(completionDate);

        assertThat(instance.getResult()).isEqualTo(JobResult.Passed);
        assertThat(instance.getStartedDateFor(JobState.Completed)).isEqualTo(completionDate);
        assertThat(instance.getState()).isEqualTo(JobState.Completed);
    }

    @Test
    public void shouldIncreaseElapsedTimeWhileBuilding() {
        JobInstance instance = JobInstanceMother.building("jobConfig1");
        instance.setClock(timeProvider);
        when(timeProvider.currentTimeMillis()).thenReturn(1000000L);
        long before = Long.parseLong(instance.getCurrentBuildDuration());
        when(timeProvider.currentTimeMillis()).thenReturn(5000000L);
        long after = Long.parseLong(instance.getCurrentBuildDuration());
        assertThat(after).isGreaterThan(before);
    }

    @Test
    public void shouldReturnTotalDurationOfBuild() {
        JobInstance instance = JobInstanceMother.completed("jobConfig1");
        assertThat(instance.getCurrentBuildDuration()).isEqualTo(instance.durationOfCompletedBuildInSeconds() + "");
    }

    @Test
    public void shouldReturnBuildLocatorAsTitle() {
        JobInstance instance = JobInstanceMother.completed("jobConfig1");
        assertThat(instance.getTitle()).isEqualTo("pipeline/label-1/stage/1/jobConfig1");
    }

    @Test
    public void shouldCreateATransitionOnStateChange() {
        JobInstance instance = JobInstanceMother.scheduled("jobConfig1");
        instance.completing(JobResult.Passed);
        final JobStateTransition scheduledState = new JobStateTransition(JobState.Scheduled, new Date());
        final JobStateTransition completedState = new JobStateTransition(JobState.Completing, new Date());
        assertThat(instance.getTransitions()).containsExactly(scheduledState, completedState)
            .first()
            .extracting(JobStateTransition::getCurrentState)
            .isNotEqualTo(JobState.Preparing);
    }

    @Test
    public void shouldNotCreateATransitionWhenPreviousStateIsTheSame() {
        JobInstance instance = JobInstanceMother.scheduled("jobConfig1");
        instance.changeState(JobState.Scheduled);
        final JobStateTransition scheduledState = new JobStateTransition(JobState.Scheduled, new Date());

        assertThat(instance.getTransitions())
            .containsOnly(scheduledState)
            .singleElement()
            .extracting(JobStateTransition::getCurrentState)
            .isNotEqualTo(JobState.Preparing);
    }

    @Test
    public void shouldReturnBuildingTransitionTimeAsStartBuildingDate() {
        final Date date = new Date();
        JobInstance instance = JobInstanceMother.scheduled("jobConfig1");
        JobStateTransitions transitions = new JobStateTransitions(
            new JobStateTransition(JobState.Building, date));
        instance.setTransitions(transitions);
        assertThat(instance.getStartedDateFor(JobState.Building)).isEqualTo(date);
    }

    @Test
    public void shouldCancelBuild() {
        final JobInstance instance = JobInstanceMother.scheduled("plan1");
        instance.cancel();
        assertThat(instance.getState()).isEqualTo(JobState.Completed);
        assertThat(instance.getResult()).isEqualTo(JobResult.Cancelled);
    }

    @Test
    public void shouldNotCancelCompletedBuild() {
        final JobInstance instance = JobInstanceMother.completed("plan1", JobResult.Passed);
        instance.cancel();
        assertThat(instance.getResult()).isEqualTo(JobResult.Passed);
    }

    @Test
    public void shouldDetermineDurationOfCompletedBuild() {
        JobInstance testJob = JobInstanceMother.completed("testJob");
        Long duration = testJob.durationOfCompletedBuildInSeconds();
        assertThat(duration).isEqualTo(120L);
    }

    @Test
    public void durationShouldBeZeroForIncompleteBuild() {
        JobInstance building = JobInstanceMother.scheduled("building");
        Long duration = building.durationOfCompletedBuildInSeconds();
        assertThat(duration).isEqualTo(0L);
    }

    @Test
    public void shouldCleanAgentIdAndResultAfterRescheduled() {
        JobInstance instance = JobInstanceMother.assignedWithAgentId("testBuild", "uuid");
        instance.completing(JobResult.Failed);
        instance.reschedule();
        assertThat(instance.getState()).isEqualTo(JobState.Scheduled);
        assertThat(instance.getAgentUuid()).isNull();
        assertThat(instance.getResult()).isEqualTo(JobResult.Unknown);
    }

    @Test
    public void shouldReturnDateForLatestTransition() {
        JobInstance instance = JobInstanceMother.scheduled("jobConfig1");
        instance.setClock(timeProvider);
        when(timeProvider.currentUtilDate()).thenReturn(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
        instance.completing(JobResult.Passed);
        assertThat(instance.latestTransitionDate()).isAfter(instance.getScheduledDate());
    }

    @Test
    public void shouldConsiderJobARerunWhenHasOriginalId() {
        JobInstance instance = new JobInstance();
        assertThat(instance.isCopy()).isFalse();
        instance.setOriginalJobId(10L);
        assertThat(instance.isCopy()).isTrue();
    }

    @Test
    public void shouldReturnJobDurationForACompletedJob() {
        int fiveSeconds = 5000;
        JobInstance instance = JobInstanceMother.passed("first", new Date(), fiveSeconds);
        assertThat(instance.getDuration()).isEqualTo(new RunDuration.ActualDuration(Duration.ofMillis(5 * fiveSeconds)));
    }

    @Test
    public void shouldReturnJobDurationForABuildingJob() {
        int fiveSeconds = 5000;
        JobInstance instance = JobInstanceMother.building("first", new Date(), fiveSeconds);
        assertThat(instance.getDuration()).isEqualTo(RunDuration.IN_PROGRESS_DURATION);
    }

    @Test
    public void shouldReturnJobTypeCorrectly() {
        JobInstance jobInstance = new JobInstance();
        jobInstance.setRunOnAllAgents(true);
        assertThat(jobInstance.jobType()).isInstanceOf(RunOnAllAgents.class);

        jobInstance = new JobInstance();
        jobInstance.setRunMultipleInstance(true);
        assertThat(jobInstance.jobType()).isInstanceOf(RunMultipleInstance.class);

        jobInstance = new JobInstance();
        assertThat(jobInstance.jobType()).isInstanceOf(SingleJobInstance.class);
    }
}
