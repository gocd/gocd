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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.util.SystemEnvironment;

import static com.thoughtworks.go.domain.AgentInstance.AgentType.REMOTE;
import static com.thoughtworks.go.domain.NullAgent.createNullAgent;

public class NullAgentInstance extends AgentInstance {
    public NullAgentInstance(String uuid) {
        super(createNullAgent(uuid), REMOTE, new SystemEnvironment(), null);
    }

    @Override
    public void deny() {
    }

    @Override
    public void cancel() {
    }

    @Override
    public void refresh() {
    }

    @Override
    public boolean isIpChangeRequired(String ipAdress) {
        return false;
    }

    @Override
    public boolean isRegistered() {
        return false;
    }

    @Override
    public void building(AgentBuildingInfo agentBuildingInfo) {
    }

    @Override
    public boolean isNullAgent() {
        return true;
    }
}
