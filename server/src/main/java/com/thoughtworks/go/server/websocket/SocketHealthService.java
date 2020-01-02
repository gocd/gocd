/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SocketHealthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketHealthService.class);

    private ConcurrentHashMap<String, SocketEndpoint> connections = new ConcurrentHashMap<>();

    public void register(SocketEndpoint socket) {
        connections.put(socket.key(), socket);
    }

    public void deregister(SocketEndpoint socket) {
        connections.remove(socket.key());
    }

    public void keepalive() {
        connections.forEachValue(25, socket -> {
            try {
                if (socket.isOpen()) {
                    socket.ping();
                }
            } catch (IOException e) {
                if ("Connection output is closed".equals(e.getMessage())) {
                    deregister(socket);
                    socket.close();
                }
                LOGGER.error("Failed to ping socket %s", socket.key(), e);
            }
        });
    }
}
