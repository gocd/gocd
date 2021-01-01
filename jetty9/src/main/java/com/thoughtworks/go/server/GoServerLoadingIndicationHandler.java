/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server;

import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.thoughtworks.go.util.SystemEnvironment.LOADING_PAGE;
import static org.eclipse.jetty.http.MimeTypes.Type.*;

/** When GoCD is starting. This is the only handler that will be active (till the web application context handler is up).
 *  During that time, this handler shows a 503 for all requests, while waiting for the rest of the server to be up.
 */
class GoServerLoadingIndicationHandler extends ContextHandler {
    private WebAppContext webAppContext;
    private boolean isWebAppStarting;
    private SystemEnvironment systemEnvironment;

    GoServerLoadingIndicationHandler(WebAppContext webAppContext, SystemEnvironment systemEnvironment) {
        setContextPath("/");
        setHandler(new LoadingHandler());

        this.webAppContext = webAppContext;
        this.isWebAppStarting = !webAppContext.isAvailable();
        this.systemEnvironment = systemEnvironment;
    }

    private class LoadingHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (isWebAppStarting()) {
                handleQueriesWhenWebAppIsStarting(target, baseRequest, request, response);
            } else if ("/".equals(target)) {
                addHeaders(response);
                response.sendRedirect(GoConstants.GO_URL_CONTEXT + systemEnvironment.landingPage());
            }
        }

        private void handleQueriesWhenWebAppIsStarting(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
            if (acceptHeaderValue(baseRequest).contains("json")) {
                respondWith503(baseRequest, response, APPLICATION_JSON.asString(), "{ \"message\": \"GoCD server is starting\" }");
            } else if (acceptHeaderValue(baseRequest).contains("html")) {
                respondWith503(baseRequest, response, TEXT_HTML.asString(), loadingPage());
            } else {
                respondWith503(baseRequest, response, TEXT_PLAIN.asString(), "GoCD server is starting");
            }
        }

        private void respondWith503(Request baseRequest, HttpServletResponse response, String contentType, String body) throws IOException {
            addHeaders(response);
            response.setStatus(org.eclipse.jetty.http.HttpStatus.SERVICE_UNAVAILABLE_503);
            response.setContentType(contentType);
            response.getWriter().println(body);
            baseRequest.setHandled(true);
        }

        private String acceptHeaderValue(Request baseRequest) {
            List<String> qualityCSV = baseRequest.getHttpFields().getQualityCSV(HttpHeader.ACCEPT);
            return qualityCSV.isEmpty() ? MimeTypes.Type.TEXT_HTML.asString() : qualityCSV.get(0);
        }

        private boolean isWebAppStarting() {
            if (!isWebAppStarting) {
                return false;
            }

            isWebAppStarting = !webAppContext.isAvailable();
            return isWebAppStarting;
        }

        private void addHeaders(HttpServletResponse response) {
            response.setHeader("Cache-Control", "no-cache, must-revalidate, no-store");
            response.setHeader("X-XSS-Protection", "1; mode=block");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "SAMEORIGIN");
            response.setHeader("X-UA-Compatible", "chrome=1");
        }

    }

    private String loadingPage() {
        try {
            return IOUtils.toString(getClass().getResource(systemEnvironment.get(LOADING_PAGE)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "<h2>GoCD is starting up. Please wait ....</h2>";
        }
    }
}
