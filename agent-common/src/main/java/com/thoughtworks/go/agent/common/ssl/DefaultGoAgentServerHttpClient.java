/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent.common.ssl;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;

public class DefaultGoAgentServerHttpClient implements GoAgentServerHttpClient {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultGoAgentServerHttpClient.class);

    private CloseableHttpClient client;
    private X500Principal principal;
    private GoAgentServerHttpClientBuilder builder;

    public DefaultGoAgentServerHttpClient(GoAgentServerHttpClientBuilder builder) {
        this.builder = builder;
    }

    // called by spring
    @PostConstruct
    public void init() throws Exception {
        this.client = builder.build();
        this.principal = builder.principal();
    }


    @Override
    public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
        return client.execute(request);
    }

    @Override
    public CloseableHttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException {
        return client.execute(request, context);
    }

    private synchronized void close() {
        try {
            if (this.client != null) {
                this.client.close();
            }
        } catch (Exception e) {
            LOG.warn("Could not close http client", e);
        }

        try {
            init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void reset() throws IOException {
        close();
    }

    public X500Principal principal() {
        return this.principal;
    }
}
