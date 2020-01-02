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

import com.thoughtworks.go.server.messaging.GoMessage;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.remote.AgentIdentifier;

public class IdleAgentMessage implements GoMessage {
    private AgentRuntimeInfo agent;

    public IdleAgentMessage(AgentRuntimeInfo agent) {
        this.agent = agent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IdleAgentMessage that = (IdleAgentMessage) o;

        if (!agent.equals(that.agent)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return agent.hashCode();
    }

    public AgentIdentifier getAgentIdentifier() {
        return agent.getIdentifier();
    }
}
