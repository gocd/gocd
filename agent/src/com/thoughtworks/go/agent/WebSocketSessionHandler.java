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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.websocket.Message;
import com.thoughtworks.go.websocket.MessageCallback;
import com.thoughtworks.go.websocket.MessageEncoding;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class WebSocketSessionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketSessionHandler.class);

    // This is a session aware socket
    private Session session;
    private String sessionName = "[No Session]";
    private final Map<String, MessageCallback> callbacks = new ConcurrentHashMap<>();
    private SystemEnvironment systemEnvironment;

    @Autowired
    public WebSocketSessionHandler(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    synchronized void stop() {
        if (isRunning()) {
            LOG.debug("close {}", sessionName());
            session.close();
            session = null;
            sessionName = "[No Session]";
        }
    }

    private synchronized boolean isRunning() {
        return session != null && session.isOpen();
    }

    synchronized boolean isNotRunning() {
        return !isRunning();
    }

    private void send(Message message) {
        for (int retries = 1; retries <= systemEnvironment.getWebsocketSendRetryCount(); retries++) {
            try {
                LOG.debug("{} attempt {} to send message: {}", sessionName(), retries, message);
                session.getRemote().sendBytesByFuture(ByteBuffer.wrap(MessageEncoding.encodeMessage(message)));
                break;
            } catch (Throwable e) {
                try {
                    LOG.debug("{} attempt {} failed to send message: {}.", sessionName(), retries, message);
                    if (retries == systemEnvironment.getWebsocketSendRetryCount()) {
                        bomb(e);
                    }
                    Thread.sleep(2000L);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    boolean sendAndWaitForAcknowledgement(Message message) {
        final CountDownLatch wait = new CountDownLatch(1);
        sendWithCallback(message, new MessageCallback() {
            @Override
            public void call() {
                wait.countDown();
            }
        });
        try {
            return wait.await(systemEnvironment.getWebsocketAckMessageTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            bomb(e);
        }
        return false;
    }

    private void sendWithCallback(Message message, MessageCallback callback) {
        callbacks.put(message.getAcknowledgementId(), callback);
        send(message);
    }

    private String sessionName() {
        return session == null ? "[No session initialized]" : "Session[" + session.getRemoteAddress() + "]";
    }

    void setSession(Session session) {
        this.session = session;
        this.sessionName = "[" + session.getRemoteAddress() + "]";
    }

    String getSessionName() {
        return sessionName;
    }

    void acknowledge(Message message) {
        String acknowledgementId = MessageEncoding.decodeData(message.getData(), String.class);
        LOG.debug("Acknowledging {}", acknowledgementId);
        callbacks.remove(acknowledgementId).call();
    }

    void clearCallBacks() {
        LOG.debug("Clearing {} ignored messages", callbacks.size());
        callbacks.clear();
    }
}
