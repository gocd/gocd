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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.Message;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class WebSocketSessionHandlerTest {

    private WebSocketSessionHandler handler;
    private Session session;

    @Before
    public void setUp() throws Exception {
        handler = new WebSocketSessionHandler(new SystemEnvironment());
        session = mock(Session.class);
        RemoteEndpoint remoteEndpoint = mock(RemoteEndpoint.class);
        when(session.getRemote()).thenReturn(remoteEndpoint);
        handler.setSession(session);
    }

    @Test
    public void shouldWaitForAcknowledgementWhileSendingMessages() throws Exception {
        final Message message = new Message(Action.reportCurrentStatus);

        Thread sendThread = new Thread(new Runnable() {
            @Override
            public void run() {
                handler.sendAndWaitForAcknowledgement(message);
            }
        });
        sendThread.start();
        assertThat(sendThread.isAlive(), is(true));

        handler.acknowledge(new Message(Action.acknowledge, message.getAcknowledgementId()));

        sendThread.join();
        assertThat(sendThread.isAlive(), is(false));
    }

    @Test
    public void shouldReturnTrueIfNotRunning() throws Exception {
        assertThat(handler.isNotRunning(), is(true));
    }

    @Test
    public void shouldReturnFalseIfRunning() throws Exception {
        when(session.isOpen()).thenReturn(true);
        assertThat(handler.isNotRunning(), is(false));
    }

    @Test
    public void shouldSetSessionNameToNoSessionWhenStopped() throws Exception {
        when(session.isOpen()).thenReturn(true);
        when(session.getRemoteAddress()).thenReturn(null);
        handler.stop();
        assertThat(handler.getSessionName(), is("[No Session]"));
    }

    @Test
    public void shouldSetSessionToNullWhenStopped() throws Exception {
        when(session.isOpen()).thenReturn(true);
        when(session.getRemoteAddress()).thenReturn(null);
        handler.stop();
        verify(session).close();
        assertThat(handler.isNotRunning(), is(true));
    }
}