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
package com.thoughtworks.go.server.perf;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WorkAssignmentPerformanceLogger {
    private PerformanceLogger performanceLogger;

    @Autowired
    public WorkAssignmentPerformanceLogger(PerformanceLogger performanceLogger) {
        this.performanceLogger = performanceLogger;
    }

    public void retrievedWorkForAgent(AgentRuntimeInfo agentRuntimeInfo, Work work, long retrieveWorkStartTime, long retrieveWorkEndTime) {
        if (work == null || !(work instanceof BuildWork)) {
            performanceLogger.log("WORK-NOWORK {} {} {}", agentRuntimeInfo.getIdentifier().getUuid(), retrieveWorkStartTime, retrieveWorkEndTime);
            return;
        }
        BuildWork buildWork = (BuildWork) work;

        performanceLogger.log("WORK-RETRIEVED {} {} {} {}", agentRuntimeInfo.getIdentifier().getUuid(), buildWork.identifierForLogging(), retrieveWorkStartTime, retrieveWorkEndTime);
    }

    public void agentReportedCompletion(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, long reportCompletionStartTime, long reportCompletionEndTime) {
        performanceLogger.log("WORK-COMPLETED {} {} {} {}", agentRuntimeInfo.getIdentifier().getUuid(), jobIdentifier, reportCompletionStartTime, reportCompletionEndTime);
    }

    public void assignedWorkToAgent(Work work, AgentIdentifier agentIdentifier, long assignWorkStartTime, long assignWorkEndTime) {
        if (work == null || !(work instanceof BuildWork)) {
            return;
        }
        BuildWork buildWork = (BuildWork) work;

        performanceLogger.log("WORK-ASSIGNED {} {} {} {}", agentIdentifier.getUuid(), buildWork.identifierForLogging(), assignWorkStartTime, assignWorkEndTime);
    }
}
