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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.NoWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.messaging.GoMessageChannel;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkAssignments implements GoMessageListener<WorkAssignedMessage> {
    private GoMessageChannel<IdleAgentMessage> idleAgentsTopic;
    private ConcurrentMap<AgentIdentifier, Work> assignments;
    private static final NoWork NO_WORK = new NoWork();

    @Autowired
    public WorkAssignments(IdleAgentTopic idleAgentsTopic,
                           WorkAssignedTopic assignedWorkTopic) {
        this.idleAgentsTopic = idleAgentsTopic;
        assignedWorkTopic.addListener(this);
        this.assignments = new ConcurrentHashMap<>();
    }

    public Work getWork(AgentRuntimeInfo runtimeInfo) {
        AgentIdentifier agent = runtimeInfo.getIdentifier();
        synchronized (agentMutex(agent)) {
            Work work = assignments.get(agent);
            if (work == null) {
                assignments.put(agent, NO_WORK);
                idleAgentsTopic.post(new IdleAgentMessage(runtimeInfo));
                return NO_WORK;
            }

            if (work instanceof NoWork) {
                return work;
            }

            return assignments.remove(agent);
        }
    }

    private String agentMutex(AgentIdentifier agent) {
        return agent.getUuid().intern();
    }

    public void onMessage(WorkAssignedMessage message) {
        AgentIdentifier agentIdentifier = message.getAgent();
        Work work = message.getWork();
        if (work instanceof NoWork) {
            synchronized (agentMutex(agentIdentifier)) {
                assignments.remove(agentIdentifier);
            }
        } else {
            synchronized (agentMutex(agentIdentifier)) {
                assignments.replace(agentIdentifier, NO_WORK, work);
            }
        }
    }

}
