/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.perf.commands;

import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.server.service.AgentService;

import java.util.Optional;

import static com.thoughtworks.go.util.TriState.FALSE;

public class DisableAgentCommand extends AgentPerformanceCommand {
    public DisableAgentCommand(AgentService agentService){
        this.agentService = agentService;
    }

    @Override
    Optional<String> execute() {
        Optional<AgentInstance> anyRegisteredAgentInstance = findAnyRegisteredAgentInstance();
        anyRegisteredAgentInstance.map(instance1 -> instance1.getAgent().getUuid()).ifPresent(this::disableAgent);
        return anyRegisteredAgentInstance.map(instance -> instance.getAgent().getUuid());
    }

    private void disableAgent(String id) {
        agentService.updateAgentAttributes(id, null, null, null, FALSE);
    }
}
