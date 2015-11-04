/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import java.util.List;

import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.listener.AgentChangeListener;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.util.TriState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @understands how to convert persistant Agent configuration to useful objects and back
 */
@Service
public class AgentConfigService {
    private GoConfigService goConfigService;

    @Autowired
    public AgentConfigService(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public Agents agents() {
        return goConfigService.agents();
    }

    public void register(AgentChangeListener agentChangeListener) {
        goConfigService.register(agentChangeListener);
    }

    public void disableAgents(AgentInstance... agentInstance) {
        goConfigService.disableAgents(true, agentInstance);
    }

    public void enableAgents(AgentInstance... agentInstance) {
        goConfigService.disableAgents(false, agentInstance);
    }

    public void deleteAgents(AgentInstance... agentInstances) {
        goConfigService.deleteAgents(agentInstances);
    }

    public void updateAgentIpByUuid(String uuid, String ipAdress, String userName) {
        goConfigService.updateAgentIpByUuid(uuid, ipAdress, userName);
    }

    public void updateAgentAttributes(String uuid, String userName, String hostname, String resources, String environments, TriState enable, AgentInstances agentInstances) {
        goConfigService.updateAgentAttributes(uuid, userName, hostname, resources, environments, enable, agentInstances);
    }

    public void saveOrUpdateAgent(AgentInstance agentInstance) {
        goConfigService.saveOrUpdateAgent(agentInstance);
    }

    public void approvePendingAgent(AgentInstance agentInstance) {
        goConfigService.approvePendingAgent(agentInstance);
    }

    // For tests
    public void updateAgentApprovalStatus(String uuid, boolean isDenied) {
        goConfigService.updateAgentApprovalStatus(uuid, isDenied);
    }


    public void modifyResources(AgentInstance[] agentInstances, List<TriStateSelection> selections) {
        goConfigService.modifyResources(agentInstances, selections);
    }

    public Agents findAgents(List<String> uuids) {
        return agents().filter(uuids);
    }
}
