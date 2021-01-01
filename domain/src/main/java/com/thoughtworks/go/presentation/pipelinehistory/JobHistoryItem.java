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
package com.thoughtworks.go.presentation.pipelinehistory;

import java.util.Date;

import com.thoughtworks.go.domain.BuildStateAware;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.JobResult;

public class JobHistoryItem implements BuildStateAware {
    private long id;
    private String name;
    private JobState state;
    private JobResult result;
    private Date scheduledDate = new Date();

    private JobHistoryItem() {
    }

    public JobHistoryItem(String name, JobState state, JobResult result, Date scheduledDate) {
        if (scheduledDate == null) { throw new IllegalArgumentException("Scheduled date cannot be null"); }
        this.name = name;
        this.state = state;
        this.result = result;
        this.scheduledDate = scheduledDate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
    }

    @Override
    public JobResult getResult() {
        return result;
    }

    public void setResult(JobResult result) {
        this.result = result;
    }

    public Date getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(Date scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public boolean hasUnsuccessfullyCompleted() {
        return result == JobResult.Cancelled || result == JobResult.Failed;
    }

    public boolean hasPassed() {
        return result == JobResult.Passed;
    }

    public boolean hasFailed() {
        return result.isFailed();
    }

    public boolean isRunning() {
        return state == JobState.Assigned || state == JobState.Preparing || state == JobState.Building || state == JobState.Completing || state == JobState.Scheduled;
    }
}
