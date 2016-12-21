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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;

import javax.security.auth.x500.X500Principal;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class GoAgentServerHttpClient implements Closeable {
    private static final Log LOG = LogFactory.getLog(GoAgentServerHttpClient.class);

    private HttpClient client;
    private X500Principal principal;
    private GoAgentServerHttpClientBuilder builder;

    public GoAgentServerHttpClient(GoAgentServerHttpClientBuilder builder) {
        this.builder = builder;
    }

    // called by spring
    public void init() throws Exception {
        this.client = builder.build();
        this.principal = builder.principal();
    }

    public ContentResponse execute(Request request) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        return request.send();
    }

    public void execute(Request request, Response.Listener listener) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        request.send(listener);
    }

    @Override
    public synchronized void close() {
        destroy();

        try {
            init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // used only in tests
    public void destroy() {
        try {
            if (this.client != null) {
                this.client.stop();
            }
        } catch (Exception e) {
            LOG.warn("Could not close http client", e);
        }
    }

    public void reset() throws IOException {
        close();
    }

    public X500Principal principal() {
        return this.principal;
    }

    public Request newRequest(String url) {
        return client.newRequest(url);
    }
}
