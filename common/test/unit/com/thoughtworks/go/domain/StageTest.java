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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.utils.Timeout;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;


public class StageTest {
    DateTime time1 = new DateTime(2008, 2, 22, 12, 22, 23, 0);
    DateTime time2 = new DateTime(2008, 2, 22, 12, 22, 24, 0);
    DateTime time3 = new DateTime(2008, 2, 22, 12, 22, 25, 0);
    DateTime time4 = new DateTime(2008, 2, 22, 12, 22, 26, 0);

    private JobInstances jobInstances;
    private Stage stage;
    private JobInstance firstJob;
    private JobInstance secondJob;
    private static final Date JOB_SCHEDULE_DATE = new Date();
    private TimeProvider timeProvider;
    private long nextId = 0;

    @Before
    public void setUp() {
        timeProvider = new TimeProvider() {
            @Override
            public Date currentTime() {
                return JOB_SCHEDULE_DATE;
            }

            public DateTime currentDateTime() {
                throw new UnsupportedOperationException("Not implemented");
            }

            public DateTime timeoutTime(Timeout timeout) {
                throw new UnsupportedOperationException("Not implemented");
            }
        };
        firstJob = new JobInstance("first-job", timeProvider);
        secondJob = new JobInstance("second-job", timeProvider);
        jobInstances = new JobInstances(firstJob, secondJob);
        stage = StageMother.custom("test", jobInstances);
    }

    @Test
    public void shouldUpdateCompletedByTransitionIdAndStageState() throws Exception {
        assertThat(stage.getCompletedByTransitionId(), is(nullValue()));
        DateTime fiveMinsForNow = new DateTime().plusMinutes(5);
        complete(firstJob, fiveMinsForNow);
        complete(secondJob, fiveMinsForNow);
        JobStateTransition lastTransition = secondJob.getTransition(JobState.Completed);
        stage.calculateResult();
        assertThat(stage.getCompletedByTransitionId(), is(nextId));
        assertThat(stage.getState(), is(StageState.Passed));
    }

    private void complete(JobInstance job, DateTime fiveMinsForNow) {
        job.completing(JobResult.Passed, fiveMinsForNow.toDate());
        job.completed(fiveMinsForNow.plusSeconds(10).toDate());
        assignIdsToAllTransitions(job);
    }

    private void assignIdsToAllTransitions(JobInstance job) {
        for (JobStateTransition jobStateTransition : job.getTransitions()) {
            jobStateTransition.setId(++nextId);
        }
    }

    @Test
    public void shouldReturnMostRecentCompletedTransitionAsCompletedDateIfLatestTransitionIdIsNot() {

        firstJob.assign("AGENT-1", time1.toDate());
        firstJob.completing(JobResult.Passed, time2.toDate());
        firstJob.completed(time2.toDate());

        secondJob.assign("AGENT-2", time3.toDate());
        secondJob.completing(JobResult.Passed, time4.toDate());
        secondJob.completed(time4.toDate());
        secondJob.getTransitions().byState(JobState.Completed).setId(1);

        stage.calculateResult();

        assertThat(stage.completedDate(), is(time4.toDate()));
    }

    @Test
    public void shouldReturnNullAsCompletedDateIfNeverCompleted() {
        firstJob.assign("AGENT-1", time1.toDate());
        secondJob.assign("AGENT-2", time3.toDate());

        assertNull("Completed date should be null", stage.completedDate());
    }

    @Test
    public void stageStateShouldBeUnkownIfNoJobs() {
        Stage newStage = new Stage();
        assertThat(newStage.stageState(), is(StageState.Unknown));
    }

    @Test
    public void shouldCalculateTotalTimeFromFirstScheduledJobToLastCompletedJob() {

        final DateTime time0 = new DateTime(2008, 2, 22, 10, 21, 23, 0);
        timeProvider = new TimeProvider() {
            @Override
            public Date currentTime() {
                return time0.toDate();
            }

            public DateTime currentDateTime() {
                throw new UnsupportedOperationException("Not implemented");
            }

            public DateTime timeoutTime(Timeout timeout) {
                throw new UnsupportedOperationException("Not implemented");
            }
        };

        firstJob = new JobInstance("first-job", timeProvider);
        secondJob = new JobInstance("second-job", timeProvider);

        jobInstances = new JobInstances(firstJob, secondJob);
        stage = StageMother.custom("test", jobInstances);

        firstJob.assign("AGENT-1", time1.toDate());
        firstJob.completing(JobResult.Passed, time2.toDate());
        firstJob.completed(time2.toDate());

        secondJob.assign("AGENT-2", time3.toDate());
        secondJob.completing(JobResult.Passed, time4.toDate());
        secondJob.completed(time4.toDate());

        stage.calculateResult();
        stage.setCreatedTime(new Timestamp(time0.toDate().getTime()));
        stage.setLastTransitionedTime(new Timestamp(time4.toDate().getTime()));

        RunDuration.ActualDuration expectedDuration = new RunDuration.ActualDuration(new Duration(time0, time4));
        RunDuration.ActualDuration duration = (RunDuration.ActualDuration) stage.getDuration();
        assertThat(duration, is(expectedDuration));
        assertThat(duration.getTotalSeconds(), is(7263L));
    }

    @Test
    public void shouldReturnZeroDurationForIncompleteStage() {
        firstJob.assign("AGENT-1", time1.toDate());
        firstJob.changeState(JobState.Building, time2.toDate());

        assertThat(stage.getDuration(), is(RunDuration.IN_PROGRESS_DURATION));
    }

    @Test
    public void shouldReturnLatestTransitionDate() {
        Date date = JOB_SCHEDULE_DATE;
        firstJob.completing(JobResult.Failed, date);
        assertThat(stage.latestTransitionDate(), is(date));
    }

    @Test
    public void shouldReturnCreatedDateWhenNoTranstions() throws Exception {
        stage = new Stage("dev", new JobInstances(), "anonymous", "manual", new TimeProvider());
        assertEquals(new Date(stage.getCreatedTime().getTime()), stage.latestTransitionDate());
    }

    @Test
    public void shouldCreateAStageWithAGivenConfigVersion() {
        Stage stage = new Stage("foo-stage", new JobInstances(), "admin", "manual", false, false, "git-sha", new TimeProvider());
        assertThat(stage.getConfigVersion(), is("git-sha"));

        stage = new Stage("foo-stage", new JobInstances(), "admin", "manual", new TimeProvider());
        assertThat(stage.getConfigVersion(), is(nullValue()));
    }

    @Test
    public void shouldBuildStageConfigIdentifierFromStageIdentifier()  {
        String stageName = "stage";
        String pipelineName = "pipeline";
        StageConfigIdentifier expectedResult = new StageConfigIdentifier(pipelineName, stageName);

        Stage stage = new Stage();
        stage.setIdentifier(new StageIdentifier(pipelineName, 1, stageName, "1"));

        StageConfigIdentifier stageConfigIdentifier = stage.getStageConfigIdentifier();
        assertThat(stageConfigIdentifier,is(expectedResult));
    }
}
