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
import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class JobStateTransition extends PersistentObject {

    private JobState currentState;
    private Date stateChangeTime;
    private long jobId;
    private long stageId;

    public JobStateTransition() {
    }

    public JobStateTransition(JobState current, Date stateChangeTime) {

        this.currentState = current;
        this.stateChangeTime = stateChangeTime;
    }


    public JobState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(JobState currentState) {
        this.currentState = currentState;
    }

    public Date getStateChangeTime() {
        return stateChangeTime;
    }

    public void setStateChangeTime(Date stateChangeTime) {
        this.stateChangeTime = stateChangeTime;
    }

    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JobStateTransition that = (JobStateTransition) o;

        if (jobId != that.jobId) {
            return false;
        }
        if (currentState != that.currentState) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = Long.valueOf(jobId).hashCode();
        result = 31 * result + (currentState != null ? currentState.hashCode() : 0);
        result = 31 * result + (stateChangeTime != null ? stateChangeTime.hashCode() : 0);
        result = 31 * result + (int) (jobId ^ (jobId >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(long stageId) {
        this.stageId = stageId;
    }
}
