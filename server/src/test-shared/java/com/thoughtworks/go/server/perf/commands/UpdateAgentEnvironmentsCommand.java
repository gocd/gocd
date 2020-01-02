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
package com.thoughtworks.go.server.perf.commands;

import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.server.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.thoughtworks.go.util.TriState.UNSET;

public class UpdateAgentEnvironmentsCommand extends AgentPerformanceCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAgentEnvironmentsCommand.class);

    public UpdateAgentEnvironmentsCommand(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    Optional<String> execute() {
        Optional<AgentInstance> anyRegisteredAgentInstance = findAnyRegisteredAgentInstance();
        anyRegisteredAgentInstance.map(instance -> instance.getAgent().getUuid()).ifPresent(this::updateAgentEnvironment);
        return anyRegisteredAgentInstance.map(instance -> instance.getAgent().getUuid());
    }

    private void updateAgentEnvironment(String id) {
        try {
            agentService.updateAgentAttributes(id, null, null, "e1,e2", UNSET);
        } catch (Exception e) {
            LOGGER.error("Error while updating agent environment for agent {}, error is {}", id, e.getMessage(), e);
        }
    }
}
