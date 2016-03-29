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

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ConsoleActivityMonitorTest {
    private static final long UNRESPONSIVE_JOB_KILL_THRESHOLD = 5*60*1000l;
    private static final long UNRESPONSIVE_JOB_WARNING_THRESHOLD = 2 * 60 * 1000l;
    private ConsoleActivityMonitor consoleActivityMonitor;
    private ConsoleActivityMonitor.ActiveJobListener listener;
    private TimeProvider timeProvider;
    private SystemEnvironment systemEnvironment;
    private JobInstanceService jobInstanceService;
    private ServerHealthService serverHealthService;
    private GoConfigService goConfigService;
    private ScheduleService scheduleService;
    private ConsoleService consoleService;

    @Before public void setUp() throws Exception {
        timeProvider = mock(TimeProvider.class);
        systemEnvironment = mock(SystemEnvironment.class);
        serverHealthService = mock(ServerHealthService.class);
        jobInstanceService = mock(JobInstanceService.class);
        scheduleService = mock(ScheduleService.class);
        goConfigService = mock(GoConfigService.class);
        consoleService = mock(ConsoleService.class);

        when(goConfigService.getUnresponsiveJobTerminationThreshold(any(JobIdentifier.class))).thenReturn(UNRESPONSIVE_JOB_KILL_THRESHOLD);//5 mins
        when(systemEnvironment.getUnresponsiveJobWarningThreshold()).thenReturn(UNRESPONSIVE_JOB_WARNING_THRESHOLD);//2 mins
        when(goConfigService.canCancelJobIfHung(any(JobIdentifier.class))).thenReturn(true);

        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                listener = (ConsoleActivityMonitor.ActiveJobListener) invocation.getArguments()[0];
                return null;
            }
        }).when(jobInstanceService).registerJobStateChangeListener(Mockito.any(JobStatusListener.class));
        consoleActivityMonitor = new ConsoleActivityMonitor(timeProvider, systemEnvironment, jobInstanceService, serverHealthService, goConfigService, consoleService);
        consoleActivityMonitor.populateActivityMap();
        stubInitializerCallsForActivityMonitor(jobInstanceService);
    }

    private void stubInitializerCallsForActivityMonitor(final JobInstanceService jobInstanceService) {
        verify(jobInstanceService).registerJobStateChangeListener(Mockito.any(JobStatusListener.class));
        verify(jobInstanceService).allBuildingJobs();
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(jobInstanceService);
    }

    @Test
    public void shouldOnlyMonitorConsoleActivityForBuildingJobs() {
        when(timeProvider.currentTimeMillis()).thenReturn(new DateTime(1971, 1, 1, 0, 55, 59, 0).getMillis());
        JobIdentifier unresponsiveJob = new JobIdentifier("pipelines", 10, "label-10", "stage", "3", "job", 25l);
        consoleActivityMonitor.consoleUpdatedFor(unresponsiveJob);

        when(timeProvider.currentTimeMillis()).thenReturn(new DateTime(1972, 1, 1, 1, 5, 0, 0).getMillis());

        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verifyNoMoreInteractions(jobInstanceService);
        verifyNoMoreInteractions(scheduleService);
    }

    @Test
    public void shouldCancelUnresponsiveJobs() {
        JobIdentifier unresponsiveJob = new JobIdentifier("pipelines", 10, "label-10", "stage", "3", "job", 25l);
        listener.jobStatusChanged(buildingInstance(unresponsiveJob));

        JobIdentifier responsiveJob = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20l);
        listener.jobStatusChanged(buildingInstance(responsiveJob));

        when(timeProvider.currentTimeMillis()).thenReturn(new DateTime(1971, 1, 1, 0, 55, 59, 0).getMillis());

        consoleActivityMonitor.consoleUpdatedFor(unresponsiveJob);

        consoleActivityMonitor.consoleUpdatedFor(responsiveJob);
        when(timeProvider.currentTimeMillis()).thenReturn(new DateTime(1972, 1, 1, 1, 0, 0, 0).getMillis());
        consoleActivityMonitor.consoleUpdatedFor(responsiveJob);

        when(timeProvider.currentTimeMillis()).thenReturn(new DateTime(1972, 1, 1, 1, 1, 0, 0).getMillis());
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

        verify(scheduleService).cancelJob(unresponsiveJob);

        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verifyNoMoreInteractions(jobInstanceService);
    }


    @Test
    public void shouldCancelUnresponsiveJobAndLetOtherJobComplete() {
        JobIdentifier unresponsiveJob = new JobIdentifier("pipelines", 10, "label-10", "stage", "3", "job", 25l);
        listener.jobStatusChanged(buildingInstance(unresponsiveJob));

        JobIdentifier responsiveJob = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20l);
        listener.jobStatusChanged(buildingInstance(responsiveJob));

        when(timeProvider.currentTimeMillis()).thenReturn(new DateTime(1971, 1, 1, 0, 55, 59, 0).getMillis());

        consoleActivityMonitor.consoleUpdatedFor(unresponsiveJob);

        consoleActivityMonitor.consoleUpdatedFor(responsiveJob);
        when(timeProvider.currentTimeMillis()).thenReturn(new DateTime(1972, 1, 1, 1, 0, 0, 0).getMillis());
        consoleActivityMonitor.consoleUpdatedFor(responsiveJob);

        when(timeProvider.currentTimeMillis()).thenReturn(new DateTime(1972, 1, 1, 1, 1, 0, 0).getMillis());
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

        verify(scheduleService).cancelJob(unresponsiveJob);

        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verifyNoMoreInteractions(jobInstanceService);
    }

    @Test
    public void shouldNotCancelJobIfJobTimeOutIsSetTo0() {
        JobIdentifier unresponsiveJob = new JobIdentifier("pipelines", 10, "label-10", "stage", "3", "job", 25l);
        listener.jobStatusChanged(buildingInstance(unresponsiveJob));

        when(timeProvider.currentTimeMillis()).thenReturn(new DateTime(1971, 1, 1, 0, 55, 59, 0).getMillis());
        consoleActivityMonitor.consoleUpdatedFor(unresponsiveJob);

        when(timeProvider.currentTimeMillis()).thenReturn(new DateTime(1972, 1, 1, 1, 1, 0, 0).getMillis());
        when(goConfigService.canCancelJobIfHung(unresponsiveJob)).thenReturn(false);

        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

        verify(scheduleService, never()).cancelJob(unresponsiveJob);

        verifyNoMoreInteractions(jobInstanceService);
    }

    @Test
    public void shouldNotTrackConsoleActivityFor_completedJob() {
        JobIdentifier jobId = new JobIdentifier("foo-pipeline", 10, "foo-10", "bar-stage", "20", "baz-build");
        JobInstance job = buildingInstance(jobId);

        listener.jobStatusChanged(job);
        Date jobStartAndCompleteTime = new Date();
        when(timeProvider.currentTimeMillis()).thenReturn(jobStartAndCompleteTime.getTime());

        consoleActivityMonitor.consoleUpdatedFor(jobId);
        job.completing(JobResult.Passed);
        job.completed(new Date());
        listener.jobStatusChanged(job);
        
        when(timeProvider.currentTimeMillis()).thenReturn(new DateTime().plusDays(10).getMillis());

        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verifyNoMoreInteractions(jobInstanceService);
    }

    @Test
    public void shouldNotCancelCompletedJob_becauseOfActivityAfterCompletion() {
        DateTime now = new DateTime();
        when(timeProvider.currentTimeMillis()).thenReturn(now.getMillis());

        JobIdentifier jobId = new JobIdentifier("foo-pipeline", 10, "foo-10", "bar-stage", "20", "baz-build");
        JobInstance job = buildingInstance(jobId);

        listener.jobStatusChanged(job);
        job.completing(JobResult.Passed);
        job.completed(new Date());
        listener.jobStatusChanged(job);

        consoleActivityMonitor.consoleUpdatedFor(jobId); //Once a job is completed we should not track the console updates.

        when(timeProvider.currentTimeMillis()).thenReturn(now.plusDays(10).getMillis());
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verifyNoMoreInteractions(jobInstanceService);
    }

    private JobInstance buildingInstance(JobIdentifier responsiveJob) {
        JobInstance respJobInstance = new JobInstance();
        respJobInstance.setIdentifier(responsiveJob);
        respJobInstance.setState(JobState.Building);
        return respJobInstance;
    }

    @Test
    public void shouldConsiderAllBuildingJobsActiveOnInitialization() {
        long now = System.currentTimeMillis();
        when(timeProvider.currentTimeMillis()).thenReturn(now);

        JobIdentifier firstJob = new JobIdentifier("pipeline-foo", 10, "foo-10", "stage-bar", "12", "build");
        JobIdentifier secondJob = new JobIdentifier("pipeline-bar", 12, "bar-12", "stage-baz", "15", "quux");

        JobInstanceService jobInstanceService = mock(JobInstanceService.class);
        when(jobInstanceService.allBuildingJobs()).thenReturn(Arrays.asList(firstJob, secondJob));
        consoleActivityMonitor = new ConsoleActivityMonitor(timeProvider, systemEnvironment, jobInstanceService,
                serverHealthService, goConfigService, consoleService);
        consoleActivityMonitor.populateActivityMap();
        stubInitializerCallsForActivityMonitor(jobInstanceService);

        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verifyZeroInteractions(jobInstanceService);

        when(timeProvider.currentTimeMillis()).thenReturn(now + UNRESPONSIVE_JOB_KILL_THRESHOLD - 1);//just below threshold
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verifyZeroInteractions(jobInstanceService);

        when(timeProvider.currentTimeMillis()).thenReturn(now + UNRESPONSIVE_JOB_KILL_THRESHOLD + 1);//just above threshold
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verify(scheduleService).cancelJob(firstJob);
        verify(scheduleService).cancelJob(secondJob);
    }

    @Test
    public void shouldSetServerHealthMessageWhenJobSeemsUnresponsive() {
        DateTime now = new DateTime();
        when(timeProvider.currentTimeMillis()).thenReturn(now.getMillis());
        JobIdentifier job = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20l);
        listener.jobStatusChanged(buildingInstance(job));

        when(timeProvider.currentTimeMillis()).thenReturn(now.plusMinutes(2).plusSeconds(1).getMillis());//just over warning time limit i.e. 2 minutes
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

        verify(serverHealthService).update(ServerHealthState.warningWithHtml("Job 'foo/stage/job' is not responding",
                "Job <a href='/go/tab/build/detail/foo/12/stage/2/job'>foo/stage/job</a> is currently running but has not shown any console activity in the last 2 minute(s). This job may be hung.",
                HealthStateType.general(HealthStateScope.forJob("foo", "stage", "job"))));

        when(timeProvider.currentTimeMillis()).thenReturn(now.plusMinutes(4).plusSeconds(1).getMillis());//after 4 minutes
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

        verify(serverHealthService).update(ServerHealthState.warningWithHtml("Job 'foo/stage/job' is not responding",
                "Job <a href='/go/tab/build/detail/foo/12/stage/2/job'>foo/stage/job</a> is currently running but has not shown any console activity in the last 4 minute(s). This job may be hung.",
                HealthStateType.general(HealthStateScope.forJob("foo", "stage", "job"))));

        when(goConfigService.getUnresponsiveJobTerminationThreshold(any(JobIdentifier.class))).thenReturn(360 * 60 * 1000L);//6 hours
        when(timeProvider.currentTimeMillis()).thenReturn(now.plusHours(1).plusMinutes(2).plusSeconds(1).getMillis());//after 62 minutes
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

        verify(serverHealthService).update(ServerHealthState.warningWithHtml("Job 'foo/stage/job' is not responding",
                "Job <a href='/go/tab/build/detail/foo/12/stage/2/job'>foo/stage/job</a> is currently running but has not shown any console activity in the last 62 minute(s). This job may be hung.",
                HealthStateType.general(HealthStateScope.forJob("foo", "stage", "job"))));
    }

    @Test
    public void shouldClearServerHealthMessageWhenUnresponsiveJobShowsActivity() {
        DateTime now = new DateTime();
        when(timeProvider.currentTimeMillis()).thenReturn(now.getMillis());
        JobIdentifier responsiveJob = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20l);
        listener.jobStatusChanged(buildingInstance(responsiveJob));

        when(timeProvider.currentTimeMillis()).thenReturn(now.plusMinutes(2).plus(1).getMillis());
        consoleActivityMonitor.consoleUpdatedFor(responsiveJob);
        verify(serverHealthService).removeByScope(HealthStateScope.forJob("foo", "stage", "job"));
    }

    @Test
    public void shouldClearServerHealthMessageWhenUnresponsiveJobIsCancelled() {
        DateTime now = new DateTime();
        when(timeProvider.currentTimeMillis()).thenReturn(now.getMillis());
        JobIdentifier unresponsiveJob = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20l);
        listener.jobStatusChanged(buildingInstance(unresponsiveJob));
        when(timeProvider.currentTimeMillis()).thenReturn(now.plus(UNRESPONSIVE_JOB_KILL_THRESHOLD).plus(1).getMillis());
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);
        verify(scheduleService).cancelJob(unresponsiveJob);
        verify(serverHealthService).removeByScope(HealthStateScope.forJob("foo", "stage", "job"));
    }

    @Test
    public void shouldClearServerHealthMessageForAnyJobCancelledExternally() {
        DateTime now = new DateTime();
        when(timeProvider.currentTimeMillis()).thenReturn(now.getMillis());
        JobIdentifier unresponsiveJob = new JobIdentifier("foo", 12, "foo-10", "stage", "2", "job", 20l);
        JobInstance job = buildingInstance(unresponsiveJob);
        listener.jobStatusChanged(job);
        job.cancel();
        listener.jobStatusChanged(job);
        verify(serverHealthService).removeByScope(HealthStateScope.forJob("foo", "stage", "job"));
    }

    @Test
    public void shouldAppendToConsoleLog_JobKilledDueToInactivityMessage() throws IOException, IllegalArtifactLocationException {
        JobIdentifier unresponsiveJob = new JobIdentifier("pipelines", 10, "label-10", "stage", "3", "job", 25l);
        listener.jobStatusChanged(buildingInstance(unresponsiveJob));

        when(timeProvider.currentTimeMillis()).thenReturn(new DateTime(1971, 1, 1, 0, 55, 59, 0).getMillis());
        consoleActivityMonitor.consoleUpdatedFor(unresponsiveJob);

        when(timeProvider.currentTimeMillis()).thenReturn(new DateTime(1972, 1, 1, 1, 1, 0, 0).getMillis());
        consoleActivityMonitor.cancelUnresponsiveJobs(scheduleService);

        verify(consoleService).appendToConsoleLog(unresponsiveJob, "Go cancelled this job as it has not generated any console output for more than 5 minute(s)");
    }

    @Test
    public void shouldClearServerHealthMessageForARescheduledJob() {
        JobIdentifier rescheduledJobIdentifier = new JobIdentifier("foo", 10, "label-10", "stage", "3", "job", 10l);
        JobInstance jobInstance = new JobInstance();
        jobInstance.setIdentifier(rescheduledJobIdentifier);
        jobInstance.setState(JobState.Rescheduled);

        listener.jobStatusChanged(jobInstance);

        verify(serverHealthService).removeByScope(HealthStateScope.forJob("foo","stage","job"));
    }
}
