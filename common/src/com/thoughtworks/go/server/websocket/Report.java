/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.websocket;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;

import java.io.Serializable;

public class Report implements Serializable {
    private AgentRuntimeInfo agentRuntimeInfo;
    private JobIdentifier jobIdentifier;
    private JobResult result;
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
}
