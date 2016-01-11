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

package com.thoughtworks.go.plugin.access.elastic;

public class AgentMetadata {
    private final String elasticAgentId;
    private final String agentState;
    private final String buildState;
    private final String configState;

    public AgentMetadata(String elasticAgentId, String agentState, String buildState, String configState) {
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
}
