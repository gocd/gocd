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
package com.thoughtworks.go.agent;

import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.work.AgentWorkContext;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class JobRunner {
    private volatile boolean cancelHandled = false;
    private volatile boolean killRunningTasksHandled = false;
    private volatile boolean isJobCancelled = false;
    private volatile boolean running = false;
    private final CountDownLatch doneSignal = new CountDownLatch(1);
    private Work work;
    private final EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
    private static final Logger LOG = LoggerFactory.getLogger(AgentHTTPClientController.class);

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
            doneSignal.countDown();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void waitUntilDone(long seconds) throws InterruptedException {
        doneSignal.await(seconds, TimeUnit.SECONDS);
    }

    public boolean isJobCancelled() {
        return isJobCancelled;
    }

    //Used for tests only

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
                ", doneSignal=" + doneSignal +
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
