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

import com.thoughtworks.go.server.domain.ErrorsAndWarningsListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.websocket.browser.BrowserWebSocket;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthStates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

@Component
public class ServerHealthMessagesCountChangeSubscriptionHandler implements WebSocketSubscriptionHandler {
    private ServerHealthService serverHealthService;
    private int errorCount = 0;
    private int warningCount = 0;

    @Autowired
    public ServerHealthMessagesCountChangeSubscriptionHandler(ServerHealthService serverHealthService) {
        this.serverHealthService = serverHealthService;
    }

    @Override
    public void start(SubscriptionMessage message, BrowserWebSocket socket) throws Exception {
        message.start(socket, this);
    }

    @Override
    public boolean isAuthorized(SubscriptionMessage message, SecurityService securityService, Username currentUser) {
        return message.isAuthorized(securityService, currentUser);
    }

    @Override
    public Class getType() {
        return ServerHealthMessageCount.class;
    }

    public void registerErrorsAndWarningsCountChangeListener(BrowserWebSocket socket) {
        serverHealthService.addErrorsAndWarningsChangeListener(new ErrorsAndWarningsListener() {
            @Override
            public void errorsAndWarningsChanged(ServerHealthStates messages) {
                if (messages.warningCount() != warningCount || messages.errorCount() != errorCount) {
                    warningCount = messages.warningCount();
                    errorCount = messages.errorCount();

                    send(warningCount, errorCount, socket);
                }
            }
        });
    }

    public void sendCurrentErrorsAndWarningsCount(BrowserWebSocket socket) {
        ServerHealthStates logs = serverHealthService.getAllLogs();
        send(logs.warningCount(), logs.errorCount(), socket);
    }

    private void send(int warningCount, int errorCount, BrowserWebSocket socket) {
        String res = "{" +
                "  \"type\":\"ServerHealthMessageCount\"," +
                "  \"response\":{" +
                "    \"errorsCount\":" + warningCount + "," +
                "    \"warningsCount\":" + errorCount + "" +
                "  }" +
                "}";
        
        try {
            socket.send(ByteBuffer.wrap(res.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
