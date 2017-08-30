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

import com.thoughtworks.go.server.service.RestfulService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.websocket.browser.GoWebSocket;
import com.thoughtworks.go.server.websocket.browser.subscription.WebSocketSubscriptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class ConsoleLogSubscription implements WebSocketSubscriptionHandler<ConsoleLog> {
    private final ConsoleLogSender consoleLogSender;
    private final SecurityService securityService;
    private RestfulService restfulService;

    @Autowired
    public ConsoleLogSubscription(ConsoleLogSender consoleLogSender, SecurityService securityService, RestfulService restfulService) {
        this.consoleLogSender = consoleLogSender;
        this.securityService = securityService;
        this.restfulService = restfulService;
    }

    @Override
    public void start(ConsoleLog message, GoWebSocket socket) throws Exception {
        consoleLogSender.process(socket, restfulService.findJob(message.getJobIdentifier()), message.getStartLine());
    }

    @Override
    public boolean isAuthorized(ConsoleLog message, GoWebSocket webSocket) {
        return securityService.hasViewPermissionForPipeline(webSocket.getCurrentUser(), message.getJobIdentifier().getPipelineName());
    }
}
