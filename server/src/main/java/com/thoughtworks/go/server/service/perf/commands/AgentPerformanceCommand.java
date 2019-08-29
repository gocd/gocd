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

package com.thoughtworks.go.server.service.perf.commands;

import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.service.AgentService;

import java.util.Optional;
import java.util.concurrent.Callable;

import static java.util.stream.StreamSupport.stream;

abstract class AgentPerformanceCommand implements Callable<Optional<String>> {
    AgentService agentService;

    String getName() {
        return getClass().getName();
    }

    Optional<AgentInstance> findAnyRegisteredAgentInstance() {
        AgentInstances registeredAgents = agentService.findRegisteredAgents();
        return stream(registeredAgents.spliterator(), false).findAny();
    }
}
