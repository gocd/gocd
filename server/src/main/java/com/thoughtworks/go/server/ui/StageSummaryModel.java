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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.RunDuration;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.domain.Stages;
import com.thoughtworks.go.server.domain.JobDurationStrategy;

public class StageSummaryModel {

    private final Stage stage;
    private final Stages stages;
    private final JobDurationStrategy jobDurationStrategy;
    private StageIdentifier lastStageIdentifier;

    public StageSummaryModel(Stage stage, Stages stages, JobDurationStrategy jobDurationStrategy, StageIdentifier lastStageIdentifier) {
        this.stage = stage;
        this.stages = stages;
        this.jobDurationStrategy = jobDurationStrategy;
        this.lastStageIdentifier = lastStageIdentifier;
    }

    public StageIdentifier getIdentifier() {
        return stage.getIdentifier();
    }

    public Stage getStage() {
        return stage;
    }

    public String getApprovedBy() {
        return stage.getApprovedBy();
    }

    public long getId() {
        return stage.getId();
    }

    public JobInstances getJobs() {
        return stage.getJobInstances();
    }

    public String getName() {
        return stage.getName();
    }

    public String getPipelineName() {
        return getIdentifier().getPipelineName();
    }

    public String getPipelineLabel() {
        return getIdentifier().getPipelineLabel();
    }

    public Integer getPipelineCounter() {
        return getIdentifier().getPipelineCounter();
    }

    public long getPipelineId() {
        return stage.getPipelineId();
    }

    public String getStageCounter() {
        return getIdentifier().getStageCounter();
    }

    public StageIdentifier getLastSuccessful() {
        return lastStageIdentifier;
    }

    public StageState getState() {
        return stage.stageState();
    }

    public String getDuration() {
        return stage.getDuration().duration(RunDuration.PERIOD_FORMATTER);
    }

    public RunDuration.ActualDuration getActualDuration(){
        return (RunDuration.ActualDuration) stage.getDuration();
    }

    public Date getCreatedTime() {
        return new Date(stage.getCreatedTime().getTime());
    }

    public int getTotalRuns() {
        return stages.size();
    }

    public StageState getStateForRun(int stageCounter) {
        return stages.byCounter(stageCounter).stageState();
    }

    public List<JobInstanceModel> passedJobs() {
        return summarize(stage.jobsWithResult(JobResult.Passed).sortByName());
    }

    public List<JobInstanceModel> nonPassedJobs() {
        return summarize(stage.jobsWithResult(JobResult.Failed, JobResult.Cancelled).sortByName());
    }

    public List<JobInstanceModel> inProgressJobs() {
        return summarize(stage.jobsWithResult(JobResult.Unknown).sortByName());
    }

    public boolean isActive(){
        return stage.isActive();
    }

    private List<JobInstanceModel> summarize(JobInstances jobInstances) {
        ArrayList<JobInstanceModel> models = new ArrayList<>();
        for (JobInstance jobInstance : jobInstances) {
            models.add(new JobInstanceModel(jobInstance, jobDurationStrategy));
        }
        return models;
    }

}
