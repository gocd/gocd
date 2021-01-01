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
package com.thoughtworks.go.agent;

import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.config.DefaultAgentRegistry;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.BasicHttpContext;
import org.springframework.remoting.httpinvoker.AbstractHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;
import org.springframework.remoting.support.RemoteInvocationResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static com.thoughtworks.go.CurrentGoCDVersion.docsUrl;

@Deprecated(forRemoval = true)
// This class should be removed once we remove the RMI buildRepository endpoint because RMI is vulnerable to a remote
// execution attack payload. This will be superceded by RemotingClient.
public class GoHttpClientHttpInvokerRequestExecutor extends AbstractHttpInvokerRequestExecutor {

    private final GoAgentServerHttpClient goAgentServerHttpClient;
    private final DefaultAgentRegistry defaultAgentRegistry;

    public GoHttpClientHttpInvokerRequestExecutor(GoAgentServerHttpClient goAgentServerHttpClient, DefaultAgentRegistry defaultAgentRegistry) {
        this.goAgentServerHttpClient = goAgentServerHttpClient;
        this.defaultAgentRegistry = defaultAgentRegistry;
    }

    @Override
    protected RemoteInvocationResult doExecuteRequest(HttpInvokerClientConfiguration config, ByteArrayOutputStream baos) throws Exception {
        HttpPost postMethod = new HttpPost(config.getServiceUrl());

        ByteArrayEntity entity = new ByteArrayEntity(baos.toByteArray());
        entity.setContentType(getContentType());
        postMethod.setEntity(entity);

        BasicHttpContext context = null;

        postMethod.setHeader("X-Agent-GUID", defaultAgentRegistry.uuid());
        postMethod.setHeader("Authorization", defaultAgentRegistry.token());

        try (CloseableHttpResponse response = goAgentServerHttpClient.execute(postMethod, context)) {
            validateResponse(response);
            try (InputStream responseBody = getResponseBody(response)) {
                return readRemoteInvocationResult(responseBody, config.getCodebaseUrl());
            }
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
        if (status.getStatusCode() >= 400) {
            String messagePrefix = String.format("The server returned status code %d. Possible reasons include:", status.getStatusCode());

            List<String> reasons = Arrays.asList(
                    "This agent has been deleted from the configuration",
                    "This agent is pending approval",
                    "There is possibly a reverse proxy (or load balancer) that has been misconfigured. See "
                            + docsUrl("/installation/configure-reverse-proxy.html#agents-and-reverse-proxies") +
                            " for details."
            );

            String delimiter = "\n   - ";
            throw new ClientProtocolException(messagePrefix + delimiter + String.join(delimiter, reasons));
        }
        if (status.getStatusCode() >= 300) {
            throw new NoHttpResponseException("Did not receive successful HTTP response: status code = " + status.getStatusCode() + ", status message = [" + status.getReasonPhrase() + "]");
        }

    }
}
