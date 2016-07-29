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

package com.thoughtworks.go.server.service;

import com.google.gson.annotations.Expose;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.remote.AgentIdentifier;

import java.io.Serializable;

public class ElasticAgentRuntimeInfo extends AgentRuntimeInfo implements Serializable {
    @Expose
    private volatile String elasticAgentId;
    @Expose
    private volatile String elasticPluginId;

    private ElasticAgentRuntimeInfo(AgentRuntimeInfo runtimeInfo, String elasticAgentId, String elasticPluginId) {
        this(runtimeInfo.getIdentifier(),
                runtimeInfo.getRuntimeStatus(),
                runtimeInfo.getLocation(),
                runtimeInfo.getCookie(),
                runtimeInfo.getAgentLauncherVersion(),
                elasticAgentId,
                elasticPluginId);
        this.setOperatingSystem(runtimeInfo.getOperatingSystem());
        this.setUsableSpace(runtimeInfo.getUsableSpace());
    }

    public ElasticAgentRuntimeInfo(AgentIdentifier identifier, AgentRuntimeStatus runtimeStatus, String location, String cookie, String agentLauncherVersion, String elasticAgentId, String elasticPluginId) {
        super(identifier, runtimeStatus, location, cookie, agentLauncherVersion, false);
        this.elasticAgentId = elasticAgentId;
        this.elasticPluginId = elasticPluginId;
    }

    public static ElasticAgentRuntimeInfo fromAgent(AgentIdentifier identifier, AgentRuntimeStatus runtimeStatus, String workingDir, String launcherVersion, String elasticAgentId, String pluginId) {
        return new ElasticAgentRuntimeInfo(identifier, runtimeStatus, workingDir, elasticAgentId, launcherVersion, elasticAgentId, pluginId);
    }

    @Override
    public void updateSelf(AgentRuntimeInfo newRuntimeInfo) {
        super.updateSelf(newRuntimeInfo);
        this.elasticAgentId = ((ElasticAgentRuntimeInfo) newRuntimeInfo).getElasticAgentId();
        this.elasticPluginId = ((ElasticAgentRuntimeInfo) newRuntimeInfo).getElasticPluginId();
    }

    @Override
    public boolean isElastic() {
        return true;
    }

    public String getElasticAgentId() {
        return elasticAgentId;
    }

    public String getElasticPluginId() {
        return elasticPluginId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ElasticAgentRuntimeInfo that = (ElasticAgentRuntimeInfo) o;

        if (elasticAgentId != null ? !elasticAgentId.equals(that.elasticAgentId) : that.elasticAgentId != null)
            return false;
        return elasticPluginId != null ? elasticPluginId.equals(that.elasticPluginId) : that.elasticPluginId == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (elasticAgentId != null ? elasticAgentId.hashCode() : 0);
        result = 31 * result + (elasticPluginId != null ? elasticPluginId.hashCode() : 0);
        return result;
    }

    public static AgentRuntimeInfo fromServer(AgentRuntimeInfo agentRuntimeInfo, String elasticAgentId, String elasticPluginId) {
        return new ElasticAgentRuntimeInfo(agentRuntimeInfo, elasticAgentId, elasticPluginId);
    }
}
