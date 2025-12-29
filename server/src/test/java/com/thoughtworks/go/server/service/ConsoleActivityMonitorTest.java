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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.serverhealth.HealthStateScope.forJob;
import static com.thoughtworks.go.serverhealth.HealthStateType.general;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.mockito.Mockito.*;

class ConsoleActivityMonitorTest {
    private static final Duration UNRESPONSIVE_JOB_KILL_THRESHOLD = Duration.ofMinutes(5);
    private static final Duration UNRESPONSIVE_JOB_WARNING_THRESHOLD = Duration.ofMinutes(2);
    private ConsoleActivityMonitor consoleActivityMonitor;
    private ConsoleActivityMonitor.ActiveJobListener activeJobListener;
    private ConsoleActivityMonitor.ScheduledJobListener scheduledJobListener;
    private TimeProvider timeProvider;
    private SystemEnvironment systemEnvironment;
    private JobInstanceService jobInstanceService;
    private ServerHealthService serverHealthService;
    private GoConfigService goConfigService;
    private ScheduleService scheduleService;
    private ConsoleService consoleService;

    @BeforeEach
    void setUp() {
        timeProvider = mock(TimeProvider.class);
        systemEnvironment = mock(SystemEnvironment.class);
        serverHealthService = mock(ServerHealthService.class);
        jobInstanceService = mock(JobInstanceService.class);
        scheduleService = mock(ScheduleService.class);
        goConfigService = mock(GoConfigService.class);
        consoleService = mock(ConsoleService.class);

        when(goConfigService.getUnresponsiveJobTerminationThreshold(any())).thenReturn(UNRESPONSIVE_JOB_KILL_THRESHOLD);
        when(systemEnvironment.getUnresponsiveJobWarningThreshold()).thenReturn(UNRESPONSIVE_JOB_WARNING_THRESHOLD);
        when(goConfigService.canCancelJobIfHung(any())).thenReturn(true);

        doAnswer((Answer<Object>) invocation -> {
            activeJobListener = (ConsoleActivityMonitor.ActiveJobListener) invocation.getArguments()[0];
            return null;
        }).when(jobInstanceService).registerJobStateChangeListener(any(ConsoleActivityMonitor.ActiveJobListener.class));
        doAnswer((Answer<Object>) invocation -> {
            scheduledJobListener = (ConsoleActivityMonitor.ScheduledJobListener) invocation.getArguments()[0];
            return null;
        }).when(jobInstanceService).registerJobStateChangeListener(any(ConsoleActivityMonitor.ScheduledJobListener.class));
        consoleActivityMonitor = new ConsoleActivityMonitor(timeProvider, systemEnvironment, jobInstanceService, serverHealthService, goConfigService, consoleService);
        consoleActivityMonitor.init();
        consoleActivityMonitor.populateActivityMap();
        stubInitializerCallsForActivityMonitor(jobInstanceService);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(jobInstanceService);
    }

    @Test
    void shouldMonitorConsoleActivityForBuildingJobs() {
        when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1971, 1, 1, 0, 55, 59, 0, UTC).toInstant().toEpochMilli());
        JobIdentifier unresponsiveJob = new JobIdentifier("pipelines", 10, "label-10", "stage", "3", "job", 25L);
        consoleActivityMonitor.consoleUpdatedFor(unresponsiveJob);

        when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1972, 1, 1, 1, 5, 0, 0, UTC).toInstant().toEpochMilli());

        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verifyNoMoreInteractions(jobInstanceService);
        verifyNoMoreInteractions(scheduleService);
    }

    @Test
    void shouldCancelUnresponsiveJobs() {
        JobIdentifier unresponsiveJob = new JobIdentifier("pipelines", 10, "label-10", "stage", "3", "job", 25L);
        activeJobListener.jobStatusChanged(buildingInstance(unresponsiveJob));

        JobIdentifier responsiveJob = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20L);
        activeJobListener.jobStatusChanged(buildingInstance(responsiveJob));

        when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1971, 1, 1, 0, 55, 59, 0, UTC).toInstant().toEpochMilli());

        consoleActivityMonitor.consoleUpdatedFor(unresponsiveJob);

        consoleActivityMonitor.consoleUpdatedFor(responsiveJob);
        when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1972, 1, 1, 1, 0, 0, 0, UTC).toInstant().toEpochMilli());
        consoleActivityMonitor.consoleUpdatedFor(responsiveJob);

        when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1972, 1, 1, 1, 1, 0, 0, UTC).toInstant().toEpochMilli());
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

        verify(scheduleService).cancelJob(unresponsiveJob);

        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verifyNoMoreInteractions(jobInstanceService);
    }

    @Test
    void shouldCancelUnresponsiveJobAndLetOtherJobComplete() {
        JobIdentifier unresponsiveJob = new JobIdentifier("pipelines", 10, "label-10", "stage", "3", "job", 25L);
        activeJobListener.jobStatusChanged(buildingInstance(unresponsiveJob));

        JobIdentifier responsiveJob = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20L);
        activeJobListener.jobStatusChanged(buildingInstance(responsiveJob));

        when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1971, 1, 1, 0, 55, 59, 0, UTC).toInstant().toEpochMilli());

        consoleActivityMonitor.consoleUpdatedFor(unresponsiveJob);

        consoleActivityMonitor.consoleUpdatedFor(responsiveJob);
        when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1972, 1, 1, 1, 0, 0, 0, UTC).toInstant().toEpochMilli());
        consoleActivityMonitor.consoleUpdatedFor(responsiveJob);

        when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1972, 1, 1, 1, 1, 0, 0, UTC).toInstant().toEpochMilli());
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

        verify(scheduleService).cancelJob(unresponsiveJob);

        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verifyNoMoreInteractions(jobInstanceService);
    }

    @Test
    void shouldNotCancelJobIfJobTimeOutIsSetTo0() {
        JobIdentifier unresponsiveJob = new JobIdentifier("pipelines", 10, "label-10", "stage", "3", "job", 25L);
        activeJobListener.jobStatusChanged(buildingInstance(unresponsiveJob));

        when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1971, 1, 1, 0, 55, 59, 0, UTC).toInstant().toEpochMilli());
        consoleActivityMonitor.consoleUpdatedFor(unresponsiveJob);

        when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1972, 1, 1, 1, 1, 0, 0, UTC).toInstant().toEpochMilli());
        when(goConfigService.canCancelJobIfHung(unresponsiveJob)).thenReturn(false);

        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

        verify(scheduleService, never()).cancelJob(unresponsiveJob);

        verifyNoMoreInteractions(jobInstanceService);
    }

    @Test
    void shouldNotTrackConsoleActivityFor_completedJob() {
        JobIdentifier jobId = new JobIdentifier("foo-pipeline", 10, "foo-10", "bar-stage", "20", "baz-build");
        JobInstance job = buildingInstance(jobId);

        activeJobListener.jobStatusChanged(job);
        Date jobStartAndCompleteTime = new Date();
        when(timeProvider.currentTimeMillis()).thenReturn(jobStartAndCompleteTime.getTime());

        consoleActivityMonitor.consoleUpdatedFor(jobId);
        job.completing(JobResult.Passed);
        job.completed(new Date());
        activeJobListener.jobStatusChanged(job);

        when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.now().plusDays(10).toInstant().toEpochMilli());

        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verifyNoMoreInteractions(jobInstanceService);
    }

    @Test
    void shouldNotCancelCompletedJob_becauseOfActivityAfterCompletion() {
        ZonedDateTime now = ZonedDateTime.now();
        when(timeProvider.currentTimeMillis()).thenReturn(now.toInstant().toEpochMilli());

        JobIdentifier jobId = new JobIdentifier("foo-pipeline", 10, "foo-10", "bar-stage", "20", "baz-build");
        JobInstance job = buildingInstance(jobId);

        activeJobListener.jobStatusChanged(job);
        job.completing(JobResult.Passed);
        job.completed(new Date());
        activeJobListener.jobStatusChanged(job);

        consoleActivityMonitor.consoleUpdatedFor(jobId); //Once a job is completed we should not track the console updates.

        when(timeProvider.currentTimeMillis()).thenReturn(now.plusDays(10).toInstant().toEpochMilli());
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verifyNoMoreInteractions(jobInstanceService);
    }

    @Test
    void shouldConsiderAllRunningJobsActiveOnInitialization() {
        long now = System.currentTimeMillis();
        when(timeProvider.currentTimeMillis()).thenReturn(now);

        JobIdentifier firstJob = new JobIdentifier("pipeline-foo", 10, "foo-10", "stage-bar", "12", "build");
        JobIdentifier secondJob = new JobIdentifier("pipeline-bar", 12, "bar-12", "stage-baz", "15", "quux");

        JobInstanceService jobInstanceService = mock(JobInstanceService.class);
        when(jobInstanceService.allRunningJobs()).thenReturn(List.of(scheduledInstance(firstJob), buildingInstance(secondJob)));
        consoleActivityMonitor = new ConsoleActivityMonitor(timeProvider, systemEnvironment, jobInstanceService, serverHealthService, goConfigService, consoleService);
        consoleActivityMonitor.init();
        consoleActivityMonitor.populateActivityMap();
        stubInitializerCallsForActivityMonitor(jobInstanceService);

        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verifyNoMoreInteractions(jobInstanceService);

        when(timeProvider.currentTimeMillis()).thenReturn(now + UNRESPONSIVE_JOB_KILL_THRESHOLD.toMillis() - 1);//just below threshold
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verifyNoMoreInteractions(jobInstanceService);

        when(timeProvider.currentTimeMillis()).thenReturn(now + UNRESPONSIVE_JOB_KILL_THRESHOLD.toMillis() + 1);//just above threshold
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verify(scheduleService).cancelJob(firstJob);
        verify(scheduleService).cancelJob(secondJob);
    }

    @Test
    void shouldSetServerHealthMessageWhenJobSeemsUnresponsive() {
        ZonedDateTime now = ZonedDateTime.now();
        when(timeProvider.currentTimeMillis()).thenReturn(now.toInstant().toEpochMilli());
        JobIdentifier job = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20L);
        activeJobListener.jobStatusChanged(buildingInstance(job));

        when(timeProvider.currentTimeMillis()).thenReturn(now.plusMinutes(2).plusSeconds(1).toInstant().toEpochMilli());//just over warning time limit i.e. 2 minutes
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

        verify(serverHealthService).update(ServerHealthState.warningWithHtml("Job 'foo/stage/job' is not responding",
                "Job <a href='/go/tab/build/detail/foo/12/stage/2/job'>foo/stage/job</a> is currently running but has not shown any console activity in the last 2 minute(s). This job may be hung.",
                general(forJob("foo", "stage", "job"))));

        when(timeProvider.currentTimeMillis()).thenReturn(now.plusMinutes(4).plusSeconds(1).toInstant().toEpochMilli());//after 4 minutes
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

        verify(serverHealthService).update(ServerHealthState.warningWithHtml("Job 'foo/stage/job' is not responding",
                "Job <a href='/go/tab/build/detail/foo/12/stage/2/job'>foo/stage/job</a> is currently running but has not shown any console activity in the last 4 minute(s). This job may be hung.",
                general(forJob("foo", "stage", "job"))));

        when(goConfigService.getUnresponsiveJobTerminationThreshold(any())).thenReturn(Duration.ofHours(6));
        when(timeProvider.currentTimeMillis()).thenReturn(now.plusHours(1).plusMinutes(2).plusSeconds(1).toInstant().toEpochMilli());//after 62 minutes
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

        verify(serverHealthService).update(ServerHealthState.warningWithHtml("Job 'foo/stage/job' is not responding",
                "Job <a href='/go/tab/build/detail/foo/12/stage/2/job'>foo/stage/job</a> is currently running but has not shown any console activity in the last 62 minute(s). This job may be hung.",
                general(forJob("foo", "stage", "job"))));
    }

    @Test
    void shouldClearServerHealthMessageWhenUnresponsiveJobShowsActivity() {
        ZonedDateTime now = ZonedDateTime.now();
        when(timeProvider.currentTimeMillis()).thenReturn(now.toInstant().toEpochMilli());
        JobIdentifier responsiveJob = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20L);
        activeJobListener.jobStatusChanged(buildingInstance(responsiveJob));

        when(timeProvider.currentTimeMillis()).thenReturn(now.plusMinutes(2).plus(1, MILLIS).toInstant().toEpochMilli());
        consoleActivityMonitor.consoleUpdatedFor(responsiveJob);
        verify(serverHealthService).removeByScope(forJob("foo", "stage", "job"));
    }

    @Test
    void shouldClearServerHealthMessageWhenUnresponsiveJobIsCancelled() {
        ZonedDateTime now = ZonedDateTime.now();
        when(timeProvider.currentTimeMillis()).thenReturn(now.toInstant().toEpochMilli());
        JobIdentifier unresponsiveJob = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20L);
        activeJobListener.jobStatusChanged(buildingInstance(unresponsiveJob));
        when(timeProvider.currentTimeMillis()).thenReturn(now.plus(UNRESPONSIVE_JOB_KILL_THRESHOLD).plus(1, MILLIS).toInstant().toEpochMilli());
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verify(scheduleService).cancelJob(unresponsiveJob);
        verify(serverHealthService).removeByScope(forJob("foo", "stage", "job"));
    }

    @Test
    void shouldClearServerHealthMessageForAnyJobCancelledExternally() {
        ZonedDateTime now = ZonedDateTime.now();
        when(timeProvider.currentTimeMillis()).thenReturn(now.toInstant().toEpochMilli());
        JobIdentifier unresponsiveJob = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20L);
        JobInstance job = buildingInstance(unresponsiveJob);
        activeJobListener.jobStatusChanged(job);
        job.cancel();
        activeJobListener.jobStatusChanged(job);
        verify(serverHealthService).removeByScope(forJob("foo", "stage", "job"));
    }

    @Test
    void shouldAppendToConsoleLog_JobKilledDueToInactivityMessage() throws IllegalArtifactLocationException {
        JobIdentifier unresponsiveJob = new JobIdentifier("pipelines", 10, "label-10", "stage", "3", "job", 25L);
        activeJobListener.jobStatusChanged(buildingInstance(unresponsiveJob));

        when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1971, 1, 1, 0, 55, 59, 0, UTC).toInstant().toEpochMilli());
        consoleActivityMonitor.consoleUpdatedFor(unresponsiveJob);

        when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1972, 1, 1, 1, 1, 0, 0, UTC).toInstant().toEpochMilli());
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

        verify(consoleService).appendToConsoleLogSafe(unresponsiveJob, "Go cancelled this job as it has not generated any console output for more than 5 minute(s)");
    }

    @Test
    void shouldClearServerHealthMessageForARescheduledJob() {
        JobIdentifier rescheduledJobIdentifier = new JobIdentifier("foo", 10, "label-10", "stage", "3", "job", 10L);
        JobInstance jobInstance = new JobInstance();
        jobInstance.setIdentifier(rescheduledJobIdentifier);
        jobInstance.setState(JobState.Rescheduled);

        activeJobListener.jobStatusChanged(jobInstance);

        verify(serverHealthService).removeByScope(forJob("foo", "stage", "job"));
    }

    @Nested
    class ScheduledJob {
        @Test
        void shouldCancelUnassignedJobAndLetOtherJobComplete() {
            JobIdentifier unassignedJob = new JobIdentifier("pipelines", 10, "label-10", "stage", "3", "job", 25L);
            scheduledJobListener.jobStatusChanged(scheduledInstance(unassignedJob));

            JobIdentifier responsiveJob = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20L);
            scheduledJobListener.jobStatusChanged(buildingInstance(responsiveJob));

            when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1971, 1, 1, 0, 55, 59, 0, UTC).toInstant().toEpochMilli());

            consoleActivityMonitor.consoleUpdatedFor(responsiveJob);
            when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1972, 1, 1, 1, 0, 0, 0, UTC).toInstant().toEpochMilli());
            consoleActivityMonitor.consoleUpdatedFor(responsiveJob);

            when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1972, 1, 1, 1, 1, 0, 0, UTC).toInstant().toEpochMilli());
            consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

            verify(scheduleService).cancelJob(unassignedJob);

            consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
            verifyNoMoreInteractions(jobInstanceService);
        }

        @Test
        void shouldNotCancelJobIfJobTimeOutIsSetTo0() {
            JobIdentifier unresponsiveJob = new JobIdentifier("pipelines", 10, "label-10", "stage", "3", "job", 25L);
            scheduledJobListener.jobStatusChanged(scheduledInstance(unresponsiveJob));

            when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1971, 1, 1, 0, 55, 59, 0, UTC).toInstant().toEpochMilli());
            consoleActivityMonitor.consoleUpdatedFor(unresponsiveJob);

            when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1972, 1, 1, 1, 1, 0, 0, UTC).toInstant().toEpochMilli());
            when(goConfigService.canCancelJobIfHung(unresponsiveJob)).thenReturn(false);

            consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

            verify(scheduleService, never()).cancelJob(unresponsiveJob);

            verifyNoMoreInteractions(jobInstanceService);
        }

        @Test
        void shouldSetServerHealthMessageWhenJobIsUnassignedForMoreThan5minutes() {
            ZonedDateTime now = ZonedDateTime.now();
            when(timeProvider.currentTimeMillis()).thenReturn(now.toInstant().toEpochMilli());
            JobIdentifier job = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20L);
            scheduledJobListener.jobStatusChanged(scheduledInstance(job));

            when(timeProvider.currentTimeMillis()).thenReturn(now.plusMinutes(2).plusSeconds(1).toInstant().toEpochMilli());//just over warning time limit i.e. 2 minutes
            consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

            verify(serverHealthService).update(ServerHealthState.warningWithHtml("Job 'foo/stage/job' is not responding",
                    "Job <a href='/go/tab/build/detail/foo/12/stage/2/job'>foo/stage/job</a> is currently running but it has not been assigned an agent in the last 2 minute(s). This job may be hung.", general(forJob("foo", "stage", "job"))));

            when(timeProvider.currentTimeMillis()).thenReturn(now.plusMinutes(4).plusSeconds(1).toInstant().toEpochMilli());//after 4 minutes
            consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

            verify(serverHealthService).update(ServerHealthState.warningWithHtml("Job 'foo/stage/job' is not responding",
                    "Job <a href='/go/tab/build/detail/foo/12/stage/2/job'>foo/stage/job</a> is currently running but it has not been assigned an agent in the last 4 minute(s). This job may be hung.", general(forJob("foo", "stage", "job"))));

            when(goConfigService.getUnresponsiveJobTerminationThreshold(any())).thenReturn(Duration.ofHours(6));
            when(timeProvider.currentTimeMillis()).thenReturn(now.plusHours(1).plusMinutes(2).plusSeconds(1).toInstant().toEpochMilli());//after 62 minutes
            consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

            verify(serverHealthService).update(ServerHealthState.warningWithHtml("Job 'foo/stage/job' is not responding",
                    "Job <a href='/go/tab/build/detail/foo/12/stage/2/job'>foo/stage/job</a> is currently running but it has not been assigned an agent in the last 62 minute(s). This job may be hung.", general(forJob("foo", "stage", "job"))));
        }

        @Test
        void shouldClearServerHealthMessageWhenUnassignedJobGetsAssigned() {
            ZonedDateTime now = ZonedDateTime.now();
            when(timeProvider.currentTimeMillis()).thenReturn(now.toInstant().toEpochMilli());
            JobIdentifier responsiveJob = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20L);
            JobInstance jobInstance = scheduledInstance(responsiveJob);
            scheduledJobListener.jobStatusChanged(jobInstance);

            jobInstance.setState(JobState.Assigned);
            when(timeProvider.currentTimeMillis()).thenReturn(now.plusMinutes(2).plus(1, MILLIS).toInstant().toEpochMilli());
            scheduledJobListener.jobStatusChanged(jobInstance);
            verify(serverHealthService).removeByScope(forJob("foo", "stage", "job"));
        }

        @Test
        void shouldClearServerHealthMessageWhenUnassignedJobIsCancelled() {
            ZonedDateTime now = ZonedDateTime.now();
            when(timeProvider.currentTimeMillis()).thenReturn(now.toInstant().toEpochMilli());
            JobIdentifier unresponsiveJob = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20L);
            scheduledJobListener.jobStatusChanged(scheduledInstance(unresponsiveJob));
            when(timeProvider.currentTimeMillis()).thenReturn(now.plus(UNRESPONSIVE_JOB_KILL_THRESHOLD).plus(1, MILLIS).toInstant().toEpochMilli());
            consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
            verify(scheduleService).cancelJob(unresponsiveJob);
            verify(serverHealthService).removeByScope(forJob("foo", "stage", "job"));
        }

        @Test
        void shouldClearServerHealthMessageForAnyJobCancelledExternally() {
            ZonedDateTime now = ZonedDateTime.now();
            when(timeProvider.currentTimeMillis()).thenReturn(now.toInstant().toEpochMilli());
            JobIdentifier unresponsiveJob = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20L);
            JobInstance job = scheduledInstance(unresponsiveJob);
            scheduledJobListener.jobStatusChanged(job);
            job.cancel();
            scheduledJobListener.jobStatusChanged(job);
            verify(serverHealthService).removeByScope(forJob("foo", "stage", "job"));
        }

        @Test
        void shouldAppendToConsoleLog_JobKilledAsItWasntAssignedAnAgentMessage() throws IllegalArtifactLocationException {
            JobIdentifier unresponsiveJob = new JobIdentifier("pipelines", 10, "label-10", "stage", "3", "job", 25L);
            scheduledJobListener.jobStatusChanged(scheduledInstance(unresponsiveJob));

            when(timeProvider.currentTimeMillis()).thenReturn(ZonedDateTime.of(1972, 1, 1, 1, 1, 0, 0, UTC).toInstant().toEpochMilli());
            consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

            verify(consoleService).appendToConsoleLogSafe(unresponsiveJob, "Go cancelled this job as it has not been assigned an agent for more than 5 minute(s)");
        }

        @Test
        void shouldClearServerHealthMessageForAssignedJob() {
            JobIdentifier rescheduledJobIdentifier = new JobIdentifier("foo", 10, "label-10", "stage", "3", "job", 10L);
            JobInstance jobInstance = new JobInstance();
            jobInstance.setIdentifier(rescheduledJobIdentifier);
            jobInstance.setState(JobState.Scheduled);
            scheduledJobListener.jobStatusChanged(jobInstance);

            verifyNoInteractions(serverHealthService);

            jobInstance.setState(JobState.Assigned);
            scheduledJobListener.jobStatusChanged(jobInstance);

            verify(serverHealthService).removeByScope(forJob("foo", "stage", "job"));
        }

        @Test
        void shouldNotClearMessagesIfJobHasAlreadyBeenAssigned() {
            ZonedDateTime now = ZonedDateTime.now();
            when(timeProvider.currentTimeMillis()).thenReturn(now.toInstant().toEpochMilli());
            JobIdentifier responsiveJob = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20L);
            JobInstance jobInstance = scheduledInstance(responsiveJob);
            scheduledJobListener.jobStatusChanged(jobInstance);

            jobInstance.setState(JobState.Assigned);
            when(timeProvider.currentTimeMillis()).thenReturn(now.plusMinutes(2).plus(1, MILLIS).toInstant().toEpochMilli());
            scheduledJobListener.jobStatusChanged(jobInstance);
            verify(serverHealthService).removeByScope(forJob("foo", "stage", "job"));

            jobInstance.setState(JobState.Preparing);
            when(timeProvider.currentTimeMillis()).thenReturn(now.plusMinutes(2).plus(2, MILLIS).toInstant().toEpochMilli());
            scheduledJobListener.jobStatusChanged(jobInstance);
            verifyNoMoreInteractions(serverHealthService);
        }
    }

    private void stubInitializerCallsForActivityMonitor(final JobInstanceService jobInstanceService) {
        verify(jobInstanceService, times(2)).registerJobStateChangeListener(any());
        verify(jobInstanceService).allRunningJobs();
    }

    private JobInstance scheduledInstance(JobIdentifier responsiveJob) {
        JobInstance respJobInstance = new JobInstance();
        respJobInstance.setIdentifier(responsiveJob);
        respJobInstance.setState(JobState.Scheduled);
        return respJobInstance;
    }

    private JobInstance buildingInstance(JobIdentifier responsiveJob) {
        JobInstance respJobInstance = new JobInstance();
        respJobInstance.setIdentifier(responsiveJob);
        respJobInstance.setState(JobState.Building);
        return respJobInstance;
    }
}
