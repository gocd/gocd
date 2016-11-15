/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;

import javax.security.auth.x500.X500Principal;
import java.io.Closeable;
import java.io.IOException;

public class GoAgentServerHttpClient implements Closeable {
    private static final Log LOG = LogFactory.getLog(GoAgentServerHttpClient.class);

    private final SystemEnvironment systemEnvironment;
    private CloseableHttpClient client;
    private X500Principal principal;

    public GoAgentServerHttpClient(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    // called by spring
    public void init() throws Exception {
        GoAgentServerHttpClientBuilder builder = new GoAgentServerHttpClientBuilder(systemEnvironment);
        this.client = builder.httpClient();
        this.principal = builder.principal();
    }


    public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
        return client.execute(request);
    }

    public CloseableHttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException {
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

    public void reset() throws IOException {
        close();
    }

    public X500Principal principal() {
        return this.principal;
    }
}
