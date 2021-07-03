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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.lang3.time.DateUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static java.text.MessageFormat.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobInstanceTest {
    public TimeProvider timeProvider;

    @BeforeEach
    public void setup() throws Exception {
        timeProvider = mock(TimeProvider.class);
    }

    @Test
    public void shouldDetermineMostRecentPassedBeforeWithNullBuildInstances() {
        JobInstance mostRecent = JobInstanceMother.building("mostRecent");
        mostRecent.completing(JobResult.Passed, new Date());
        mostRecent.completed(new Date());
        assertEquals(mostRecent, mostRecent.mostRecentPassed(JobInstance.NULL));
        assertEquals(mostRecent, JobInstance.NULL.mostRecentPassed(mostRecent));
    }

    @Test
    public void shouldDetermineMostRecentPassed() {
        JobInstance oldestPassed = JobInstanceMother.building("oldestPassed");
        oldestPassed.completing(JobResult.Passed, DateUtils.addHours(new Date(), -1));
        oldestPassed.completed(DateUtils.addHours(new Date(), -1));

        JobInstance newestPassed = JobInstanceMother.building("newestPassed");
        newestPassed.completing(JobResult.Passed, new Date());
        newestPassed.completed(new Date());

        assertEquals(newestPassed, newestPassed.mostRecentPassed(oldestPassed));
        assertEquals(newestPassed, oldestPassed.mostRecentPassed(newestPassed));

        JobInstance newestFailed = JobInstanceMother.building("newestFailed");
        newestFailed.completing(JobResult.Failed, DateUtils.addHours(new Date(), +1));
        newestFailed.completed(DateUtils.addHours(new Date(), +1));

        assertEquals(newestPassed, newestPassed.mostRecentPassed(newestFailed));
    }

    @Test
    public void shouldGetDisplayStatusFailedIfStateIsCompletedAndResultIsFailed() {
        JobInstance job = JobInstanceMother.completed("test", JobResult.Failed);
        assertThat(job.displayStatusWithResult(), is("failed"));
    }

    @Test
    public void shouldGetDisplayStatusScheduledIfStateIsScheduled() {
        JobInstance job = JobInstanceMother.scheduled("test");
        assertThat(job.displayStatusWithResult(), is("scheduled"));
    }

    @Test
    public void shouldChangeStatus() throws Exception {
        JobInstance instance = JobInstanceMother.scheduled("jobConfig1");
        instance.assign("1234", timeProvider.currentTime());
        assertThat(instance.getState(), is(JobState.Assigned));
        assertThat(instance.getTransitions().byState(JobState.Assigned), not(nullValue()));
    }

    @Test
    public void shouldSetCompletingTimeAndResult() throws Exception {
        JobInstance instance = JobInstanceMother.scheduled("jobConfig1");
        final Date completionDate = new Date();
        instance.completing(JobResult.Passed, completionDate);

        assertThat(instance.getResult(), is(JobResult.Passed));
        assertThat(instance.getState(), is(JobState.Completing));
    }

    @Test
    public void shouldSetCompletedTimeOnComplete() throws Exception {
        JobInstance instance = JobInstanceMother.scheduled("jobConfig1");
        final Date completionDate = new Date();
        instance.completing(JobResult.Passed, completionDate);
        instance.completed(completionDate);

        assertThat(instance.getResult(), is(JobResult.Passed));
        assertThat(instance.getStartedDateFor(JobState.Completed), is(completionDate));
        assertThat(instance.getState(), is(JobState.Completed));
    }

    @Test
    public void shouldIncreaseElapsedTimeWhileBuilding() throws Exception {
        JobInstance instance = JobInstanceMother.building("jobConfig1");
        instance.setClock(timeProvider);
        when(timeProvider.currentTime()).thenReturn(new Date(1000000));
        long before = Long.parseLong(instance.getCurrentBuildDuration());
        when(timeProvider.currentTime()).thenReturn(new Date(5000000));
        long after = Long.parseLong(instance.getCurrentBuildDuration());
        assertTrue(after > before, "after " + after + " should bigger than " + before);
    }

    @Test
    public void shouldReturnTotalDurationOfBuild() throws Exception {
        JobInstance instance = JobInstanceMother.completed("jobConfig1");
        assertThat(instance.getCurrentBuildDuration(), is(instance.durationOfCompletedBuildInSeconds().toString()));
    }

    @Test
    public void shouldReturnBuildLocatorAsTitle() throws Exception {
        JobInstance instance = JobInstanceMother.completed("jobConfig1");
        assertThat(instance.getTitle(), is("pipeline/label-1/stage/1/jobConfig1"));
    }

    @Test
    public void shouldCreateATransitionOnStateChange() throws Exception {
        JobInstance instance = JobInstanceMother.scheduled("jobConfig1");
        instance.completing(JobResult.Passed);
        final JobStateTransition scheduledState = new JobStateTransition(JobState.Scheduled, new Date());
        final JobStateTransition completedState = new JobStateTransition(JobState.Completing, new Date());
        assertThat(instance.getTransitions(), hasItem(scheduledState));
        assertThat(instance.getTransitions(), hasItem(completedState));
        assertThat(instance.getTransitions().first(), not(isTransitionWithState(JobState.Preparing)));
    }

    @Test
    public void shouldNotCreateATransitionWhenPreviousStateIsTheSame() throws Exception {
        JobInstance instance = JobInstanceMother.scheduled("jobConfig1");
        instance.changeState(JobState.Scheduled);
        final JobStateTransition scheduledState = new JobStateTransition(JobState.Scheduled, new Date());

        assertThat(instance.getTransitions(), hasItem(scheduledState));
        assertThat(instance.getTransitions(), iterableWithSize(1));
        assertThat(instance.getTransitions().first(), not(isTransitionWithState(JobState.Preparing)));
    }

    @Test
    public void shouldReturnBuildingTransitionTimeAsStartBuildingDate() {
        final Date date = new Date();
        JobInstance instance = JobInstanceMother.scheduled("jobConfig1");
        JobStateTransitions transitions = new JobStateTransitions(
                new JobStateTransition(JobState.Building, date));
        instance.setTransitions(transitions);
        assertThat(instance.getStartedDateFor(JobState.Building), is(date));
    }

    public static JobStateTransitionMatcher isTransitionWithState(final JobState expectedState) {
        return new JobStateTransitionMatcher(expectedState);
    }

    @Test
    public void shouldCancelBuild() {
        final JobInstance instance = JobInstanceMother.scheduled("plan1");
        instance.cancel();
        assertThat(instance.getState(), is(JobState.Completed));
        assertThat(instance.getResult(), is(JobResult.Cancelled));
    }

    @Test
    public void shouldNotCancelCompletedBuild() {
        final JobInstance instance = JobInstanceMother.completed("plan1", JobResult.Passed);
        instance.cancel();
        assertThat(instance.getResult(), is(JobResult.Passed));
    }

    @Test
    public void shouldDetermineDurationOfCompletedBuild() throws Exception {
        JobInstance testJob = JobInstanceMother.completed("testJob");
        Long duration = testJob.durationOfCompletedBuildInSeconds();
        assertThat(duration, is(120L));
    }

    @Test public void durationShouldBeZeroForIncompleteBuild() throws Exception {
        JobInstance building = JobInstanceMother.scheduled("building");
        Long duration = building.durationOfCompletedBuildInSeconds();
        assertThat(duration, is(0L));
    }

    @Test
    public void shouldCleanAgentIdAndResultAfterRescheduled() throws Exception {
        JobInstance instance = JobInstanceMother.assignedWithAgentId("testBuild", "uuid");
        instance.completing(JobResult.Failed);
        instance.reschedule();
        assertThat(instance.getState(), is(JobState.Scheduled));
        assertThat(instance.getAgentUuid(), is(nullValue()));
        assertThat(instance.getResult(), is(JobResult.Unknown));
    }

    @Test public void shouldReturnDateForLatestTransition() throws Exception {
        JobInstance instance = JobInstanceMother.scheduled("jobConfig1");
        instance.setClock(timeProvider);
        when(timeProvider.currentTime()).thenReturn(new DateTime().plusDays(1).toDate());
        instance.completing(JobResult.Passed);
        assertThat(instance.latestTransitionDate(),is(greaterThan(instance.getScheduledDate())));
    }

    @Test
    public void shouldConsiderJobARerunWhenHasOriginalId() {
        JobInstance instance = new JobInstance();
        assertThat(instance.isCopy(), is(false));
        instance.setOriginalJobId(10l);
        assertThat(instance.isCopy(), is(true));
    }

    @Test
    public void shouldReturnJobDurationForACompletedJob() {
        int fiveSeconds = 5000;
        JobInstance instance = JobInstanceMother.passed("first", new Date(), fiveSeconds);
        assertThat(instance.getDuration(), is(new RunDuration.ActualDuration(new Duration(5 * fiveSeconds))));
    }

    @Test
    public void shouldReturnJobDurationForABuildingJob() {
        int fiveSeconds = 5000;
        JobInstance instance = JobInstanceMother.building("first", new Date(), fiveSeconds);
        assertThat(instance.getDuration(), is(RunDuration.IN_PROGRESS_DURATION));
    }

	@Test
	public void shouldReturnJobTypeCorrectly() {
		JobInstance jobInstance = new JobInstance();
		jobInstance.setRunOnAllAgents(true);
		assertThat(jobInstance.jobType(), instanceOf(RunOnAllAgents.class));

		jobInstance = new JobInstance();
		jobInstance.setRunMultipleInstance(true);
		assertThat(jobInstance.jobType(), instanceOf(RunMultipleInstance.class));

		jobInstance = new JobInstance();
		assertThat(jobInstance.jobType(), instanceOf(SingleJobInstance.class));
	}

    private static class JobStateTransitionMatcher extends BaseMatcher<JobStateTransition> {
        private JobState actualState;
        private final JobState expectedState;

        public JobStateTransitionMatcher(JobState expectedState) {
            this.expectedState = expectedState;
        }

        @Override
        public boolean matches(Object o) {
            JobStateTransition transition = (JobStateTransition) o;
            actualState = transition.getCurrentState();
            return actualState == expectedState;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(format("Expect to get a state {0} but was {1}", expectedState, actualState));
        }
    }
}
