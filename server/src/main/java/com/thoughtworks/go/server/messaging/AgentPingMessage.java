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

package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.remote.AgentIdentifier;

public class AgentPingMessage implements GoMessage {
    private AgentIdentifier agentIdentifier;

    public AgentPingMessage(AgentIdentifier agentIdentifier) {
        this.agentIdentifier = agentIdentifier;
    }

    public String toString() {
        return String.format("[AgentPing: %s]", agentIdentifier);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AgentPingMessage that = (AgentPingMessage) o;

        if (agentIdentifier != null ? !agentIdentifier.equals(that.agentIdentifier) : that.agentIdentifier != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return (agentIdentifier != null ? agentIdentifier.hashCode() : 0);
    }

}
