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
package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.server.service.AgentService;

public class EnvironmentViewModel {

    private EnvironmentConfig environmentConfig;
    private AgentsViewModel agentViewModels;

    public EnvironmentViewModel() {
    }

    public EnvironmentViewModel(EnvironmentConfig environmentConfig) {
        this.environmentConfig = environmentConfig;
    }

    public EnvironmentViewModel(EnvironmentConfig environmentConfig, AgentsViewModel agentViewModels) {
        this.environmentConfig = environmentConfig;
        this.agentViewModels = agentViewModels;
    }

    public EnvironmentConfig getEnvironmentConfig() {
        return environmentConfig;
    }

    public void setAgentViewModels(AgentService agentService) {
        this.agentViewModels = agentService.filterAgentsViewModel(environmentConfig.getAgents().getUuids());
    }

    public AgentsViewModel getAgentViewModels() {
        return agentViewModels;
    }

    public void setAgentViewModels(AgentsViewModel agentViewModels) {
        this.agentViewModels = agentViewModels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EnvironmentViewModel that = (EnvironmentViewModel) o;

        if (environmentConfig != null ? !environmentConfig.equals(that.environmentConfig) : that.environmentConfig != null)
            return false;
        return agentViewModels != null ? agentViewModels.equals(that.agentViewModels) : that.agentViewModels == null;
    }

    @Override
    public int hashCode() {
        int result = environmentConfig != null ? environmentConfig.hashCode() : 0;
        result = 31 * result + (agentViewModels != null ? agentViewModels.hashCode() : 0);
        return result;
    }
}
