/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.domain.exception.UnregisteredAgentException;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

public class UnregisteredAgentWork implements Work {
    private String message;
    private final String uuid;

    public UnregisteredAgentWork(String invalidUuid) {
        this.uuid = invalidUuid;
        this.message = "Invalid agent: the UUID[" + invalidUuid + "] held by this agent is not registered";
    }

    public void doWork(AgentIdentifier agentIdentifier,
                       BuildRepositoryRemote remoteBuildRepository,
                       GoArtifactsManipulator manipulator, EnvironmentVariableContext environmentVariableContext, AgentRuntimeInfo agentRuntimeInfo, PackageAsRepositoryExtension packageAsRepositoryExtension, SCMExtension scmExtension, TaskExtension taskExtension) {
        throw new UnregisteredAgentException(message, uuid);
    }

    public String description() {
        throw new UnregisteredAgentException(message, uuid);
    }

    public void cancel(EnvironmentVariableContext environmentVariableContext, AgentRuntimeInfo agentruntimeInfo) {
        throw new UnregisteredAgentException(message, uuid);
    }
}
