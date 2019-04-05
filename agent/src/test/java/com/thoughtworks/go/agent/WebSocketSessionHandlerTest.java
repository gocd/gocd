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
import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.Message;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.extensions.OutgoingFrames;
import org.eclipse.jetty.websocket.common.LogicalConnection;
import org.eclipse.jetty.websocket.common.WebSocketRemoteEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WebSocketSessionHandlerTest {

    private WebSocketSessionHandler handler;
    private Session session;

    @BeforeEach
    void setUp() {
        handler = new WebSocketSessionHandler(new SystemEnvironment());
        session = mock(Session.class);
        handler.setSession(session);
    }

    @Test
    void shouldWaitForAcknowledgementWhileSendingMessages() throws Exception {
        final Message message = new Message(Action.reportCurrentStatus);

        when(session.getRemote()).thenReturn(new FakeWebSocketEndpoint(() -> handler.acknowledge(new Message(Action.acknowledge, message.getAcknowledgementId()))));

        Thread sendThread = new Thread(() -> handler.sendAndWaitForAcknowledgement(message));
        sendThread.start();
        assertThat(sendThread.isAlive()).isTrue();

        sendThread.join();
        assertThat(sendThread.isAlive()).isFalse();
    }

    @Test
    void shouldReturnTrueIfNotRunning() throws Exception {
        assertThat(handler.isNotRunning()).isTrue();
    }

    @Test
    void shouldReturnFalseIfRunning() throws Exception {
        when(session.isOpen()).thenReturn(true);
        assertThat(handler.isNotRunning()).isFalse();
    }

    @Test
    void shouldSetSessionNameToNoSessionWhenStopped() throws Exception {
        when(session.isOpen()).thenReturn(true);
        when(session.getRemoteAddress()).thenReturn(null);
        handler.stop();
        assertThat(handler.getSessionName()).isEqualTo("[No Session]");
    }

    @Test
    void shouldSetSessionToNullWhenStopped() throws Exception {
        when(session.isOpen()).thenReturn(true);
        when(session.getRemoteAddress()).thenReturn(null);
        handler.stop();
        verify(session).close();
        assertThat(handler.isNotRunning()).isTrue();
    }

    class FakeWebSocketEndpoint extends WebSocketRemoteEndpoint {
        private Runnable runnable;

        FakeWebSocketEndpoint(Runnable runnable) {
            super(mock(LogicalConnection.class), mock(OutgoingFrames.class));
            this.runnable = runnable;
        }

        @Override
        public Future<Void> sendBytesByFuture(ByteBuffer data) {
            runnable.run();
            return null;
        }
    }
}
