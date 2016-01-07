/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.websocket;

import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentRemoteHandler {
    private Map<String, Agent> agentSessions = new ConcurrentHashMap();

    @Qualifier("buildRepositoryMessageProducer")
    @Autowired
    private BuildRepositoryRemote buildRepositoryRemote;
    @Autowired
    private AgentService agentService;

    @Autowired
    public AgentRemoteHandler(@Qualifier("buildRepositoryMessageProducer") BuildRepositoryRemote buildRepositoryRemote, AgentService agentService) {
        this.buildRepositoryRemote = buildRepositoryRemote;
        this.agentService = agentService;
    }

    public void process(Agent agent, Message msg) {
        switch (msg.getAction()) {
            case ping:
                AgentRuntimeInfo info = (AgentRuntimeInfo) msg.getData();
                this.agentSessions.put(info.getUUId(), agent);
                if (info.getCookie() == null) {
                    String cookie = buildRepositoryRemote.getCookie(info.getIdentifier(), info.getLocation());
                    info.setCookie(cookie);
                    if (!agent.send(new Message(Action.setCookie, cookie))) {
                        return;
                    }
                }
                AgentInstruction instruction = this.buildRepositoryRemote.ping(info);
                if (instruction.isShouldCancelJob()) {
                    agent.send(new Message(Action.cancelJob, instruction));
                }
                break;
            case reportCurrentStatus:
                Report report = (Report) msg.getData();
                buildRepositoryRemote.reportCurrentStatus(report.getAgentRuntimeInfo(), report.getJobIdentifier(), report.getJobState());
                break;
            case reportCompleting:
                report = (Report) msg.getData();
                buildRepositoryRemote.reportCompleting(report.getAgentRuntimeInfo(), report.getJobIdentifier(), report.getResult());
                break;
            case reportCompleted:
                report = (Report) msg.getData();
                buildRepositoryRemote.reportCompleted(report.getAgentRuntimeInfo(), report.getJobIdentifier(), report.getResult());
                break;
            default:
                throw new RuntimeException("Unknown action: " + msg.getAction());
        }
    }

    public void remove(Agent agent) {
        for(Map.Entry<String, Agent> entry : agentSessions.entrySet()) {
            if (entry.getValue().equals(agent)) {
                agentSessions.remove(entry.getKey());
                return;
            }
        }
    }

    public Map<String, Agent> connectedAgents() {
        return agentSessions;
    }
}
