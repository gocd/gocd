/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.websocket;

import com.google.gson.annotations.Expose;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;

import java.io.Serializable;

public class Report implements Serializable {
    @Expose
    private String buildId;
    @Expose
    private AgentRuntimeInfo agentRuntimeInfo;
    @Expose
    private JobIdentifier jobIdentifier;
    @Expose
    private JobResult result;
    @Expose
    private JobState jobState;

    public Report(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobState jobState) {
        this.agentRuntimeInfo = agentRuntimeInfo;
        this.jobIdentifier = jobIdentifier;
        this.jobState = jobState;
    }

    public Report(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobResult result) {
        this.agentRuntimeInfo = agentRuntimeInfo;
        this.jobIdentifier = jobIdentifier;
        this.result = result;
    }

    public Report(AgentRuntimeInfo agentRuntimeInfo, String buildId, JobState jobState, JobResult result) {
        this.agentRuntimeInfo = agentRuntimeInfo;
        this.jobState = jobState;
        this.buildId = buildId;
        this.result = result;
    }

    public JobState getJobState() {
        return jobState;
    }

    public JobIdentifier getJobIdentifier() {
        return jobIdentifier;
    }

    public AgentRuntimeInfo getAgentRuntimeInfo() {
        return agentRuntimeInfo;
    }

    public JobResult getResult() {
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Report report = (Report) o;

        if (buildId != null ? !buildId.equals(report.buildId) : report.buildId != null) return false;
        if (!agentRuntimeInfo.equals(report.agentRuntimeInfo)) return false;
        if (jobIdentifier != null ? !jobIdentifier.equals(report.jobIdentifier) : report.jobIdentifier != null)
            return false;
        if (result != report.result) return false;
        return jobState == report.jobState;

    }

    @Override
    public int hashCode() {
        int result1 = buildId != null ? buildId.hashCode() : 0;
        result1 = 31 * result1 + agentRuntimeInfo.hashCode();
        result1 = 31 * result1 + (jobIdentifier != null ? jobIdentifier.hashCode() : 0);
        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
        result1 = 31 * result1 + (jobState != null ? jobState.hashCode() : 0);
        return result1;
    }

    @Override
    public String toString() {
        return "Report{" +
                "buildId='" + buildId + '\'' +
                ", agentRuntimeInfo=" + agentRuntimeInfo +
                ", jobIdentifier=" + jobIdentifier +
                ", result=" + result +
                ", jobState=" + jobState +
                '}';
    }

    public String getBuildId() {
        return buildId;
    }
}
