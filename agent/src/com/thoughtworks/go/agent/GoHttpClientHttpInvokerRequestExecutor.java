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
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;
import org.springframework.remoting.httpinvoker.AbstractHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;
import org.springframework.remoting.support.RemoteInvocationResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class GoHttpClientHttpInvokerRequestExecutor extends AbstractHttpInvokerRequestExecutor {
    private final GoAgentServerHttpClient goAgentServerHttpClient;

    public GoHttpClientHttpInvokerRequestExecutor(GoAgentServerHttpClient goAgentServerHttpClient) {
        this.goAgentServerHttpClient = goAgentServerHttpClient;
    }

    @Override
    protected RemoteInvocationResult doExecuteRequest(HttpInvokerClientConfiguration config, ByteArrayOutputStream baos) throws Exception {
        Request postMethod = goAgentServerHttpClient.newRequest(config.getServiceUrl()).method(HttpMethod.POST);
        postMethod.content(new BytesContentProvider(getContentType(), baos.toByteArray()));

        InputStreamResponseListener listener = new InputStreamResponseListener();
        goAgentServerHttpClient.execute(postMethod, listener);
        Response response = listener.get(15, TimeUnit.SECONDS);
        validateResponse(response);
        try(InputStream responseBody = listener.getInputStream()) {
            return readRemoteInvocationResult(responseBody, config.getCodebaseUrl());
        }
    }

    private void validateResponse(Response response) throws IOException {
        if (response.getStatus() >= 300) {
            throw new IOException("Did not receive successful HTTP response: status code = " + response.getStatus() + ", status message = [" + response.getReason() + "]");
        }

    }
}
