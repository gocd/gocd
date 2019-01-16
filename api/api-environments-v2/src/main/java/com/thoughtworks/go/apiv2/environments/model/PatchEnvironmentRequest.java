/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv2.environments.model;

import com.thoughtworks.go.config.EnvironmentVariableConfig;

import java.util.List;

public class PatchEnvironmentRequest {
    private List<String> pipelineToAdd;
    private List<String> pipelineToRemove;
    private List<String> agentsToAdd;
    private List<String> agentsToRemove;
    private List<String> envVariablesToRemove;
    private List<EnvironmentVariableConfig> environmentVariablesToAdd;


    public PatchEnvironmentRequest(List<String> pipelineToAdd, List<String> pipelineToRemove, List<String> agentsToAdd, List<String> agentsToRemove, List<EnvironmentVariableConfig> environmentVariablesToAdd, List<String> envVariablesToRemove) {
        this.pipelineToAdd = pipelineToAdd;
        this.pipelineToRemove = pipelineToRemove;
        this.agentsToAdd = agentsToAdd;
        this.agentsToRemove = agentsToRemove;
        this.envVariablesToRemove = envVariablesToRemove;
        this.environmentVariablesToAdd = environmentVariablesToAdd;
    }

    public List<String> getPipelineToAdd() {
        return pipelineToAdd;
    }

    public List<String> getPipelineToRemove() {
        return pipelineToRemove;
    }

    public List<String> getAgentsToAdd() {
        return agentsToAdd;
    }

    public List<String> getAgentsToRemove() {
        return agentsToRemove;
    }

    public List<String> getEnvironmentVariablesToRemove() {
        return envVariablesToRemove;
    }

    public List<EnvironmentVariableConfig> getEnvironmentVariablesToAdd() {
        return environmentVariablesToAdd;
    }
}
