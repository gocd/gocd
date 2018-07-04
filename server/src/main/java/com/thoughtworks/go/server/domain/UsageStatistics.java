/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.domain;

public class UsageStatistics {
    private final long pipelineCount;
    private final long agentCount;
    private final Long oldestPipelineExecutionTime;
    private final String serverId;
    private final String gocdVersion;

    public UsageStatistics(Long pipelineCount, Long agentCount, Long oldestPipelineExecutionTime, String serverId, String gocdVersion) {
        this.pipelineCount = pipelineCount;
        this.agentCount = agentCount;
        this.oldestPipelineExecutionTime = oldestPipelineExecutionTime != null ? oldestPipelineExecutionTime : 0l;
        this.serverId = serverId;
        this.gocdVersion = gocdVersion;
    }

    public String serverId() {
        return serverId;
    }

    public String gocdVersion() {
        return gocdVersion;
    }

    public long pipelineCount() {
        return pipelineCount;
    }

    public long agentCount() {
        return agentCount;
    }

    public Long oldestPipelineExecutionTime() {
        return oldestPipelineExecutionTime;
    }
}
