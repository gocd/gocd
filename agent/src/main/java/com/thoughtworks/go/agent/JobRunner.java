/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.agent;

import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.work.AgentWorkContext;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobRunner {
    private static final Logger LOG = LoggerFactory.getLogger(AgentHTTPClientController.class);
    private final EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
    private volatile boolean cancelHandled = false;
    private volatile boolean killRunningTasksHandled = false;
    private volatile boolean isJobCancelled = false;
    private volatile boolean running = false;
    private volatile Work work;

    public void handleInstruction(AgentInstruction instruction, AgentRuntimeInfo agentStatus) {
        if (shouldCancelJob(instruction)) {
            cancelJob(agentStatus);
        }

        if (shouldKillRunningTasks(instruction)) {
            killRunningTasks(agentStatus);
        }
    }

    public void run(Work work, AgentWorkContext agentWorkContext) {
        running = true;
        this.work = work;
        try {
            work.doWork(environmentVariableContext, agentWorkContext);
        } finally {
            running = false;
        }
    }

    public boolean isJobCancelled() {
        return isJobCancelled;
    }

    @TestOnly
    void setWork(Work work) {
        this.work = work;
    }

    @Override
    public String toString() {
        return "JobRunner{" +
                "cancelHandled=" + cancelHandled +
                ", killRunningTasksHandled=" + killRunningTasksHandled +
                ", isJobCancelled=" + isJobCancelled +
                ", running=" + running +
                ", work=" + work +
                ", environmentVariableContext=" + environmentVariableContext +
                '}';
    }

    private boolean shouldKillRunningTasks(AgentInstruction instruction) {
        return instruction.shouldKillRunningTasks() && !killRunningTasksHandled;
    }

    private boolean shouldCancelJob(AgentInstruction instruction) {
        return instruction.shouldCancel() && !cancelHandled;
    }

    private void cancelJob(AgentRuntimeInfo agentRuntimeInfo) {
        isJobCancelled = true;
        if (work != null) {
            LOG.info("");
            work.cancel(environmentVariableContext, agentRuntimeInfo);
        }

        cancelHandled = true;
    }

    private void killRunningTasks(AgentRuntimeInfo agentRuntimeInfo) {
        isJobCancelled = true;
        if (work != null) {
            work.cancel(environmentVariableContext, agentRuntimeInfo);
        }

        killRunningTasksHandled = true;
    }
}
