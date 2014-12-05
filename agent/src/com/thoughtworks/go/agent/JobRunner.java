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

import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;

public class JobRunner {
    private boolean handled = false;
    private boolean isJobCancelled = false;
    private Work work;
    private EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

    public void handleInstruction(AgentInstruction instruction, AgentRuntimeInfo agentStatus) {
        if (instruction.isShouldCancelJob() && !handled) {
            cancelJob(agentStatus);
        }
    }

    private void cancelJob(AgentRuntimeInfo agentRuntimeInfo) {
        isJobCancelled = true;
        if (work != null) {
            work.cancel(environmentVariableContext, agentRuntimeInfo);
        }
        handled = true;
    }

    public void run(Work work, AgentIdentifier agentIdentifier, BuildRepositoryRemote server,
                    GoArtifactsManipulator manipulator, AgentRuntimeInfo agentRuntimeInfo, TaskExtension taskExtension) {
        this.work = work;
        work.doWork(agentIdentifier, server, manipulator, environmentVariableContext, agentRuntimeInfo, taskExtension);
    }

    public boolean isJobCancelled() {
        return isJobCancelled;
    }

    //Used for tests only
    void setWork(Work work) {
        this.work = work;
    }
}
