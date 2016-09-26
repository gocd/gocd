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
import com.thoughtworks.go.agent.JobRunner;
import com.thoughtworks.go.agent.WebSocketAgentController;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClientBuilder;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.URLService;
import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.Message;
import com.thoughtworks.go.websocket.MessageEncoding;
import com.thoughtworks.go.websocket.Report;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.ByteBuffer;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@Component
public class AgentWebSocketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentWebSocketService.class);
    private Session session;
    private final SystemEnvironment environment;
    private URLService urlService;
    private WebSocketClient client;

    @Autowired
    public AgentWebSocketService(SystemEnvironment environment, URLService urlService) {
        this.environment = environment;
        this.urlService = urlService;
    }

    public synchronized void start(AgentController agentController) throws Exception {
        GoAgentServerHttpClientBuilder builder = new GoAgentServerHttpClientBuilder(environment);
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStore(builder.agentKeystore());
        sslContextFactory.setKeyStorePassword(builder.keystorePassword());
        sslContextFactory.setKeyManagerPassword(builder.keystorePassword());
        sslContextFactory.setTrustStore(builder.agentTruststore());
        sslContextFactory.setTrustStorePassword(builder.keystorePassword());
        sslContextFactory.setWantClientAuth(true);
        if (client == null || client.isStopped()) {
            client = new WebSocketClient(sslContextFactory);
            client.setMaxIdleTimeout(environment.getWebsocketMaxIdleTime());
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
