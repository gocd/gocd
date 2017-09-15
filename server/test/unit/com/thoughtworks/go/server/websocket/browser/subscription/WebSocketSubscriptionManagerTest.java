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

package com.thoughtworks.go.server.websocket.browser.subscription;

import com.thoughtworks.go.server.websocket.browser.BrowserWebSocket;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class WebSocketSubscriptionManagerTest {

    private WebSocketSubscriptionManager manager;
    @Autowired
    private WebSocketSubscriptionHandler handler;
    @Mock
    private SubscriptionMessage message;
    @Mock
    private BrowserWebSocket websocket;


    @Before
    public void setUp() throws Exception {
        initMocks(this);
        manager = new WebSocketSubscriptionManager(handler);
    }

    @Test
    public void shouldSubscribeTheIncomingSubscriptionRequest() throws Exception {
        when(message.isAuthorized(manager, websocket)).thenReturn(true);
        manager.subscribe(message, websocket);

        verify(message, times(1)).isAuthorized(manager, websocket);
        verify(message, times(1)).subscribe(manager, websocket);
    }

    @Test
    public void shouldNotSubscribeForUnauthorizedSubscriptionRequest() throws Exception {
        when(message.isAuthorized(manager, websocket)).thenReturn(false);
        manager.subscribe(message, websocket);

        verify(message, times(1)).isAuthorized(manager, websocket);
        verify(message, times(0)).subscribe(manager, websocket);
    }
}