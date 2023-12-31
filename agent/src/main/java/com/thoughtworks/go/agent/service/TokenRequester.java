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
package com.thoughtworks.go.agent.service;

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.config.AgentRegistry;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;

public class TokenRequester {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenRequester.class);
    private final String tokenURL;
    private final AgentRegistry agentRegistry;
    private final GoAgentServerHttpClient httpClient;

    public TokenRequester(String tokenURL, AgentRegistry agentRegistry, GoAgentServerHttpClient httpClient) {
        this.tokenURL = tokenURL;
        this.agentRegistry = agentRegistry;
        this.httpClient = httpClient;
    }

    public String getToken() throws IOException {
        LOGGER.debug("[Agent Registration] Using URL {} to get a token.", tokenURL);

        HttpRequestBase getTokenRequest = (HttpRequestBase) RequestBuilder.get(tokenURL)
                .addParameter("uuid", agentRegistry.uuid())
                .build();

        try (CloseableHttpResponse response = httpClient.execute(getTokenRequest)) {
            if (response.getStatusLine().getStatusCode() == SC_OK) {
                LOGGER.info("The server has generated token for the agent.");
                return responseBody(response);
            } else {
                LOGGER.error("[Agent Registration] Got status {} from GoCD", response.getStatusLine());

                String error = Optional.ofNullable(ContentType.get(response.getEntity()))
                    .filter(ct -> ContentType.TEXT_HTML.getMimeType().equals(ct.getMimeType()))
                    .map(ignore -> "<non-machine HTML response>")
                    .orElseGet(() -> responseBody(response));
                throw new RuntimeException(String.format("Agent registration could not acquire token due to %s: %s", response.getStatusLine(), error));
            }
        } finally {
            getTokenRequest.releaseConnection();
        }
    }

    private String responseBody(CloseableHttpResponse response) {
        try (InputStream is = response.getEntity() == null ? InputStream.nullInputStream() : response.getEntity().getContent()) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
