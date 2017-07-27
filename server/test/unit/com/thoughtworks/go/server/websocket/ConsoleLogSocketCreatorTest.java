/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.websocket;

import com.thoughtworks.go.server.service.RestfulService;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class ConsoleLogSocketCreatorTest {
    private RestfulService restfulService;
    private ServletUpgradeRequest request;
    private ConsoleLogSocketCreator creator;

    @Before
    public void setUp() throws Exception {
        restfulService = mock(RestfulService.class);

        request = mock(ServletUpgradeRequest.class);
        creator = new ConsoleLogSocketCreator(mock(ConsoleLogSender.class), restfulService, new SocketHealthService());
    }

    @Test
    public void createWebSocketParsesJobIdentifierFromURI() throws Exception {
        when(request.getRequestPath()).thenReturn("/console-websocket/pipe/pipeLabel/stage/stageCount/job");
        creator.createWebSocket(request, mock(ServletUpgradeResponse.class));

        verify(restfulService).findJob("pipe", "pipeLabel", "stage", "stageCount", "job");
    }

}