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
package com.thoughtworks.go.work;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.remote.work.AgentWorkContext;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class SleepWork implements Work {
    private String name;
    private int sleepInMilliSeconds;
    private transient CountDownLatch cancelLatch;

    public SleepWork(String name, int sleepInMilliSeconds) {
        this.name = name;
        this.sleepInMilliSeconds = sleepInMilliSeconds;
    }

    @Override
    public void doWork(EnvironmentVariableContext environmentVariableContext, AgentWorkContext agentWorkContext) {
        cancelLatch = new CountDownLatch(1);
        agentWorkContext.getAgentRuntimeInfo().busy(new AgentBuildingInfo("sleepwork", "sleepwork1"));
        boolean canceled = false;
        DefaultGoPublisher goPublisher = new DefaultGoPublisher(agentWorkContext.getArtifactsManipulator(), new JobIdentifier(),
                agentWorkContext.getRepositoryRemote(), agentWorkContext.getAgentRuntimeInfo(), "utf-8");

        try {
            if (this.sleepInMilliSeconds > 0) {
                canceled = cancelLatch.await(this.sleepInMilliSeconds, TimeUnit.MILLISECONDS);
            }

            String result = canceled ? "done_canceled" : "done";
            agentWorkContext.getArtifactsManipulator().setProperty(null, new Property(name + "_result", result));
            SystemEnvironment systemEnvironment = new SystemEnvironment();
            if (systemEnvironment.isConsoleLogsThroughWebsocketEnabled() && systemEnvironment.isWebsocketsForAgentsEnabled()) {
                goPublisher.consumeLine(format("Sleeping for %s milliseconds", this.sleepInMilliSeconds));
            }
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
