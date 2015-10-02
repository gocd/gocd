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

package com.thoughtworks.go.server.messaging.scheduling;

import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.NoWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.messaging.GoMessageChannel;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.perf.WorkAssignmentPerformanceLogger;
import com.thoughtworks.go.server.service.BuildAssignmentService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkFinder implements GoMessageListener<IdleAgentMessage> {
    private static final Logger LOGGER = Logger.getLogger(WorkFinder.class);

    private static final NoWork NO_WORK = new NoWork();
    private BuildAssignmentService buildAssignmentService;
    private GoMessageChannel<WorkAssignedMessage> assignedWorkTopic;
    private WorkAssignmentPerformanceLogger workAssignmentPerformanceLogger;

    @Autowired
    public WorkFinder(BuildAssignmentService buildAssignmentService,
                      IdleAgentTopic idleAgentTopic,
                      WorkAssignedTopic assignedWorkTopic,
                      WorkAssignmentPerformanceLogger workAssignmentPerformanceLogger) {
        this.buildAssignmentService = buildAssignmentService;
        this.assignedWorkTopic = assignedWorkTopic;
        this.workAssignmentPerformanceLogger = workAssignmentPerformanceLogger;
        idleAgentTopic.addListener(this);
    }

    public void onMessage(IdleAgentMessage idleAgentMessage) {
        AgentIdentifier agent = idleAgentMessage.getAgentIdentifier();
        Work work = null;
        long startTime = System.currentTimeMillis();

        try {
            work = buildAssignmentService.assignWorkToAgent(agent);
        } finally {
            if (work == null) {
                work = NO_WORK;
            }

            workAssignmentPerformanceLogger.assignedWorkToAgent(work, agent, startTime, System.currentTimeMillis());
            try {
                assignedWorkTopic.post(new WorkAssignedMessage(agent, work));
            } catch (Throwable e) {
                LOGGER.fatal(null, e);
            }
        }
    }

}
