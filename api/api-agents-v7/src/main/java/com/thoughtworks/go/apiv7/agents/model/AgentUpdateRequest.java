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
package com.thoughtworks.go.apiv7.agents.model;

import com.thoughtworks.go.util.TriState;

public class AgentUpdateRequest {
    private final String hostname;
    private final String resources;
    private final String environments;
    private final TriState agentConfigState;

    public AgentUpdateRequest(String hostname, TriState agentConfigState, String environments, String resources) {
        this.hostname = hostname;
        this.agentConfigState = agentConfigState;
        this.environments = environments;
        this.resources = resources;
    }

    public String getHostname() {
        return hostname;
    }

    public TriState getAgentConfigState() {
        return agentConfigState;
    }

    public String getEnvironments() {
        return environments;
    }

    public String getResources() {
        return resources;
    }
}
