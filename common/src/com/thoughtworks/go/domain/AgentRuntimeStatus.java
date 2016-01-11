/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.server.service.AgentRuntimeInfo;

/**
 * @understands live information about an agent
 */
public enum AgentRuntimeStatus {
    Idle, Building, LostContact, Missing, Cancelled, Unknown;

    public AgentRuntimeStatus buildState() {
        switch (this) {
            case Idle:
                return this;
            case Building:
                return this;
            case Cancelled:
                return this;
            default:
                return Unknown;
        }
    }

    public AgentRuntimeStatus agentState() {
        switch (this) {
            case Idle:
                return this;
            case Building:
                return this;
            case LostContact:
                return this;
            case Missing:
                return this;
            case Cancelled:
                return Building;
            default:
                return Unknown;
        }
    }

    public interface ChangeListener {
        void statusUpdateRequested(AgentRuntimeInfo runtimeInfo, AgentRuntimeStatus newStatus);
    }
}
