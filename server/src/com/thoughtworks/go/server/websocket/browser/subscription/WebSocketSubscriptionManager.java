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


import com.thoughtworks.go.server.websocket.browser.GoWebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSubscriptionManager {
    private final WebSocketSubscriptionHandler subscription;

    @Autowired
    public WebSocketSubscriptionManager(WebSocketSubscriptionHandler subscription) {
        this.subscription = subscription;
    }

    public void subscribe(SubscriptionMessage subscriptionMessage, GoWebSocket webSocket) throws Exception {
        if (subscriptionMessage.isAuthorized(this, webSocket)) {
            subscriptionMessage.subscribe(this, webSocket);
        }
    }

    public WebSocketSubscriptionHandler getSubscription() {
        return subscription;
    }
}
