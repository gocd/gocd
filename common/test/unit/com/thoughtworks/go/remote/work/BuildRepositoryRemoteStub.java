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

package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;

import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;

public class BuildRepositoryRemoteStub implements BuildRepositoryRemote {
    public final List<JobState> states = new ArrayList<JobState>();
    public final List<JobResult> results = new ArrayList<JobResult>();
    private final boolean isIgnored;

    public BuildRepositoryRemoteStub() {
        this(false);
    }

    public BuildRepositoryRemoteStub(boolean isIgnored) {
        this.isIgnored = isIgnored;
    }

    public AgentInstruction ping(AgentRuntimeInfo info) {
        return new AgentInstruction(false);
    }

    public Work getWork(AgentRuntimeInfo runtimeInfo) {
        return getWork(new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, isIgnored));
    }

    public void reportCurrentStatus(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobState jobState) {
        states.add(jobState);
    }

    public void reportCompleting(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobResult result) {
        results.add(result);
    }

    public void reportCompleted(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobId, JobResult result) {
        results.add(result);
        states.add(JobState.Completed);
    }

    public boolean isIgnored(JobIdentifier jobIdentifier) {
        return isIgnored;
    }

    public String getCookie(AgentIdentifier identifier, String location) {
        throw new RuntimeException("implement me");
    }
}
