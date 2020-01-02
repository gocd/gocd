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
package com.thoughtworks.go.server.service.plugins.processor.elasticagent.v1;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

class AgentMetadataDTO implements Serializable {
    private static final Gson GSON = new GsonBuilder().
            excludeFieldsWithoutExposeAnnotation().
            serializeNulls().
            setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).
            create();

    @Expose
    @SerializedName("agent_id")
    private final String elasticAgentId;
    @Expose
    @SerializedName("agent_state")
    private final String agentState;
    @Expose
    @SerializedName("build_state")
    private final String buildState;
    @Expose
    @SerializedName("config_state")
    private final String configState;

    public AgentMetadataDTO(String elasticAgentId, String agentState, String buildState, String configState) {
        this.elasticAgentId = elasticAgentId;
        this.agentState = agentState;
        this.buildState = buildState;
        this.configState = configState;
    }

    public String elasticAgentId() {
        return elasticAgentId;
    }

    public String agentState() {
        return agentState;
    }

    public String buildState() {
        return buildState;
    }

    public String configState() {
        return configState;
    }

    public JsonElement toJSON() {
        return GSON.toJsonTree(this);
    }

    public static Collection<AgentMetadataDTO> fromJSONArray(String json) {
        Type AGENT_METADATA_LIST_TYPE = new TypeToken<List<AgentMetadataDTO>>() {
        }.getType();
        return GSON.fromJson(json, AGENT_METADATA_LIST_TYPE);
    }

    @Override
    public String toString() {
        return "AgentMetadata{" +
                "elasticAgentId='" + elasticAgentId + '\'' +
                ", agentState='" + agentState + '\'' +
                ", buildState='" + buildState + '\'' +
                ", configState='" + configState + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AgentMetadataDTO that = (AgentMetadataDTO) o;

        if (elasticAgentId != null ? !elasticAgentId.equals(that.elasticAgentId) : that.elasticAgentId != null)
            return false;
        if (agentState != null ? !agentState.equals(that.agentState) : that.agentState != null) return false;
        if (buildState != null ? !buildState.equals(that.buildState) : that.buildState != null) return false;
        return configState != null ? configState.equals(that.configState) : that.configState == null;

    }

    @Override
    public int hashCode() {
        int result = elasticAgentId != null ? elasticAgentId.hashCode() : 0;
        result = 31 * result + (agentState != null ? agentState.hashCode() : 0);
        result = 31 * result + (buildState != null ? buildState.hashCode() : 0);
        result = 31 * result + (configState != null ? configState.hashCode() : 0);
        return result;
    }
}
