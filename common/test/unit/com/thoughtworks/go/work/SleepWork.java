/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.work;

import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SleepWork implements Work {
    private String name;
    private int sleepInMilliSeconds;
    private transient CountDownLatch cancelLatch;

    public SleepWork(String name, int sleepInMilliSeconds) {
        this.name = name;
        this.sleepInMilliSeconds = sleepInMilliSeconds;
    }

    @Override
    public void doWork(AgentIdentifier agentIdentifier, BuildRepositoryRemote remoteBuildRepository, GoArtifactsManipulator manipulator, EnvironmentVariableContext environmentVariableContext, AgentRuntimeInfo agentRuntimeInfo, PackageAsRepositoryExtension packageAsRepositoryExtension, SCMExtension scmExtension, TaskExtension taskExtension) {
        cancelLatch = new CountDownLatch(1);
        agentRuntimeInfo.busy(new AgentBuildingInfo("sleepwork", "sleepwork1"));
        boolean canceled = false;

        try {
            if (this.sleepInMilliSeconds > 0) {
                canceled = cancelLatch.await(this.sleepInMilliSeconds, TimeUnit.MILLISECONDS);
            }

            String result = canceled ? "done_canceled" : "done";
            manipulator.setProperty(null, new Property(name + "_result", result));

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String description() {
        return "Sleep " + sleepInMilliSeconds + " milliseconds.";
    }

    @Override
    public void cancel(EnvironmentVariableContext environmentVariableContext, AgentRuntimeInfo agentruntimeInfo) {
        agentruntimeInfo.cancel();
        cancelLatch.countDown();
    }
}
