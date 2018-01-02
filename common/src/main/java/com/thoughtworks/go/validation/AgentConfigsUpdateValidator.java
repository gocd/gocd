/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.validation;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.CruiseConfig;

import java.util.ArrayList;
import java.util.List;

public class AgentConfigsUpdateValidator implements ConfigUpdateValidator {
    private final List<String> agentsUuids;
    private final List<AgentConfig> validatedAgents = new ArrayList<>();

    public AgentConfigsUpdateValidator(List<String> agentsUuids) {
        this.agentsUuids = agentsUuids;
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        boolean isValid = true;
        for (String uuid : agentsUuids) {
            AgentConfig agentConfig = preprocessedConfig.agents().getAgentByUuid(uuid);
            isValid = agentConfig.validateTree(ConfigSaveValidationContext.forChain(preprocessedConfig)) && isValid;
            validatedAgents.add(agentConfig);
        }
        return isValid;
    }

    public List<AgentConfig> getUpdatedAgents() {
        return validatedAgents;
    }
}

