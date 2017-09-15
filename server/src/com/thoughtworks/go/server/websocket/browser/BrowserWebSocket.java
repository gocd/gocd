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

package com.thoughtworks.go.server.websocket.browser;

import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.websocket.SocketEndpoint;
import com.thoughtworks.go.server.websocket.SocketHealthService;
import com.thoughtworks.go.server.websocket.browser.subscription.SubscriptionMessage;
import com.thoughtworks.go.server.websocket.browser.subscription.WebSocketSubscriptionManager;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Predicate;

@WebSocket
public class BrowserWebSocket implements SocketEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserWebSocket.class);
    private static final String PING = "{\"type\":\"ping\"}";
    private final String key;
    private WebSocketSubscriptionManager subscriptionManager;
    private Username currentUser;

    private Session session;
    private String sessionId;

    private SocketHealthService socketHealthService;

    BrowserWebSocket(SocketHealthService socketHealthService, WebSocketSubscriptionManager subscriptionManager, Username currentUser) {
        this.socketHealthService = socketHealthService;
        this.subscriptionManager = subscriptionManager;
        this.currentUser = currentUser;
        this.key = UUID.randomUUID().toString();
    }

    @OnWebSocketConnect
    public void onConnect(Session session) throws Exception {
        this.session = session;
        socketHealthService.register(this);
        LOGGER.debug("{} connected", sessionName());
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

    @OnWebSocketMessage
    public void onMessage(Session session, String input) throws Exception {
        List<SubscriptionMessage> subscriptionMessages = SubscriptionMessage.fromJSON(input);
        for (SubscriptionMessage subscriptionMessage : subscriptionMessages) {
            try {
                subscriptionManager.subscribe(subscriptionMessage, this);
            } catch (Exception e) {
                LOGGER.debug("There was an error subscribing {} to {}", getCurrentUser(), subscriptionMessage);
                session.close();
            }
        }
    }

    public Username getCurrentUser() {
        return currentUser;
    }

    @Override
    public void send(ByteBuffer data) throws IOException {
        session.getRemote().sendBytes(data);
    }

    @Override
    public void ping() throws IOException {
        session.getRemote().sendString(PING);
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
            if (null == session) {
                throw new IllegalStateException(String.format("Cannot get session name because the session has not been assigned to socket %s", key()));
            }
            sessionId = String.format("Session[%s:%s]", session.getRemoteAddress(), key());
        }
        return sessionId;
    }

    public List<String> parseEvents(UpgradeRequest request) {
        Optional<NameValuePair> events = URLEncodedUtils.parse(request.getRequestURI(), "UTF-8").
                stream().
                filter(new Predicate<NameValuePair>() {
                    @Override
                    public boolean test(NameValuePair pair) {
                        return "events".equals(pair.getName());
                    }
                }).findFirst();

        return events.isPresent() ? Arrays.asList(events.get().getValue().split(",")) : new ArrayList<>();
    }

    public Session getSession() {
        return session;
    }
}
