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
package com.thoughtworks.go.agent.common.ssl;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public class GoAgentServerHttpClient implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(GoAgentServerHttpClient.class);

    private CloseableHttpClient client;
    private GoAgentServerHttpClientBuilder builder;

    public GoAgentServerHttpClient(GoAgentServerHttpClientBuilder builder) {
        this.builder = builder;
    }

    // called by spring
    public void init() throws Exception {
        this.client = builder.build();
    }
    
    public CloseableHttpResponse execute(HttpRequestBase request) throws IOException {
        request.setURI(request.getURI().normalize());
        return client.execute(request);
    }

    public CloseableHttpResponse execute(HttpRequestBase request, HttpContext context) throws IOException {
        request.setURI(request.getURI().normalize());
        return client.execute(request, context);
    }

    @Override
    public synchronized void close() {
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

    public void reset() {
        close();
    }
}
