/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.remote.work;

import java.io.IOException;

import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.HttpService;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.log4j.Logger;

public class RemoteConsoleAppender implements ConsoleAppender {

    private static final Logger LOGGER = Logger.getLogger(RemoteConsoleAppender.class);

    private String consoleUri;
    private HttpService httpService;
    private final AgentIdentifier agentIdentifier;

    public RemoteConsoleAppender(String consoleUri, HttpService httpService, AgentIdentifier agentIdentifier) {
        this.consoleUri = consoleUri;
        this.httpService = httpService;
        this.agentIdentifier = agentIdentifier;
    }

    public void append(String content) throws IOException {
        PutMethod putMethod = new PutMethod(consoleUri);
        try {
            HttpClient httpClient = httpService.httpClient();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Appending console to URL -> " + consoleUri);
            }
            putMethod.setRequestEntity(new StringRequestEntity(content));

            HttpClientParams clientParams = new HttpClientParams();
            clientParams.setParameter("agentId", agentIdentifier.getUuid());
            HttpService.setSizeHeader(putMethod, content.getBytes().length);
            putMethod.setParams(clientParams);
            httpClient.executeMethod(putMethod);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Got " + putMethod.getStatusLine());
            }
        } finally {
            putMethod.releaseConnection();
        }
    }
}
