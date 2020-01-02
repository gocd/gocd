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
import com.thoughtworks.go.server.perf.Heartbeat;
import com.thoughtworks.go.server.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.stream.StreamSupport.stream;

public abstract class AgentPerformanceCommand implements Callable<Optional<String>> {
    public static final LinkedBlockingQueue<AgentPerformanceCommand> queue = new LinkedBlockingQueue<>();
    private Logger LOG = LoggerFactory.getLogger(AgentPerformanceCommand.class);
    private AgentPerformanceCommandResult result = new AgentPerformanceCommandResult().setName(getName());
    private Heartbeat heartbeat = new Heartbeat();
    AgentService agentService;

    String getName() {
        return getClass().getSimpleName();
    }

    public Optional<String> call() {
        boolean completed = true;
        Optional<String> value = Optional.empty();
        try {
            heartbeat.start();
            queue.put(this);
            value = this.execute();
        } catch (Exception e) {
            logCommandExecutionError(e);
            completed = false;
        } finally {
            endHeartBeatAndLogCommandCompletion(completed, heartbeat, value);
        }
        return value;
    }

    abstract Optional<String> execute();

    Optional<AgentInstance> findAnyRegisteredAgentInstance() {
        AgentInstances registeredAgents = agentService.findRegisteredAgents();
        LOG.debug("Registered Agent Instances size: {}",registeredAgents.size());
        Optional<AgentInstance> anyAgentInstance = stream(registeredAgents.spliterator(), false).findAny();
        if (!anyAgentInstance.isPresent()) LOG.debug("Returning empty agent instance: {} by {}", anyAgentInstance, this.getName());
        return anyAgentInstance;
    }

    public AgentPerformanceCommandResult getResult() {
        return result;
    }

    private void logCommandExecutionError(Exception e) {
        result.setFailureMessage(e.getMessage());
        LOG.error("Error while executing command [" + this.getName() + "]. More details : ", e);
    }

    private void endHeartBeatAndLogCommandCompletion(boolean completed, Heartbeat heartbeat, Optional<String> value) {
        String format = "Completed {} command for agent {} with status {} and time taken {} msec.";
        heartbeat.end();
        String status = completed ? "completed" : "failed";
        long ageInMillis = heartbeat.getAgeInMillis();
        result.setStatus(status)
                .setAgentUuids("[" + value.orElse("") + "]")
                .setTimeTakenInMillis(ageInMillis);
        LOG.info(format, this.getName(), value.orElse(""), status, ageInMillis);
    }
}
