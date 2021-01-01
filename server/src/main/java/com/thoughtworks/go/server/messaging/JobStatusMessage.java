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
package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.StageIdentifier;

public class JobStatusMessage implements GoMessage {
    private JobIdentifier jobIdentifier;
    private JobState state;
    private String agentUuid;

    public JobStatusMessage(JobIdentifier jobIdentifier, JobState state, String agentUuid) {
        this.jobIdentifier = jobIdentifier;
        this.state = state;
        this.agentUuid = agentUuid;
    }

    public JobIdentifier getJobIdentifier() {
        return jobIdentifier;
    }

    public JobState getJobState() {
        return state;
    }

    @Override
    public String toString() {
        return String.format("[JobStatusMessage: %s %s %s]", agentUuid, jobIdentifier, state);
    }

    public StageIdentifier getStageIdentifier() {
        return jobIdentifier.getStageIdentifier();
    }

    public String getAgentUuid() {
        return agentUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobStatusMessage that = (JobStatusMessage) o;

        if (agentUuid != null ? !agentUuid.equals(that.agentUuid) : that.agentUuid != null) {
            return false;
        }
        if (jobIdentifier != null ? !jobIdentifier.equals(that.jobIdentifier) : that.jobIdentifier != null) {
            return false;
        }
        if (state != that.state) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (jobIdentifier != null ? jobIdentifier.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (agentUuid != null ? agentUuid.hashCode() : 0);
        return result;
    }
}
