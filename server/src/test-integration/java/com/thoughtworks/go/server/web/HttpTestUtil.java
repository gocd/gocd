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
package com.thoughtworks.go.server.web;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.UrlEncoded;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Understands test http server that is used to test http client code end-to-end
 * <p>
 * Flicked from https://github.com/test-load-balancer/tlb (pre http-components) e19d4911b089eeaf1a2c
 */
public class HttpTestUtil {

    private final Server server;
    private Thread blocker;

    private static final int MAX_IDLE_TIME = 30000;

    public static class EchoServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws IOException {
            handleRequest(request, resp);
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse resp) throws IOException {
            handleRequest(request, resp);
        }

        @Override
        protected void doPut(HttpServletRequest request, HttpServletResponse resp) throws IOException {
            handleRequest(request, resp);
        }

        private void handleRequest(HttpServletRequest request, HttpServletResponse resp) throws IOException {
            try (PrintWriter writer = resp.getWriter()) {
                Request req = (Request) request;

                writer.write(req.getRequestURL().toString());

                if (req.getQueryParameters() != null) {
                    String query = UrlEncoded.encode(req.getQueryParameters(), StandardCharsets.UTF_8, true);
                    writer.write(query.isBlank() ? "" : "?" + query);
                }
            }
        }
    }

    public interface ContextCustomizer {
        void customize(WebAppContext ctx) throws Exception;
    }

    public HttpTestUtil(final ContextCustomizer customizer) throws Exception {
        server = new Server();
        WebAppContext ctx = new WebAppContext();
        SessionHandler sh = new SessionHandler();
        ctx.setSessionHandler(sh);
        customizer.customize(ctx);
        ctx.setContextPath("/go");
        server.setHandler(ctx);
    }

    public void httpConnector(final int port) {
        ServerConnector connector = connectorWithPort(port);
        server.addConnector(connector);
    }

    private ServerConnector connectorWithPort(int port) {
        ServerConnector http = new ServerConnector(server);
        http.setPort(port);
        http.setIdleTimeout(MAX_IDLE_TIME);
        return http;
    }

    public synchronized void start() throws InterruptedException {
        if (blocker != null)
            throw new IllegalStateException("Aborting server start, it seems server is already running.");

        blocker = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                Thread.sleep(Integer.MAX_VALUE);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        });
        blocker.start();
        while (!server.isStarted()) {
            Thread.sleep(50);
        }
    }

    public synchronized void stop() {
        if (blocker == null)
            throw new IllegalStateException("Aborting server stop, it seems there is no server running.");

        try {
            server.stop();
            blocker.interrupt();
            blocker.join();
            blocker = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
