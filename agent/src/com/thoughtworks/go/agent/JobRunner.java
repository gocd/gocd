/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.agent;

import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class JobRunner {
    private AtomicBoolean handled = new AtomicBoolean(false);
    private AtomicBoolean isJobCancelled = new AtomicBoolean(false);
    private AtomicBoolean running = new AtomicBoolean(false);
    private CountDownLatch doneSignal = new CountDownLatch(1);
    private Work work;
    private EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

    public void handleInstruction(AgentInstruction instruction, AgentRuntimeInfo agentStatus) {
        if (instruction.isShouldCancelJob() && !handled.get()) {
            cancelJob(agentStatus);
        }
    }

    private void cancelJob(AgentRuntimeInfo agentRuntimeInfo) {
        isJobCancelled.set(true);
        if (work != null) {
            work.cancel(environmentVariableContext, agentRuntimeInfo);
        }
        handled.set(true);
    }

    public void run(Work work, AgentIdentifier agentIdentifier, BuildRepositoryRemote server, GoArtifactsManipulator manipulator, AgentRuntimeInfo agentRuntimeInfo,
                    PackageAsRepositoryExtension packageAsRepositoryExtension, SCMExtension scmExtension, TaskExtension taskExtension) {
        running.set(true);
        this.work = work;
        try {
            work.doWork(agentIdentifier, server, manipulator, environmentVariableContext, agentRuntimeInfo, packageAsRepositoryExtension, scmExtension, taskExtension);
        } finally {
            running.set(false);
            doneSignal.countDown();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public void waitUntilDone(long seconds) {
        try {
            doneSignal.await(seconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isJobCancelled() {
        return isJobCancelled.get();
    }

    //Used for tests only
    void setWork(Work work) {
        this.work = work;
    }

    @Override
    public String toString() {
        return "JobRunner{" +
                "handled=" + handled +
                ", isJobCancelled=" + isJobCancelled +
                ", running=" + running +
                ", doneSignal=" + doneSignal +
                ", work=" + work +
                ", environmentVariableContext=" + environmentVariableContext +
                '}';
    }
}
