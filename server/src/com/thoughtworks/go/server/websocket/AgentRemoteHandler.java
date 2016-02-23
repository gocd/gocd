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
import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.Message;
import com.thoughtworks.go.websocket.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentRemoteHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentRemoteHandler.class);
    private Map<Agent, String> sessionIds = new ConcurrentHashMap<>();
    private Map<Agent, String> agentCookie = new ConcurrentHashMap<>();
    private Map<String, Agent> agentSessions = new ConcurrentHashMap<>();

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
                // It is possible that server receives multiple same agent ping messages due to network issues or agent ping bug, no matter what we do at agent side.
                // For identifying an agent that uses duplicated uuid, we have cookie system in place. Here we need synchronize to
                // make sure we assign only one cookie to an agent.
                // buildRepositoryRemote#getCookie is confusing, it actually generate new cookie every time you call it.
                // In websocket case, we don't need cookie, because websocket itself can be used to identify 2 same UUID agents.
                // But before we remove old server agent communication code, we need maintain it.
                // We can clean up cookie stuff after we switched over to websocket.
                synchronized (agent) {
                    if (!sessionIds.containsKey(agent)) {
                        LOGGER.info("{} is connected with websocket {}", info.getIdentifier(), agent);
                        sessionIds.put(agent, info.getUUId());
                        this.agentSessions.put(info.getUUId(), agent);
                    }
                    if (info.getCookie() == null) {
                        String cookie = agentCookie.get(agent);
                        if (cookie == null) {
                            cookie = buildRepositoryRemote.getCookie(info.getIdentifier(), info.getLocation());
                            agentCookie.put(agent, cookie);
                        }
                        info.setCookie(cookie);
                        agent.send(new Message(Action.setCookie, cookie));
                    }
                    AgentInstruction instruction = this.buildRepositoryRemote.ping(info);
                    if (instruction.isShouldCancelJob()) {
                        agent.send(new Message(Action.cancelJob));
                    }
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
        agentCookie.remove(agent);
        String uuid = sessionIds.remove(agent);
        if (uuid == null) {
            return;
        }
        agentSessions.remove(uuid);
        AgentInstance instance = agentService.findAgent(uuid);
        if (instance != null) {
            instance.lostContact();
            LOGGER.info("{} lost contact because websocket connection is closed", instance.getAgentIdentifier());
        }
    }

    public Map<String, Agent> connectedAgents() {
        return agentSessions;
    }

    public void sendCancelMessage(String uuid) {
        if (uuid == null) {
            return;
        }
        Agent agent = agentSessions.get(uuid);
        if (agent != null) {
            agent.send(new Message(Action.cancelJob));
        }
    }
}
