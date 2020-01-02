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
package com.thoughtworks.go.server.messaging.scheduling;

import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.messaging.GoMessage;

public class WorkAssignedMessage implements GoMessage {
    private AgentIdentifier agent;
    private Work work;

    public WorkAssignedMessage(AgentIdentifier agentIdentifier, Work work) {
        this.agent = agentIdentifier;
        this.work = work;
    }

    public AgentIdentifier getAgent() {
        return agent;
    }

    public Work getWork() {
        return work;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WorkAssignedMessage that = (WorkAssignedMessage) o;

        if (agent != null ? !agent.equals(that.agent) : that.agent != null) {
            return false;
        }
        if (work != null ? !work.equals(that.work) : that.work != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = agent != null ? agent.hashCode() : 0;
        result = 31 * result + (work != null ? work.hashCode() : 0);
        return result;
    }
}
