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
package com.thoughtworks.go.agent.statusapi;

import com.sun.net.httpserver.HttpExchange;
import org.apache.http.HttpHeaders;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

@FunctionalInterface
interface HttpHandler extends com.sun.net.httpserver.HttpHandler {

    Response response();

    record Response(int statusCode, String body) {}

    @Override
    default void handle(HttpExchange exchange) throws IOException {
        writeResponse(exchange, isAllowed(exchange.getRequestMethod().toUpperCase())
            ? response()
            : new Response(HttpURLConnection.HTTP_BAD_METHOD, "This method is not allowed. Please use GET or HEAD.")
        );
    }

    private static boolean isAllowed(String requestMethod) {
        return "GET".equals(requestMethod) || "HEAD".equals(requestMethod);
    }

    private void writeResponse(HttpExchange exchange, Response response) throws IOException {
        byte[] rawResponse = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(response.statusCode(), rawResponse.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(rawResponse);
        }
    }

}
