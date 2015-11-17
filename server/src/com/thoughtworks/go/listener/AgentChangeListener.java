/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.listener;

import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.server.service.AgentService;

public class AgentChangeListener extends EntityConfigChangedListener<Agents> {
    private final AgentService agentService;

    public AgentChangeListener(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    public void onEntityConfigChange(Agents agents) {
        agentService.sync(agents);
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        onEntityConfigChange(newCruiseConfig.agents());
    }
}
