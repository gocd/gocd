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

package com.thoughtworks.go.agent.service;

import com.thoughtworks.go.agent.AgentController;
import com.thoughtworks.go.agent.JobRunner;
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
import com.thoughtworks.go.websocket.Report;
import com.thoughtworks.go.websocket.Socket;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
@WebSocket
public class AgentWebsocketService implements Socket {
    public static class BuildRepositoryRemoteAdapter implements BuildRepositoryRemote {
        private JobRunner runner;
        private AgentWebsocketService service;

        public BuildRepositoryRemoteAdapter(JobRunner runner, AgentWebsocketService service) {
            this.runner = runner;
            this.service = service;
        }
        @Override
        public AgentInstruction ping(AgentRuntimeInfo info) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Work getWork(AgentRuntimeInfo runtimeInfo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void reportCurrentStatus(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobState jobState) {
            service.send(new Message(Action.reportCurrentStatus, new Report(agentRuntimeInfo, jobIdentifier, jobState)));
        }

        @Override
        public void reportCompleting(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobResult result) {
            service.send(new Message(Action.reportCompleting, new Report(agentRuntimeInfo, jobIdentifier, result)));
        }

        @Override
        public void reportCompleted(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobResult result) {
            service.send(new Message(Action.reportCompleted, new Report(agentRuntimeInfo, jobIdentifier, result)));
        }

        @Override
        public boolean isIgnored(JobIdentifier jobIdentifier) {
            return runner.isJobCancelled();
        }

        @Override
        public String getCookie(AgentIdentifier identifier, String location) {
            throw new UnsupportedOperationException();
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentWebsocketService.class);
    private AgentController controller;
    private Session session;
    private URLService urlService;
    private WebSocketClient client;
    private Executor executor = Executors.newFixedThreadPool(5);

    @Autowired
    public AgentWebsocketService(URLService urlService) {
        this.urlService = urlService;
    }

    public synchronized void start() throws Exception {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(SslInfrastructureService.AGENT_CERTIFICATE_FILE.getAbsolutePath());
        sslContextFactory.setKeyStorePassword(SslInfrastructureService.AGENT_STORE_PASSWORD);
        sslContextFactory.setKeyManagerPassword(SslInfrastructureService.AGENT_STORE_PASSWORD);
        sslContextFactory.setTrustStorePath(SslInfrastructureService.AGENT_TRUST_FILE.getAbsolutePath());
        sslContextFactory.setTrustStorePassword(SslInfrastructureService.AGENT_STORE_PASSWORD);
        sslContextFactory.setWantClientAuth(true);
        if (client == null || client.isStopped()) {
            SystemEnvironment environment = new SystemEnvironment();
            client = new WebSocketClient(sslContextFactory);
            client.setMaxIdleTimeout(environment.getWebsocketMaxIdleTime());
            client.start();
        }
        if (session != null) {
            session.close();
        }
        LOGGER.info("Connecting to websocket endpoint: " + urlService.getAgentRemoteWebSocketUrl());
        session = client.connect(this, new URI(urlService.getAgentRemoteWebSocketUrl()), new ClientUpgradeRequest()).get();
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
        try {
            Message.send(this, message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        LOGGER.info(session + " connected.");
    }

    @OnWebSocketMessage
    public void onMessage(InputStream raw) {
        final Message msg = Message.decode(raw);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sessionName() + " message: " + msg);
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    AgentWebsocketService.this.controller.process(msg);
                } catch (InterruptedException e) {
                    LOGGER.error("Process message[" + msg + "] is interruptted.", e);
                } catch (RuntimeException e) {
                    LOGGER.error("Unexpected error while processing message[" + msg + "]: " + e.getMessage(), e);
                }
            }
        });
    }

    @OnWebSocketClose
    public void onClose(int closeCode, String closeReason) {
        LOGGER.info(sessionName() + " closed.");
    }

    @OnWebSocketError
    public void onError(Throwable error) {
        LOGGER.error(sessionName() + " error", error);
    }

    @OnWebSocketFrame
    public void onFrame(Frame frame) {
        LOGGER.debug("{} receive frame: {}", sessionName(), frame.getPayloadLength());
    }

    @Override
    public void sendPartialBytes(ByteBuffer byteBuffer, boolean last) throws IOException {
        session.getRemote().sendPartialBytes(byteBuffer, last);
    }

    @Override
    public int getMaxMessageBufferSize() {
        return session.getPolicy().getMaxBinaryMessageBufferSize();
    }

    private String sessionName() {
        return session == null ? "[No session initialized]" : "Session[" + session.getRemoteAddress() + "]";
    }

    public void setController(AgentController controller) {
        this.controller = controller;
    }

}
