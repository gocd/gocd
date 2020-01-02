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
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.EnvironmentConfigService;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

import static com.thoughtworks.go.util.TriState.UNSET;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

public class BulkUpdateAgentCommand extends AgentPerformanceCommand {
    private EnvironmentConfigService envConfigService;

    public BulkUpdateAgentCommand(AgentService agentService, EnvironmentConfigService envConfigService) {
        this.envConfigService = envConfigService;
        this.agentService = agentService;
    }

    @Override
    Optional<String> execute() {
        Optional<List<String>> anyRegisteredUUIDs = findAnyRegisteredUUIDs();
        anyRegisteredUUIDs.ifPresent(this::bulkUpdateAgent);
        return Optional.ofNullable(StringUtils.join(anyRegisteredUUIDs.get(), " | "));
    }

    private void bulkUpdateAgent(List<String> uuids) {
        agentService.bulkUpdateAgentAttributes(uuids, asList("r3", "r4"), null, asList("e3", "e4"), asList("e1", "e2"), UNSET, envConfigService);
    }

    private Optional<List<String>> findAnyRegisteredUUIDs() {
        AgentInstances registeredAgents = agentService.findRegisteredAgents();
        return Optional.of(stream(registeredAgents.spliterator(), false)
                .map(AgentInstance::getUuid)
                .limit(3)
                .collect(toList()));
    }
}
