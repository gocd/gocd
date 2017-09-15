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

import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.server.websocket.SocketHealthService;
import com.thoughtworks.go.server.websocket.browser.subscription.WebSocketSubscriptionManager;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrowserWebSocketCreator implements WebSocketCreator {
    private SocketHealthService socketHealthService;
    private WebSocketSubscriptionManager subscriptionFactory;

    @Autowired
    public BrowserWebSocketCreator(SocketHealthService socketHealthService, WebSocketSubscriptionManager webSocketSubscriptionManager) {
        this.socketHealthService = socketHealthService;
        this.subscriptionFactory = webSocketSubscriptionManager;
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
        return new BrowserWebSocket(socketHealthService, subscriptionFactory, UserHelper.getUserName());
    }
}
