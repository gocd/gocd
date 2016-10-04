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

package com.thoughtworks.go.agent.service;

import com.thoughtworks.go.agent.AgentController;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerWebSocketClientBuilder;
import com.thoughtworks.go.util.URLService;
import com.thoughtworks.go.websocket.Message;
import com.thoughtworks.go.websocket.MessageEncoding;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.ByteBuffer;

@Component
public class AgentWebSocketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentWebSocketService.class);
    private Session session;
    private GoAgentServerWebSocketClientBuilder builder;
    private URLService urlService;
    private WebSocketClient client;

    @Autowired
    public AgentWebSocketService(GoAgentServerWebSocketClientBuilder builder, URLService urlService) {
        this.builder = builder;
        this.urlService = urlService;
    }

    public synchronized void start(AgentController agentController) throws Exception {
        if (client == null || client.isStopped()) {
            client = builder.build();
            client.start();
        }
        if (session != null) {
            session.close();
        }
        LOGGER.info("Connecting to websocket endpoint: " + urlService.getAgentRemoteWebSocketUrl());
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        request.addExtensions("fragment;maxLength=" + client.getPolicy().getMaxBinaryMessageBufferSize());
        session = client.connect(agentController, new URI(urlService.getAgentRemoteWebSocketUrl()), request).get();
    }

    public synchronized void stop() {
        if (isRunning()) {
            LOGGER.debug("close {}", sessionName());
            session.close();
            session = null;
        }
    }

    public synchronized boolean isRunning() {
        return session != null && session.isOpen();
    }

    public void send(Message message) {
        LOGGER.debug("{} send message: {}", sessionName(), message);
        session.getRemote().sendBytesByFuture(ByteBuffer.wrap(MessageEncoding.encodeMessage(message)));
    }

    private String sessionName() {
        return session == null ? "[No session initialized]" : "Session[" + session.getRemoteAddress() + "]";
    }

}
