/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.JobResult;

public class JobResultMessage implements GoMessage {
    private JobIdentifier jobIdentifier;
    private JobResult result;
    private final String agentUuid;

    public JobResultMessage(JobIdentifier jobIdentifier, JobResult result, String agentUuid) {
        this.jobIdentifier = jobIdentifier;
        this.result = result;
        this.agentUuid = agentUuid;
    }

    public JobIdentifier getJobIdentifier() {
        return jobIdentifier;
    }

    @Override
    public String toString() {
        return String.format("[JobResultMessage: %s %s %s]", jobIdentifier, result, agentUuid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobResultMessage that = (JobResultMessage) o;

        if (agentUuid != null ? !agentUuid.equals(that.agentUuid) : that.agentUuid != null) {
            return false;
        }
        if (jobIdentifier != null ? !jobIdentifier.equals(that.jobIdentifier) : that.jobIdentifier != null) {
            return false;
        }
        if (result != that.result) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result1;
        result1 = (jobIdentifier != null ? jobIdentifier.hashCode() : 0);
        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
        result1 = 31 * result1 + (agentUuid != null ? agentUuid.hashCode() : 0);
        return result1;
    }

    public String getAgentUuid() {
        return agentUuid;
    }
}
