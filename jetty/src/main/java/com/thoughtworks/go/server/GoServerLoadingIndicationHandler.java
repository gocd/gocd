/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.ee8.webapp.WebAppContext;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.eclipse.jetty.http.MimeTypes.Type.*;

/** When GoCD is starting. This is the only handler that will be active (till the web application context handler is up).
 *  During that time, this handler shows a 503 for all requests, while waiting for the rest of the server to be up.
 */
class GoServerLoadingIndicationHandler extends ContextHandler {
    private final WebAppContext webAppContext;
    private final SystemEnvironment systemEnvironment;
    private volatile boolean isWebAppStarting;

    GoServerLoadingIndicationHandler(WebAppContext webAppContext, SystemEnvironment systemEnvironment) {
        setContextPath("/");
        setHandler(new LoadingHandler());

        this.webAppContext = webAppContext;
        this.isWebAppStarting = !webAppContext.isAvailable();
        this.systemEnvironment = systemEnvironment;
    }

    private class LoadingHandler extends Handler.Abstract {
        @Override
        public boolean handle(Request request, Response response, Callback callback) throws IOException {
            if (isWebAppStarting()) {
                handleQueriesWhenWebAppIsStarting(request, response);
                return true;
            } else if ("/".equals(request.getHttpURI().getPath())) {
                addHeaders(response.getHeaders());
                Response.sendRedirect(request, response, callback, systemEnvironment.getLandingPage());
                return true;
            }
            return false;
        }

        private void handleQueriesWhenWebAppIsStarting(Request baseRequest, Response response) throws IOException {
            if (acceptHeaderValue(baseRequest).contains("json")) {
                respondWith503(response, APPLICATION_JSON.asString(), "{ \"message\": \"GoCD server is starting\" }");
            } else if (acceptHeaderValue(baseRequest).contains("html")) {
                respondWith503(response, TEXT_HTML.asString(), loadingPage());
            } else {
                respondWith503(response, TEXT_PLAIN.asString(), "GoCD server is starting");
            }
        }

        private void respondWith503(Response response, String contentType, String body) {
            addHeaders(response.getHeaders());
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE_503);
            response.getHeaders().put(HttpHeader.CONTENT_TYPE, contentType);
            response.write(true, ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8)), null);
        }

        private String acceptHeaderValue(Request request) {
            List<String> qualityCSV = request.getHeaders().getQualityCSV(HttpHeader.ACCEPT);
            return qualityCSV.isEmpty() ? MimeTypes.Type.TEXT_HTML.asString() : qualityCSV.getFirst();
        }

        private boolean isWebAppStarting() {
            if (!isWebAppStarting) {
                return false;
            }

            isWebAppStarting = !webAppContext.isAvailable();
            return isWebAppStarting;
        }

        private void addHeaders(HttpFields.Mutable headers) {
            headers.put(HttpHeader.CACHE_CONTROL, "no-cache, must-revalidate, no-store");
            headers.put("X-XSS-Protection", "1; mode=block");
            headers.put("X-Content-Type-Options", "nosniff");
            headers.put("X-Frame-Options", "SAMEORIGIN");
            headers.put("X-UA-Compatible", "chrome=1");
        }
    }

    static String loadingPage() throws IOException {
        try (InputStream in = Objects.requireNonNull(GoServerLoadingIndicationHandler.class.getResourceAsStream("/loading.page.html"))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
