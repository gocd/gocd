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

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;

public class JobHistory extends BaseCollection<JobHistoryItem> {
    public Date getScheduledDate() {
        // All builds in a stage are scheduled at the same time, so this is safe.
        return this.first().getScheduledDate();
    }

    public JobHistory addJob(String name, JobState state, JobResult result, Date scheduledDate) {
        add(new JobHistoryItem(name, state, result, scheduledDate));
        return this;
    }

    public static JobHistory withJob(String name, JobState state, JobResult result, Date scheduledDate) {
        return new JobHistory().addJob(name, state, result, scheduledDate);
    }
}
