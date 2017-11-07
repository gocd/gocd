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

import com.thoughtworks.go.domain.activity.JobStatusCache;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.websocket.browser.BrowserWebSocket;

import java.io.IOException;

public class ServerHealthMessageCount extends SubscriptionMessage {

    public ServerHealthMessageCount() {
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ServerHealthMessageCount)) return false;
        return true;
    }

    @Override
    public boolean isAuthorized(SecurityService securityService, Username currentUser) {
        return false;
    }

    @Override
    public void start(BrowserWebSocket socket, WebSocketSubscriptionHandler handler) throws IOException {

    }
}
