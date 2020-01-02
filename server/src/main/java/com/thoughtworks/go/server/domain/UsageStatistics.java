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
package com.thoughtworks.go.server.domain;

import java.util.Map;

public class UsageStatistics {
    private final long pipelineCount;
    private final long configRepoPipelineCount;
    private final long agentCount;
    private final long jobCount;
    private final Map<String, Long> elasticAgentPluginToJobCount;
    private final long oldestPipelineExecutionTime;
    private final String serverId;
    private final String gocdVersion;
    private Boolean addCTA;
    private Boolean saveAndRunCTA;
    private Boolean testDrive;

    private UsageStatistics(Builder builder) {
        this.pipelineCount = builder.pipelineCount;
        this.agentCount = builder.agentCount;
        this.oldestPipelineExecutionTime = builder.oldestPipelineExecutionTime;
        this.serverId = builder.serverId;
        this.gocdVersion = builder.gocdVersion;
        this.configRepoPipelineCount = builder.configRepoPipelineCount;
        this.jobCount = builder.jobCount;
        this.elasticAgentPluginToJobCount = builder.elasticAgentPluginToJobCount;
        this.addCTA = builder.addCTA;
        this.saveAndRunCTA = builder.saveAndRunCTA;
        this.testDrive = builder.testDrive;
    }

    public static Builder newUsageStatistics() {
        return new Builder();
    }

    public Boolean testDrive() {
        return testDrive;
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

    public long configRepoPipelineCount() {
        return configRepoPipelineCount;
    }

    public long agentCount() {
        return agentCount;
    }

    public long oldestPipelineExecutionTime() {
        return oldestPipelineExecutionTime;
    }

    public long jobCount() {
        return jobCount;
    }

    public Boolean addCTA() {
        return addCTA;
    }

    public Boolean saveAndRunCTA() {
        return saveAndRunCTA;
    }

    public Map<String, Long> elasticAgentPluginToJobCount() {
        return elasticAgentPluginToJobCount;
    }

    public static final class Builder {
        private long pipelineCount;
        private long agentCount;
        private long oldestPipelineExecutionTime;
        private String serverId;
        private String gocdVersion;
        private long configRepoPipelineCount;
        private long jobCount;
        private Map<String, Long> elasticAgentPluginToJobCount;
        private Boolean addCTA;
        private Boolean saveAndRunCTA;
        private Boolean testDrive;

        private Builder() {
        }

        public UsageStatistics build() {
            return new UsageStatistics(this);
        }

        public Builder addCTA(Boolean addCTA) {
            this.addCTA = addCTA;
            return this;
        }

        public Builder testDrive(Boolean testDrive) {
            this.testDrive = testDrive;
            return this;
        }

        public Builder saveAndRunCTA(Boolean saveAndRunCTA) {
            this.saveAndRunCTA = saveAndRunCTA;
            return this;
        }

        public Builder pipelineCount(long pipelineCount) {
            this.pipelineCount = pipelineCount;
            return this;
        }

        public Builder agentCount(long agentCount) {
            this.agentCount = agentCount;
            return this;
        }

        public Builder oldestPipelineExecutionTime(long oldestPipelineExecutionTime) {
            this.oldestPipelineExecutionTime = oldestPipelineExecutionTime;
            return this;
        }

        public Builder serverId(String serverId) {
            this.serverId = serverId;
            return this;
        }

        public Builder gocdVersion(String gocdVersion) {
            this.gocdVersion = gocdVersion;
            return this;
        }

        public Builder configRepoPipelineCount(long configRepoPipelineCount) {
            this.configRepoPipelineCount = configRepoPipelineCount;
            return this;
        }

        public Builder jobCount(long jobCount) {
            this.jobCount = jobCount;
            return this;
        }

        public Builder elasticAgentPluginToJobCount(Map<String, Long> elasticAgentPluginToJobCount) {
            this.elasticAgentPluginToJobCount = elasticAgentPluginToJobCount;
            return this;
        }
    }
}
