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

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.command.StreamConsumer;
import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.ConsoleTransmission;
import com.thoughtworks.go.websocket.Message;
import com.thoughtworks.go.websocket.MessageEncoding;

public class ConsoleOutputWebsocketTransmitter implements StreamConsumer {
    private WebSocketSessionHandler webSocketSessionHandler;
    private String buildId;

    public ConsoleOutputWebsocketTransmitter(WebSocketSessionHandler webSocketSessionHandler, String buildId) {
        this.webSocketSessionHandler = webSocketSessionHandler;
        this.buildId = buildId;
    }

    @Override
    public void consumeLine(String line) {
        ConsoleTransmission transmission = new ConsoleTransmission(line, buildId);
        this.webSocketSessionHandler.sendAndWaitForAcknowledgement(new Message(Action.consoleOut, MessageEncoding.encodeData(transmission)));
    }
}