/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.agent.statusapi;

import com.sun.net.httpserver.HttpServer;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;

import static com.thoughtworks.go.util.Pair.pair;

@Component
public class AgentStatusHttpd {

    private static final Logger LOG = LoggerFactory.getLogger(AgentStatusHttpd.class);
    private static final int SERVER_SOCKET_BACKLOG = 10;

    private final IsConnectedToServerV1 isConnectedToServer;
    private final SystemEnvironment environment;
    private HttpServer server;

    @Autowired
    public AgentStatusHttpd(SystemEnvironment environment,
                            IsConnectedToServerV1 isConnectedToServerV1) {
        this.environment = environment;
        this.isConnectedToServer = isConnectedToServerV1;
    }

    private void setupRoutes(HttpServer server) {
        server.createContext("/health/v1/isConnectedToServer", isConnectedToServer);
        server.createContext("/health/latest/isConnectedToServer", isConnectedToServer);
        server.createContext("/", (HttpHandler) () -> pair(HttpStatus.SC_NOT_FOUND, "The page you requested was not found"));
    }

    @PostConstruct
    public void init() {
        if (!environment.getAgentStatusEnabled()) {
            LOG.info("Agent status HTTP API server has been disabled.");
            return;
        }
        InetSocketAddress address = new InetSocketAddress(environment.getAgentStatusHostname(), environment.getAgentStatusPort());
        try {
            server = HttpServer.create(address, SERVER_SOCKET_BACKLOG);
            setupRoutes(server);
            server.start();
            LOG.info("Agent status HTTP API server running on http://{}:{}.", server.getAddress().getHostName(), server.getAddress().getPort());
        } catch (Exception e) {
            LOG.warn("Could not start agent status HTTP API server on host {}, port {}.", address.getHostName(), address.getPort(), e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (server != null) {
            server.stop(0);
        }
    }
}
