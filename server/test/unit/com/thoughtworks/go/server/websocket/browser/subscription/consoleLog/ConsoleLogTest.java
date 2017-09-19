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

package com.thoughtworks.go.server.websocket.browser.subscription.consoleLog;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.websocket.browser.BrowserWebSocket;
import com.thoughtworks.go.server.websocket.browser.subscription.WebSocketSubscriptionHandler;
import com.thoughtworks.go.server.websocket.browser.subscription.WebSocketSubscriptionManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConsoleLogTest {
    private ConsoleLog consoleLog;
    private JobIdentifier jobIdentifier;
    @Mock
    private WebSocketSubscriptionManager manager;
    @Mock
    private BrowserWebSocket websocket;
    @Mock
    private WebSocketSubscriptionHandler handler;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        jobIdentifier = new JobIdentifier("foo", -1, "42", "test", "1", "unit");
        jobIdentifier.setBuildId(null);
        jobIdentifier.setPipelineCounter(null);
        consoleLog = new ConsoleLog(jobIdentifier, 0);
    }

    @Test
    public void shouldReturnTheStartLineOfConsoleLog() throws Exception {
        assertThat(consoleLog.getStartLine(), is(0L));
    }

    @Test
    public void shouldSubscribeConsoleLogOverProvidedSession() throws Exception {
        when(manager.getHandler(ConsoleLog.class)).thenReturn(handler);
        consoleLog.subscribe(manager, websocket);

        verify(handler, times(1)).start(consoleLog, websocket);
    }

    @Test
    public void shouldAskSubscriptionForAuthorization() throws Exception {
        when(manager.getHandler(ConsoleLog.class)).thenReturn(handler);
        consoleLog.isAuthorized(manager, websocket);

        verify(handler, times(1)).isAuthorized(consoleLog, websocket);
    }
}