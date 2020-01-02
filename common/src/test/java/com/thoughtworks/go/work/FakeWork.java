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
package com.thoughtworks.go.work;

import com.thoughtworks.go.remote.work.AgentWorkContext;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

public class FakeWork implements Work {
    int callCount;

    @Override
    public void doWork(EnvironmentVariableContext environmentVariableContext, AgentWorkContext agentWorkContext) {
    }

    @Override
    public String description() {
        return null;
    }

    @Override
    public void cancel(EnvironmentVariableContext environmentVariableContext, AgentRuntimeInfo agentruntimeInfo) {
        callCount++;
    }

    public int getCallCount() {
        return callCount;
    }
}
