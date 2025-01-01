/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.agent.testhelpers;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FakeBuildRepositoryRemote implements BuildRepositoryRemote {
    public final static List<AgentRuntimeStatus> AGENT_STATUS = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(FakeBuildRepositoryRemote.class);

    public static final String PIPELINE_NAME = "studios";
    public static final String STAGE_NAME = "pipeline";

    @Override
    public AgentInstruction ping(AgentRuntimeInfo info) {
        AGENT_STATUS.add(info.getRuntimeStatus());
        return AgentInstruction.NONE;
    }

    @Override
    public Work getWork(AgentRuntimeInfo runtimeInfo) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void reportCurrentStatus(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobState jobState) {
        LOGGER.info("Current status of build instance with id {} is {}", jobIdentifier, jobState);
    }

    @Override
    public void reportCompleting(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobResult result) {
        LOGGER.info("Build result of project {} is {}", jobIdentifier, result);
    }

    @Override
    public void reportCompleted(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobId, JobResult result) {
        LOGGER.info("Completed Build");
    }

    @Override
    public boolean isIgnored(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier) {
        return false;
    }

    @Override
    public String getCookie(AgentRuntimeInfo agentRuntimeInfo) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
