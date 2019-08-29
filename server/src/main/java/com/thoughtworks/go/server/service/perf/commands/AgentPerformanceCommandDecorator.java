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

import com.thoughtworks.go.server.service.perf.Heartbeat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.String.format;

public class AgentPerformanceCommandDecorator extends AgentPerformanceCommand {
    private static final LinkedBlockingQueue<AgentPerformanceCommand> queue = new LinkedBlockingQueue<>();

    private Logger LOG = LoggerFactory.getLogger(AgentPerformanceCommandDecorator.class);
    private AgentPerformanceCommand command;

    public AgentPerformanceCommandDecorator(AgentPerformanceCommand command) {
        this.command = command;
    }

    public Optional<String> call() {
        Heartbeat heartbeat = null;
        Optional<String> value = null;
        boolean completed = true;

        try {
            heartbeat = addCommandToQueueAndStartHeartBeat();
            value = command.call();
        } catch (Exception e) {
            logCommandExecutionError(e);
            completed = false;
        } finally {
            endHeartBeatAndLogCommandCompletion(completed, heartbeat, value);
        }

        return value;
    }

    private void logCommandExecutionError(Exception e) {
        LOG.error("Error while executing command [" + command.getName() + "]. More details : ", e);
    }

    private Heartbeat addCommandToQueueAndStartHeartBeat() throws InterruptedException {
        LOG.debug("Started [" + command.getName() + "]");
        queue.put(command);
        return startHeartBeat();
    }

    private Heartbeat startHeartBeat() {
        Heartbeat heartbeat = new Heartbeat();
        heartbeat.start();
        return heartbeat;
    }

    private void endHeartBeatAndLogCommandCompletion(boolean completed, Heartbeat heartbeat, Optional<String> value) {
        LOG.debug("Completed [" + command.getName() + "] for agent [" + value.orElse(null) + "]");
        heartbeat.end();
        String status = completed ? "completed" : "failed";
        String msg = format("[%s] %s and took [%s] msecs to execute, for agent [%s]",
                command.getName(),
                status,
                heartbeat.getAgeInMillis(),
                value.orElse(null));
        LOG.debug(msg);
    }
}
