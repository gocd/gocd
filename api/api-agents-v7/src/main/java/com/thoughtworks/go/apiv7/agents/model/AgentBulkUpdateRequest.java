/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv7.agents.model;

import com.thoughtworks.go.util.TriState;

import java.util.Collections;
import java.util.List;

public class AgentBulkUpdateRequest {
    private List<String> uuids;
    private TriState agentConfigState;
    private Operations operations;

    public AgentBulkUpdateRequest(List<String> uuids, Operations operations, TriState agentConfigState) {
        this.uuids = uuids;
        this.operations = operations;
        this.agentConfigState = agentConfigState;
    }

    public List<String> getUuids() {
        return uuids;
    }

    public Operations getOperations() {
        return operations;
    }

    public TriState getAgentConfigState() {
        return agentConfigState;
    }

    public static class Operations {
        private static final Operation EMPTY_OPERATION = new Operation(Collections.emptyList(), Collections.emptyList());
        private Operation environments;
        private Operation resources;

        public Operations() {
            this.environments = EMPTY_OPERATION;
            this.resources = EMPTY_OPERATION;
        }

        public Operations(Operation environments, Operation resources) {
            this.environments = environments;
            this.resources = resources;
        }

        public Operation getEnvironments() {
            return environments;
        }

        public Operation getResources() {
            return resources;
        }
    }

    public static class Operation {
        private List<String> add;
        private List<String> remove;

        public Operation(List<String> add, List<String> remove) {
            this.add = add;
            this.remove = remove;
        }

        public List<String> toAdd() {
            return add;
        }

        public List<String> toRemove() {
            return remove;
        }
    }
}


