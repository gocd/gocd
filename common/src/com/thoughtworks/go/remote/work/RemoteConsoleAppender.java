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

package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.util.HttpService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;

import java.io.IOException;

public class RemoteConsoleAppender implements ConsoleAppender {

    private static final Logger LOGGER = Logger.getLogger(RemoteConsoleAppender.class);

    private String consoleUri;
    private HttpService httpService;

    public RemoteConsoleAppender(String consoleUri, HttpService httpService) {
        this.consoleUri = consoleUri;
        this.httpService = httpService;
    }

    public void append(String content) throws IOException {
        HttpPut putMethod = new HttpPut(consoleUri);
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Appending console to URL -> " + consoleUri);
            }

            putMethod.setEntity(new StringEntity(content));
            HttpService.setSizeHeader(putMethod, content.getBytes().length);
            CloseableHttpResponse response = httpService.execute(putMethod);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Got " + response.getStatusLine().getStatusCode());
            }
        } finally {
            putMethod.releaseConnection();
        }
    }
}
