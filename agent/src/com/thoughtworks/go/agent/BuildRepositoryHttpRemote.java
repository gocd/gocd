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

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.springframework.stereotype.Component;

@Component
public class BuildRepositoryHttpRemote implements BuildRepositoryRemote {
    @Override
    public AgentInstruction ping(AgentRuntimeInfo info) {
        return null;
    }

    @Override
    public Work getWork(AgentRuntimeInfo runtimeInfo) {
        return null;
    }

    @Override
    public void reportCurrentStatus(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobState jobState) {

    }

    @Override
    public void reportCompleting(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobResult result) {

    }

    @Override
    public void reportCompleted(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobResult result) {

    }

    @Override
    public boolean isIgnored(JobIdentifier jobIdentifier) {
        return false;
    }

    @Override
    public String getCookie(AgentIdentifier identifier, String location) {
        return null;
    }
}
