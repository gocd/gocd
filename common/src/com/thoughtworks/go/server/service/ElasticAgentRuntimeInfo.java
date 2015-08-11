/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.AgentAutoRegistrationProperties;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;

import static org.apache.commons.lang.StringUtils.isBlank;

public class ElasticAgentRuntimeInfo implements Serializable {

    private volatile String agentId;
    private volatile String pluginId;

    public ElasticAgentRuntimeInfo(String elasticAgentId, String pluginId) {
        this.agentId = elasticAgentId;
        this.pluginId = pluginId;
    }

    public static ElasticAgentRuntimeInfo createFrom(AgentAutoRegistrationProperties properties) {
        if (properties != null && properties.exist() && !isBlank(properties.getAgentAutoRegisterElasticPluginId())) {
            return new ElasticAgentRuntimeInfo(properties.getAgentAutoRegisterElasticAgentId(), properties.getAgentAutoRegisterElasticPluginId());
        }
        return null;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getPluginId() {
        return pluginId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ElasticAgentRuntimeInfo that = (ElasticAgentRuntimeInfo) o;

        if (agentId != null ? !agentId.equals(that.agentId) : that.agentId != null) return false;
        return !(pluginId != null ? !pluginId.equals(that.pluginId) : that.pluginId != null);

    }

    @Override
    public int hashCode() {
        int result = agentId != null ? agentId.hashCode() : 0;
        result = 31 * result + (pluginId != null ? pluginId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
