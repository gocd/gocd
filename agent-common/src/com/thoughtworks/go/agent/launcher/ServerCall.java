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

package com.thoughtworks.go.agent.launcher;

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClientBuilder;
import com.thoughtworks.go.util.SslVerificationMode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * @understands calling any server url
 */
public class ServerCall {
    private static final Log LOG = LogFactory.getLog(ServerCall.class);
    public static final int HTTP_TIMEOUT_IN_MILLISECONDS = 5000;

    public ServerResponseWrapper invoke(HttpRequestBase request, File rootCertFile, SslVerificationMode sslVerificationMode) throws Exception {
        HashMap<String, String> headers = new HashMap<>();

        HttpClientBuilder httpClientBuilder = new GoAgentServerHttpClientBuilder(rootCertFile, sslVerificationMode).httpClientBuilder(HttpClients.custom());

        request.setConfig(RequestConfig.custom().setConnectTimeout(HTTP_TIMEOUT_IN_MILLISECONDS).build());

        try (
                CloseableHttpClient httpClient = httpClientBuilder.build();
                final CloseableHttpResponse response = httpClient.execute(request);
                StringWriter sw = new StringWriter();
                PrintWriter out = new PrintWriter(sw)
        ) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                out.println("Return Code: " + response.getStatusLine().getStatusCode());
                out.println("Few Possible Causes: ");
                out.println("1. Your Go Server is down or not accessible.");
                out.println("2. This agent might be incompatible with your Go Server.Please fix the version mismatch between Go Server and Go Agent.");
                throw new Exception(sw.toString());
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new Exception("Got status " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine() + " from server");
            }
            for (Header header : response.getAllHeaders()) {
                headers.put(header.getName(), header.getValue());
            }
            try (InputStream content = response.getEntity() != null ? response.getEntity().getContent() : null) {
                return new ServerResponseWrapper(headers, content);
            }
        } catch (Exception e) {
            String message = "Couldn't access Go Server with base url: " + request.getURI() + ": " + e.toString();
            LOG.error(message);
            throw new Exception(message, e);
        }
    }

    public static class ServerResponseWrapper {
        public final Map<String, String> headers;
        public final InputStream body;

        public ServerResponseWrapper(Map<String, String> headers, InputStream body) throws IOException {
            this.headers = Collections.unmodifiableMap(headers);
            this.body = toByteArrayInputStream(body);
        }

        private InputStream toByteArrayInputStream(InputStream body) throws IOException {
            if (body == null) {
                return null;
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                IOUtils.copy(body, baos);
                return new ByteArrayInputStream(baos.toByteArray());
            }
        }
    }
}
