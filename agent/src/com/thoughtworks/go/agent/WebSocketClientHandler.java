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

import com.thoughtworks.go.agent.common.ssl.GoAgentServerWebSocketClientBuilder;
import com.thoughtworks.go.util.URLService;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;

public class WebSocketClientHandler {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketClientHandler.class);

    private WebSocketClient webSocketClient;
    private GoAgentServerWebSocketClientBuilder builder;
    private URLService urlService;

    @Autowired
    public WebSocketClientHandler(GoAgentServerWebSocketClientBuilder builder, URLService urlService) {
        this.builder = builder;
        this.urlService = urlService;
    }

    public Session connect(AgentWebSocketClientController controller)
            throws Exception {
        if (webSocketClient == null || !webSocketClient.isRunning()) {
            if (webSocketClient != null) {
                webSocketClient.stop();
            }
            webSocketClient = builder.build();
            webSocketClient.start();
        }

        LOG.info("Connecting to websocket endpoint: {}", urlService.getAgentRemoteWebSocketUrl());
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        request.addExtensions("fragment;maxLength=" + getMessageBufferSize());
        return webSocketClient.connect(controller, new URI(urlService.getAgentRemoteWebSocketUrl()), request).get();
    }

    private int getMessageBufferSize() {
        return webSocketClient.getPolicy().getMaxBinaryMessageBufferSize();
    }
}
