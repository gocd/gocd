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
package com.thoughtworks.go.agent.service;

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.config.AgentRegistry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.RequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
            final String responseBody = responseBody(response);
            if (response.getStatusLine().getStatusCode() == SC_OK) {
                LOGGER.info("The server has generated token for the agent.");
                return responseBody;
            } else {
                LOGGER.error("Received status code from server {}", response.getStatusLine().getStatusCode());
                LOGGER.error("Reason for failure {} ", responseBody);
                throw new RuntimeException(responseBody);
            }
        } finally {
            getTokenRequest.releaseConnection();
        }
    }

    private String responseBody(CloseableHttpResponse response) throws IOException {
        try (InputStream is = response.getEntity() == null ? new NullInputStream(0) : response.getEntity().getContent()) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }
}
