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
package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.domain.JobDurationStrategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.thoughtworks.go.helper.StageMother.completedFailedStageInstance;
import static com.thoughtworks.go.helper.StageMother.custom;
import static org.assertj.core.api.Assertions.assertThat;

public class StageSummaryModelTest {
    private static final JobDurationStrategy JOB_DURATION_STRATEGY = JobDurationStrategy.ALWAYS_ZERO;

    @Test
    public void shouldReturnInProgressWhenTheDurationIs0() {
        Stage stage = StageMother.scheduledStage("pipeline-name", 1, "stage", 1, "job");
        StageSummaryModel stageSummaryModel = new StageSummaryModel(stage, new Stages(), JOB_DURATION_STRATEGY, null);
        assertThat(stageSummaryModel.getDuration()).isEqualTo("In Progress");
    }

    @Test
    public void shouldReturn0ForAFailedStage0() {
        Stage stage = completedFailedStageInstance("pipeline-name", "stage", "job");
        StageSummaryModel stageSummaryModel = new StageSummaryModel(stage, new Stages(), JOB_DURATION_STRATEGY, null);
        assertThat(stageSummaryModel.getDuration()).isEqualTo("00:00:00");
    }

    @Test
    public void shouldReturnTotalRuns() {
        Stage failed = completedFailedStageInstance("pipeline-name", "stage", "job");
        failed.setCounter(1);
        Stage passed = custom("stage");
        passed.setCounter(2);
        StageSummaryModel stageSummaryModel = new StageSummaryModel(failed, new Stages(failed, passed), JOB_DURATION_STRATEGY, null);
        assertThat(stageSummaryModel.getTotalRuns()).isEqualTo(2);
        assertThat(stageSummaryModel.getStateForRun(1)).isEqualTo(StageState.Failed);
        assertThat(stageSummaryModel.getStateForRun(2)).isEqualTo(StageState.Passed);
        assertThat(stageSummaryModel.getCancelledByForRun(1)).isNull();
        assertThat(stageSummaryModel.getCancelledByForRun(2)).isNull();
    }

    @Test
    public void shouldReturnJobsForAGivenResult() {
        JobInstance first = JobInstanceMother.completed("first", JobResult.Failed);
        JobInstance second = JobInstanceMother.completed("bsecond", JobResult.Passed);
        JobInstance third = JobInstanceMother.completed("athird", JobResult.Passed);
        JobInstance fourth = JobInstanceMother.building("fourth");
        JobInstance fifth = JobInstanceMother.completed("fifth", JobResult.Cancelled);
        Stage stage = StageMother.custom("pipeline", "stage", new JobInstances(first, second, third, fourth, fifth));

        StageSummaryModel model = new StageSummaryModel(stage, new Stages(stage), JOB_DURATION_STRATEGY, null);
        assertThat(model.passedJobs().size()).isEqualTo(2);
        assertThat(model.passedJobs().get(0).getName()).isEqualTo(third.getName());
        assertThat(model.passedJobs().get(1).getName()).isEqualTo(second.getName());

        assertThat(model.nonPassedJobs().size()).isEqualTo(2);
        assertThat(model.nonPassedJobs().get(0).getName()).isEqualTo(fifth.getName());
        assertThat(model.nonPassedJobs().get(1).getName()).isEqualTo(first.getName());

        assertThat(model.inProgressJobs().size()).isEqualTo(1);
        assertThat(model.inProgressJobs().get(0).getName()).isEqualTo(fourth.getName());

    }

    @Test
    public void shouldRetrieveShowElapsedTime() {
        JobInstance first = JobInstanceMother.completed("first", JobResult.Failed);
        Stage stage = StageMother.custom("pipeline", "stage", new JobInstances(first));
        StageSummaryModel model = new StageSummaryModel(stage, new Stages(stage), JOB_DURATION_STRATEGY, null);
        assertThat(model.nonPassedJobs().get(0).getElapsedTime()).isEqualTo(first.getElapsedTime());
    }

    @Test
    public void shouldRetrievePercentCompleteOnJobs() {
        JobInstance first = JobInstanceMother.completed("first", JobResult.Failed);
        Stage stage = StageMother.custom("pipeline", "stage", new JobInstances(first));
        StageSummaryModel model = new StageSummaryModel(stage, new Stages(stage), new JobDurationStrategy.ConstantJobDuration(Duration.ofSeconds(1000)), null);
        assertThat(model.nonPassedJobs().get(0).getElapsedTime()).isEqualTo(Duration.ofSeconds(120));
        assertThat(model.nonPassedJobs().get(0).getPercentComplete()).isEqualTo(12);
    }

    @Test
    public void shouldExplainWhetherJobIsComplete() {
        JobInstance first = JobInstanceMother.completed("first", JobResult.Failed);
        Stage stage = StageMother.custom("pipeline", "stage", new JobInstances(first));
        StageSummaryModel model = new StageSummaryModel(stage, new Stages(stage), JOB_DURATION_STRATEGY, null);
        assertThat(model.nonPassedJobs().get(0).isCompleted()).isTrue();
    }

    @Test
    public void shouldGetPipelineCounter() {
        JobInstance first = JobInstanceMother.completed("first", JobResult.Failed);
        Stage stage = StageMother.custom("pipeline", "stage", new JobInstances(first));
        StageSummaryModel model = new StageSummaryModel(stage, new Stages(stage), JOB_DURATION_STRATEGY, stage.getIdentifier());
        assertThat(model.getPipelineCounter()).isEqualTo(stage.getIdentifier().getPipelineCounter());
    }

}
