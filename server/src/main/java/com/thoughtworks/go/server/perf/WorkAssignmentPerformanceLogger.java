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
package com.thoughtworks.go.server.perf;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WorkAssignmentPerformanceLogger {
    private final Logger performanceLogger;

    @Autowired
    public WorkAssignmentPerformanceLogger(PerformanceLogger performanceLogger) {
        this.performanceLogger = performanceLogger;
    }

    public void retrievedWorkForAgent(AgentRuntimeInfo agentRuntimeInfo, Work work, long retrieveWorkStartTime, long retrieveWorkEndTime) {
        if (performanceLogger.isDebugEnabled()) {
            if (work instanceof BuildWork buildWork) {
                performanceLogger.debug("WORK-RETRIEVED {} {} {} {}", agentRuntimeInfo.getIdentifier().getUuid(), buildWork.identifierForLogging(), retrieveWorkStartTime, retrieveWorkEndTime);
            } else {
                performanceLogger.debug("WORK-NOWORK {} {} {}", agentRuntimeInfo.getIdentifier().getUuid(), retrieveWorkStartTime, retrieveWorkEndTime);
            }
        }
    }

    public void agentReportedCompletion(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, long reportCompletionStartTime, long reportCompletionEndTime) {
        if (performanceLogger.isDebugEnabled()) {
            performanceLogger.debug("WORK-COMPLETED {} {} {} {}", agentRuntimeInfo.getIdentifier().getUuid(), jobIdentifier, reportCompletionStartTime, reportCompletionEndTime);
        }
    }

    public void assignedWorkToAgent(Work work, AgentIdentifier agentIdentifier, long assignWorkStartTime, long assignWorkEndTime) {
        if (performanceLogger.isDebugEnabled() && work instanceof BuildWork buildWork) {
            performanceLogger.debug("WORK-ASSIGNED {} {} {} {}", agentIdentifier.getUuid(), buildWork.identifierForLogging(), assignWorkStartTime, assignWorkEndTime);
        }
    }
}
