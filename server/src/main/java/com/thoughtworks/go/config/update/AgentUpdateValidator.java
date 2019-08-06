/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.exceptions.InvalidPendingAgentOperationException;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.util.TriState;

import static com.thoughtworks.go.serverhealth.HealthStateScope.GLOBAL;
import static com.thoughtworks.go.serverhealth.HealthStateType.general;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

public class AgentUpdateValidator {
    private final AgentInstance agentInstance;
    private final HttpOperationResult result;
    private final TriState state;

    public AgentUpdateValidator(AgentInstance agentInstance, TriState state, HttpOperationResult result) {
        this.agentInstance = agentInstance;
        this.state = state;
        this.result = result;
    }

    public void validate() throws Exception {
        bombIfAnyOperationOnPendingAgent();
    }

    private void bombIfAnyOperationOnPendingAgent() throws InvalidPendingAgentOperationException {
        if (!agentInstance.isPending()) {
            return;
        }

        if (state.isTrue() || state.isFalse()) {
            return;
        }

        String msg = format("Pending agent [%s] must be explicitly enabled or disabled when performing any operation on it.", agentInstance.getUuid());
        result.badRequest(msg, msg, general(GLOBAL));
        throw new InvalidPendingAgentOperationException(singletonList(agentInstance.getUuid()));
    }
}
