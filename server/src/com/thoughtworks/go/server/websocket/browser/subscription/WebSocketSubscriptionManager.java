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

import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.websocket.browser.BrowserWebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WebSocketSubscriptionManager {
    private final Map<Class<? extends SubscriptionMessage>, WebSocketSubscriptionHandler> handlers = new HashMap<>();
    private final SecurityService securityService;

    @Autowired
    public WebSocketSubscriptionManager(SecurityService securityService, WebSocketSubscriptionHandler... handlers) {
        this.securityService = securityService;
        for (WebSocketSubscriptionHandler handler : handlers) {
            this.handlers.put(handler.getType(), handler);
        }
    }

    public void subscribe(SubscriptionMessage subscriptionMessage, BrowserWebSocket browserWebSocket) throws Exception {
        WebSocketSubscriptionHandler webSocketSubscriptionHandler = handlers.get(subscriptionMessage.getClass());
        if(webSocketSubscriptionHandler.isAuthorized(subscriptionMessage, securityService, browserWebSocket.getCurrentUser())) {
            webSocketSubscriptionHandler.start(subscriptionMessage, browserWebSocket);
        }
    }
}
