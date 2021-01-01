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

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.UUID;

public class RegisterAgentCommand extends AgentPerformanceCommand {
    public RegisterAgentCommand(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    Optional<String> execute() {
        return registerAgent();
    }

    private Optional<String> registerAgent() {
        InetAddress localHost = getInetAddress();
        Agent agent = new Agent("Perf-Test-Agent-" + UUID.randomUUID(), localHost.getHostName(), localHost.getHostAddress(), UUID.randomUUID().toString());
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(agent, false, "location", 233232L, "osx");
        agentService.requestRegistration(agentRuntimeInfo);
        return Optional.ofNullable(agent.getUuid());
    }

    private InetAddress getInetAddress() {
        InetAddress localHost;
        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return localHost;
    }
}
