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

package com.thoughtworks.go.agent.statusapi;

import com.thoughtworks.go.util.SystemEnvironment;
import fi.iki.elonen.NanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentStatusHttpd extends NanoHTTPD {

    private static final Logger LOG = LoggerFactory.getLogger(AgentStatusHttpd.class);
    private static final String DEFAULT_MIME_TYPE = "text/plain; charset=utf-8";
    private static final String METHOD_NOT_ALLOWED_MESSAGE = "This method is not allowed. Please use GET or HEAD.";
    private static final String PAGE_NOT_FOUND_MESSAGE = "The page you requested was not found";

    private final Map<String, HttpHandler> routes = new ConcurrentHashMap<>();
    private final HttpHandler isConnectedToServer;
    private final SystemEnvironment environment;
    private final int port;
    private final Set<Method> allowedMethods = new HashSet<>(Arrays.asList(Method.GET, Method.PUT));

    @Autowired
    public AgentStatusHttpd(SystemEnvironment environment,
                            IsConnectedToServerV1 isConnectedToServerV1) {
        super(environment.getAgentStatusHostname(), environment.getAgentStatusPort());
        this.port = environment.getAgentStatusPort();
        this.environment = environment;
        this.isConnectedToServer = isConnectedToServerV1;
        setupRoutes();
    }

    private void setupRoutes() {
        routes.put("/health/v1/isConnectedToServer", isConnectedToServer);
        routes.put("/health/latest/isConnectedToServer", isConnectedToServer);
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (!allowedMethods.contains(session.getMethod())) {
            return methodNotAllowed();
        }
        HttpHandler httpHandler = routes.get(session.getUri());
        if (httpHandler != null) {
            return httpHandler.process();
        } else {
            return notFound();
        }
    }

    private Response methodNotAllowed() {
        return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, DEFAULT_MIME_TYPE, METHOD_NOT_ALLOWED_MESSAGE);
    }

    private Response notFound() {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, DEFAULT_MIME_TYPE, PAGE_NOT_FOUND_MESSAGE);
    }

    @PostConstruct
    public void init() {
        if (!environment.getAgentStatusEnabled()) {
            LOG.info("Agent status HTTP API server has been disabled.");
            return;
        }
        try {
            start();
            LOG.info("Agent status HTTP API server running on http://{}:{}.", getHostname() == null ? "0.0.0.0" : getHostname(), getListeningPort());
        } catch (Exception e) {
            LOG.warn("Could not start agent status HTTP API server on host {}, port {}.", getHostname(), port, e);
        }
    }
}
