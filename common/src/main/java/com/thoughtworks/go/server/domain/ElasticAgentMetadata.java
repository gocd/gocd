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

import com.thoughtworks.go.domain.AgentConfigStatus;
import com.thoughtworks.go.domain.AgentRuntimeStatus;

public class ElasticAgentMetadata {
    private final String uuid;
    private final String elasticAgentId;
    private final String elasticPluginId;
    private final AgentRuntimeStatus status;
    private final AgentConfigStatus configStatus;

    public ElasticAgentMetadata(String uuid, String elasticAgentId, String elasticPluginId, AgentRuntimeStatus status, AgentConfigStatus configStatus) {
        this.uuid = uuid;
        this.elasticAgentId = elasticAgentId;
        this.elasticPluginId = elasticPluginId;
        this.status = status;
        this.configStatus = configStatus;
    }

    public AgentRuntimeStatus agentState() {
        return status.agentState();
    }

    public AgentRuntimeStatus buildState() {
        return status.buildState();
    }

    public AgentConfigStatus configStatus() {
        return configStatus;
    }

    public String elasticAgentId() {
        return elasticAgentId;
    }

    public String elasticPluginId() {
        return elasticPluginId;
    }

    public String uuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return "ElasticAgentMetadata{" +
                "uuid='" + uuid + '\'' +
                ", elasticAgentId='" + elasticAgentId + '\'' +
                ", elasticPluginId='" + elasticPluginId + '\'' +
                ", status=" + status +
                ", configStatus=" + configStatus +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ElasticAgentMetadata that = (ElasticAgentMetadata) o;

        if (!uuid.equals(that.uuid)) return false;
        if (!elasticAgentId.equals(that.elasticAgentId)) return false;
        if (!elasticPluginId.equals(that.elasticPluginId)) return false;
        if (status != that.status) return false;
        return configStatus == that.configStatus;
    }

    @Override
    public int hashCode() {
        int result = uuid.hashCode();
        result = 31 * result + elasticAgentId.hashCode();
        result = 31 * result + elasticPluginId.hashCode();
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (configStatus != null ? configStatus.hashCode() : 0);
        return result;
    }
}
