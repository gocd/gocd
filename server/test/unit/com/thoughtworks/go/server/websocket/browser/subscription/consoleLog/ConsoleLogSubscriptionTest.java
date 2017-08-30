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

package com.thoughtworks.go.server.websocket.browser.subscription.consoleLog;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.RestfulService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.websocket.browser.GoWebSocket;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConsoleLogSubscriptionTest {
    private ConsoleLogSubscription subscription;
    private ConsoleLog message;

    @Mock
    private ConsoleLogSender consoleLogSender;
    @Mock
    private SecurityService securitySerice;
    @Mock
    private RestfulService restfulService;
    @Mock
    private GoWebSocket socket;
    private JobIdentifier jobIdentifier;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        subscription = new ConsoleLogSubscription(consoleLogSender, securitySerice, restfulService);
        jobIdentifier = new JobIdentifier("foo", -1, "42", "test", "1", "unit");
        jobIdentifier.setBuildId(null);
        jobIdentifier.setPipelineCounter(null);
        message = new ConsoleLog(jobIdentifier, 0);
    }

    @Test
    public void shouldStartProcesssingConsoleLogsOverProvidedWebsocket() throws Exception {
        when(restfulService.findJob(jobIdentifier)).thenReturn(jobIdentifier);

        subscription.start(message, socket);

        verify(consoleLogSender, times(1)).process(socket, jobIdentifier, 0L);
        verify(restfulService, times(1)).findJob(jobIdentifier);
    }

    @Test
    public void shouldCheckForPipelineViewPermission() throws Exception {
        Username user = new Username("Bob");
        String pipelineName = jobIdentifier.getPipelineName();
        when(socket.getCurrentUser()).thenReturn(user);

        subscription.isAuthorized(message, socket);

        verify(securitySerice, times(1)).hasViewPermissionForPipeline(user, pipelineName);
    }
}