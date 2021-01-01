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
package com.thoughtworks.go.server.perf.commands;

public class AgentPerformanceCommandResult {
    private String name;
    private String status;
    private String failureMessage;
    private String agentUuids;
    private long timeTakenInMillis;

    public String getName() {
        return name;
    }

    public AgentPerformanceCommandResult setName(String name) {
        this.name = name;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public AgentPerformanceCommandResult setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public AgentPerformanceCommandResult setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
        return this;
    }

    public String getAgentUuids() {
        return agentUuids;
    }

    public AgentPerformanceCommandResult setAgentUuids(String agentUuids) {
        this.agentUuids = agentUuids;
        return this;
    }

    public long getTimeTakenInMillis() {
        return timeTakenInMillis;
    }

    public AgentPerformanceCommandResult setTimeTakenInMillis(long timeTakenInMillis) {
        this.timeTakenInMillis = timeTakenInMillis;
        return this;
    }
}
