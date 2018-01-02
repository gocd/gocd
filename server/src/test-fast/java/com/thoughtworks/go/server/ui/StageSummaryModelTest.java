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

package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.domain.Stages;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.StageMother;
import static com.thoughtworks.go.helper.StageMother.completedFailedStageInstance;
import static com.thoughtworks.go.helper.StageMother.custom;

import com.thoughtworks.go.server.domain.JobDurationStrategy;
import static org.hamcrest.core.Is.is;

import org.joda.time.Duration;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class StageSummaryModelTest {
    private static final JobDurationStrategy JOB_DURATION_STRATEGY = JobDurationStrategy.ALWAYS_ZERO;

    @Test public void shouldReturnInProgressWhenTheDurationIs0() throws Exception {
        Stage stage = StageMother.scheduledStage("pipeline-name", 1, "stage", 1, "job");
        StageSummaryModel stageSummaryModel = new StageSummaryModel(stage, new Stages(), JOB_DURATION_STRATEGY, null);
        assertThat(stageSummaryModel.getDuration(), is("In Progress"));
    }

    @Test public void shouldReturn0ForAFailedStage0() throws Exception {
        Stage stage = completedFailedStageInstance("pipeline-name", "stage", "job");
        StageSummaryModel stageSummaryModel = new StageSummaryModel(stage, new Stages(), JOB_DURATION_STRATEGY, null);
        assertThat(stageSummaryModel.getDuration(), is("00:00:00"));
    }

    @Test public void shouldReturnTotalRuns() throws Exception {
        Stage failed = completedFailedStageInstance("pipeline-name", "stage", "job");
        failed.setCounter(1);
        Stage passed = custom("stage");
        passed.setCounter(2);
        StageSummaryModel stageSummaryModel = new StageSummaryModel(failed, new Stages(failed, passed), JOB_DURATION_STRATEGY, null);
        assertThat(stageSummaryModel.getTotalRuns(),is(2));
        assertThat(stageSummaryModel.getStateForRun(1),is(StageState.Failed));
        assertThat(stageSummaryModel.getStateForRun(2),is(StageState.Passed));
    }

    @Test
    public void shouldReturnJobsForAGivenResult() throws Exception {
        JobInstance first = JobInstanceMother.completed("first", JobResult.Failed);
        JobInstance second = JobInstanceMother.completed("bsecond", JobResult.Passed);
        JobInstance third = JobInstanceMother.completed("athird", JobResult.Passed);
        JobInstance fourth = JobInstanceMother.building("fourth");
        JobInstance fifth = JobInstanceMother.completed("fifth", JobResult.Cancelled);
        Stage stage = StageMother.custom("pipeline", "stage", new JobInstances(first, second, third, fourth, fifth));

        StageSummaryModel model = new StageSummaryModel(stage, new Stages(stage), JOB_DURATION_STRATEGY, null);
        assertThat(model.passedJobs().size(), is(2));
        assertThat(model.passedJobs().get(0).getName(), is(third.getName()));
        assertThat(model.passedJobs().get(1).getName(), is(second.getName()));

        assertThat(model.nonPassedJobs().size(), is(2));
        assertThat(model.nonPassedJobs().get(0).getName(), is(fifth.getName()));
        assertThat(model.nonPassedJobs().get(1).getName(), is(first.getName()));

        assertThat(model.inProgressJobs().size(), is(1));
        assertThat(model.inProgressJobs().get(0).getName(), is(fourth.getName()));

    }

    @Test
    public void shouldRetriveShowElapsedTime() throws Exception {
        JobInstance first = JobInstanceMother.completed("first", JobResult.Failed);
        Stage stage = StageMother.custom("pipeline", "stage", new JobInstances(first));
        StageSummaryModel model = new StageSummaryModel(stage, new Stages(stage), JOB_DURATION_STRATEGY, null);
        assertThat(model.nonPassedJobs().get(0).getElapsedTime(), is(first.getElapsedTime()));
    }

    @Test
    public void shouldRetrivePercentCompleteOnJobs() throws Exception {
        JobInstance first = JobInstanceMother.completed("first", JobResult.Failed);
        Stage stage = StageMother.custom("pipeline", "stage", new JobInstances(first));
        StageSummaryModel model = new StageSummaryModel(stage, new Stages(stage), new JobDurationStrategy.ConstantJobDuration(1000 * 1000), null);
        assertThat(model.nonPassedJobs().get(0).getElapsedTime(), is(new Duration(120 * 1000)));
        assertThat(model.nonPassedJobs().get(0).getPercentComplete(), is(12));
    }

    @Test
    public void shouldExplainWhetherJobIsComplete() throws Exception {
        JobInstance first = JobInstanceMother.completed("first", JobResult.Failed);
        Stage stage = StageMother.custom("pipeline", "stage", new JobInstances(first));
        StageSummaryModel model = new StageSummaryModel(stage, new Stages(stage), JOB_DURATION_STRATEGY, null);
        assertThat(model.nonPassedJobs().get(0).isCompleted(), is(true));
    }

    @Test
    public void shouldGetPipelineCounter() throws Exception {
        JobInstance first = JobInstanceMother.completed("first", JobResult.Failed);
        Stage stage = StageMother.custom("pipeline", "stage", new JobInstances(first));
        StageSummaryModel model = new StageSummaryModel(stage, new Stages(stage), JOB_DURATION_STRATEGY, stage.getIdentifier());
        assertThat(model.getPipelineCounter(), is(stage.getIdentifier().getPipelineCounter()));
    }

}
