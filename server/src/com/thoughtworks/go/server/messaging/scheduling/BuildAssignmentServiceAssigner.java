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

import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.BuildAssignmentService;
import com.thoughtworks.go.server.service.WorkAssigner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BuildAssignmentServiceAssigner implements WorkAssigner {
    private AgentService agentService;
    private BuildAssignmentService buildAssignmentService;

    @Autowired
    public BuildAssignmentServiceAssigner(
            AgentService agentService, BuildAssignmentService buildAssignmentService) {

        this.agentService = agentService;
        this.buildAssignmentService = buildAssignmentService;
    }


    public Work assignWorkToAgent(AgentIdentifier agent) {
        AgentInstance instance = agentService.findAgentAndRefreshStatus(agent.getUuid());
        return buildAssignmentService.assignWorkToAgent(instance);
    }
}
