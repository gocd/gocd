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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.BasicHttpContext;
import org.springframework.remoting.httpinvoker.AbstractHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;
import org.springframework.remoting.support.RemoteInvocationResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class GoHttpClientHttpInvokerRequestExecutor extends AbstractHttpInvokerRequestExecutor {
    private final GoAgentServerHttpClient goAgentServerHttpClient;
    private final SystemEnvironment environment;

    public GoHttpClientHttpInvokerRequestExecutor(GoAgentServerHttpClient goAgentServerHttpClient, SystemEnvironment environment) {
        this.goAgentServerHttpClient = goAgentServerHttpClient;
        this.environment = environment;
    }

    @Override
    protected RemoteInvocationResult doExecuteRequest(HttpInvokerClientConfiguration config, ByteArrayOutputStream baos) throws Exception {
        HttpPost postMethod = new HttpPost(config.getServiceUrl());

        ByteArrayEntity entity = new ByteArrayEntity(baos.toByteArray());
        entity.setContentType(getContentType());
        postMethod.setEntity(entity);

        BasicHttpContext context = null;

        if (environment.useSslContext()) {
            context = new BasicHttpContext();
            context.setAttribute(HttpClientContext.USER_TOKEN, goAgentServerHttpClient.principal());
        }

        try (CloseableHttpResponse response = goAgentServerHttpClient.execute(postMethod, context)) {
            validateResponse(response);
            InputStream responseBody = getResponseBody(response);
            return readRemoteInvocationResult(responseBody, config.getCodebaseUrl());
        }
    }

    private InputStream getResponseBody(HttpResponse httpResponse) throws IOException {
        if (isGzipResponse(httpResponse)) {
            return new GZIPInputStream(httpResponse.getEntity().getContent());
        } else {
            return httpResponse.getEntity().getContent();
        }
    }

    private boolean isGzipResponse(HttpResponse httpResponse) {
        Header encodingHeader = httpResponse.getFirstHeader(HTTP_HEADER_CONTENT_ENCODING);
        return (encodingHeader != null && encodingHeader.getValue() != null && encodingHeader.getValue().toLowerCase().contains(ENCODING_GZIP));
    }

    private void validateResponse(HttpResponse response) throws IOException {
        StatusLine status = response.getStatusLine();
        if (status.getStatusCode() >= 300) {
            throw new NoHttpResponseException("Did not receive successful HTTP response: status code = " + status.getStatusCode() + ", status message = [" + status.getReasonPhrase() + "]");
        }

    }
}
