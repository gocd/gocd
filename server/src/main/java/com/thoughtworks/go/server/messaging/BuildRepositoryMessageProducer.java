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
package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.remote.*;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.messaging.scheduling.WorkAssignments;
import com.thoughtworks.go.server.perf.WorkAssignmentPerformanceLogger;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BuildRepositoryMessageProducer implements BuildRepositoryRemote {
    private BuildRepositoryRemoteImpl buildRepository;
    private WorkAssignments workAssignments;
    private WorkAssignmentPerformanceLogger workAssignmentPerformanceLogger;

    @Autowired
    public BuildRepositoryMessageProducer(BuildRepositoryRemoteImpl buildRepository, WorkAssignments workAssignments, WorkAssignmentPerformanceLogger workAssignmentPerformanceLogger) {
        this.buildRepository = buildRepository;
        this.workAssignments = workAssignments;
        this.workAssignmentPerformanceLogger = workAssignmentPerformanceLogger;
    }

    @Override
    public AgentInstruction ping(AgentRuntimeInfo info) {
        return buildRepository.ping(info);
    }

    @Override
    public Work getWork(AgentRuntimeInfo runtimeInfo) {
        long startTime = System.currentTimeMillis();

        Work work = workAssignments.getWork(runtimeInfo);

        workAssignmentPerformanceLogger.retrievedWorkForAgent(runtimeInfo, work, startTime, System.currentTimeMillis());
        return work;
    }

    @Override
    public void reportCurrentStatus(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobState jobState) {
        buildRepository.reportCurrentStatus(agentRuntimeInfo, jobIdentifier, jobState);
    }

    @Override
    public void reportCompleting(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobResult result) {
        buildRepository.reportCompleting(agentRuntimeInfo, jobIdentifier, result);
    }

    @Override
    public boolean isIgnored(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier) {
        return buildRepository.isIgnored(agentRuntimeInfo, jobIdentifier);
    }

    @Override
    public String getCookie(AgentRuntimeInfo agentRuntimeInfo) {
        return buildRepository.getCookie(agentRuntimeInfo);
    }

    @Override
    public void reportCompleted(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobId, JobResult result) {
        long startTime = System.currentTimeMillis();

        buildRepository.reportCompleted(agentRuntimeInfo, jobId, result);

        workAssignmentPerformanceLogger.agentReportedCompletion(agentRuntimeInfo, jobId, startTime, System.currentTimeMillis());
    }
}
