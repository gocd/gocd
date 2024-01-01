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

import com.sun.net.httpserver.HttpExchange;
import com.thoughtworks.go.util.Pair;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@FunctionalInterface
interface HttpHandler extends com.sun.net.httpserver.HttpHandler {

    Pair<Integer, String> response();

    @Override
    default void handle(HttpExchange exchange) throws IOException {
        writeResponse(exchange, isAllowed(exchange.getRequestMethod())
            ? response()
            : Pair.pair(HttpStatus.SC_METHOD_NOT_ALLOWED, "This method is not allowed. Please use GET or HEAD.")
        );
    }

    private static boolean isAllowed(String requestMethod) {
        return Set.of("GET", "PUT").contains(requestMethod.toUpperCase());
    }

    private void writeResponse(HttpExchange exchange, Pair<Integer, String> response) throws IOException {
        byte[] rawResponse = response.last().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(response.first(), rawResponse.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(rawResponse);
        }
    }

}
