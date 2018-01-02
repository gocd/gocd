/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.JobIdentifier;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Predicate;

@WebSocket
public class ConsoleLogSocket implements SocketEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleLogSocket.class);

    private final JobIdentifier jobIdentifier;
    private final ConsoleLogSender handler;
    private Session session;
    private String sessionId;
    private String key;
    private SocketHealthService socketHealthService;

    ConsoleLogSocket(ConsoleLogSender handler, JobIdentifier jobIdentifier, SocketHealthService socketHealthService) {
        this.handler = handler;
        this.jobIdentifier = jobIdentifier;
        this.key = String.format("%s:%d", jobIdentifier, hashCode());
        this.socketHealthService = socketHealthService;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) throws Exception {
        this.session = session;
        socketHealthService.register(this);
        LOGGER.debug("{} connected", sessionName());


        long start = parseStartLine(session.getUpgradeRequest());
        LOGGER.debug("{} sending logs for {} starting at line {}.", sessionName(), jobIdentifier, start);

        try {
            handler.process(this, jobIdentifier, start);
        } catch (IOException e) {
            if ("Connection output is closed".equals(e.getMessage())) {
                LOGGER.debug("{} client (likely, browser) closed connection prematurely.", sessionName());
                close(); // for good measure
            } else {
                throw e;
            }
        }
    }

    @OnWebSocketError
    public void onError(Throwable error) {
        LOGGER.error("{} closing session because an error was thrown", sessionName(), error);
        try {
            close(StatusCode.SERVER_ERROR, error.getMessage());
        } finally {
            socketHealthService.deregister(this);
        }
    }

    @OnWebSocketClose
    public void onClose(int status, String reason) {
        socketHealthService.deregister(this);
    }

    @Override
    public void send(ByteBuffer data) throws IOException {
        session.getRemote().sendBytes(data);
    }

    @Override
    public void ping() throws IOException {
        session.getRemote().sendString(WebsocketMessages.PING);
    }

    @Override
    public boolean isOpen() {
        return session.isOpen();
    }

    @Override
    public void close() {
        close(StatusCode.NORMAL, null);
    }

    @Override
    public void close(int code, String reason) {
        session.close(code, reason);
    }

    @Override
    public String key() {
        return key;
    }

    private String sessionName() {
        if (null == sessionId) {
            if (null == session) throw new IllegalStateException(String.format("Cannot get session name because the session has not been assigned to socket %s", key()));
            sessionId = String.format("Session[%s:%s]", session.getRemoteAddress(), key());
        }
        return sessionId;
    }

    private long parseStartLine(UpgradeRequest request) {
        Optional<NameValuePair> startLine = URLEncodedUtils.parse(request.getRequestURI(), "UTF-8").
                stream().
                filter(new Predicate<NameValuePair>() {
                    @Override
                    public boolean test(NameValuePair pair) {
                        return "startLine".equals(pair.getName());
                    }
                }).findFirst();

        return startLine.isPresent() ? Long.valueOf(startLine.get().getValue()) : 0L;
    }

}
