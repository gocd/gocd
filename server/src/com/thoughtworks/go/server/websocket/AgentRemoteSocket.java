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

import com.thoughtworks.go.websocket.Message;
import com.thoughtworks.go.websocket.Socket;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@WebSocket
public class AgentRemoteSocket implements Agent, Socket {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentRemoteSocket.class);
    private Executor executor = Executors.newFixedThreadPool(100);
    private AgentRemoteHandler handler;
    private Session session;

    public AgentRemoteSocket(AgentRemoteHandler handler) {
        this.handler = handler;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        LOGGER.debug("{} connected.", sessionName());
    }

    @OnWebSocketMessage
    public void onMessage(InputStream input) {
        Message msg = Message.decode(input);
        LOGGER.debug("{} message: {}", sessionName(), msg);
        handler.process(this, msg);
    }

    @OnWebSocketClose
    public void onClose(int closeCode, String closeReason) {
        LOGGER.debug("{} closed. code: {}, reason: {}", sessionName(), closeCode, closeReason);
        handler.remove(this);
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
    public void send(final Message msg) {
        LOGGER.debug("{} send message: {}", sessionName(), msg);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Message.send(AgentRemoteSocket.this, msg);
                } catch (IOException e) {
                    onError(e);
                }
            }
        });
    }

    private String sessionName() {
        return session == null ? "[No session initialized]" : "Session[" + session.getRemoteAddress() + "]";
    }

    @Override
    public String toString() {
        return "[AgentRemoteSocket: " + sessionName() + "]";
    }

    @Override
    public void sendPartialBytes(ByteBuffer byteBuffer, boolean last) throws IOException {
        session.getRemote().sendPartialBytes(byteBuffer, last);
    }

    @Override
    public int getMaxMessageBufferSize() {
        return session.getPolicy().getMaxBinaryMessageBufferSize();
    }
}
