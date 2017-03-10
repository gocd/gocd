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

import com.thoughtworks.go.domain.ConsoleOut;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.server.service.JobDetailService;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

public class ClientRemoteHandlerTest {
    private ClientRemoteHandler clientRemoteHandler;
    private ConsoleService consoleService;
    private ConsoleLogEndpoint clientRemoteSocket;
    private JobIdentifier jobIdentifier;
    private ConsoleOut consoleOut;
    private JobInstance jobInstance;

    @Before
    public void setUp() throws Exception {
        consoleService = mock(ConsoleService.class);
        JobDetailService jobDetailService = mock(JobDetailService.class);
        clientRemoteSocket = mock(ConsoleLogEndpoint.class);
        clientRemoteHandler = new ClientRemoteHandler(consoleService, jobDetailService);
        consoleOut = mock(ConsoleOut.class);
        jobIdentifier = mock(JobIdentifier.class);
        jobInstance = mock(JobInstance.class);
        when(jobDetailService.findMostRecentBuild(jobIdentifier)).thenReturn(jobInstance);
        when(consoleService.getConsoleOut(eq(jobIdentifier), anyInt())).thenReturn(consoleOut);
    }

    @Test
    public void shouldSendConsoleLog() throws Exception {
        when(jobInstance.isCompleted()).thenReturn(true);
        when(consoleOut.output()).thenReturn("Expected output for this test");

        clientRemoteHandler.process(clientRemoteSocket, jobIdentifier);

        verify(clientRemoteSocket).send("Expected output for this test");
    }

    @Test
    public void shouldSendConsoleLogInMultipleMessagesIfBuildInProgress() throws Exception {
        when(jobInstance.isCompleted()).thenReturn(false).thenReturn(true);
        when(consoleOut.output()).thenReturn("First Output").thenReturn("Second Output");

        when(consoleOut.calculateNextStart()).thenReturn(1);

        clientRemoteHandler.process(clientRemoteSocket, jobIdentifier);

        verify(consoleService, times(1)).getConsoleOut(jobIdentifier, 0);
        verify(consoleService, times(1)).getConsoleOut(jobIdentifier, 1);
        verify(clientRemoteSocket, times(1)).send("First Output");
        verify(clientRemoteSocket, times(1)).send("Second Output");
    }

    @Test
    public void shouldNotSendMessagesWhenOutputHasNotAdvanced() throws Exception {
        when(jobInstance.isCompleted()).thenReturn(false).thenReturn(true);
        when(consoleOut.calculateNextStart()).thenReturn(0).thenReturn(0);

        clientRemoteHandler.process(clientRemoteSocket, jobIdentifier);

        verify(clientRemoteSocket, times(1)).send(anyString());
    }

    @Test
    public void shouldCloseSocketAfterProcessingMessage() throws Exception {
        when(jobInstance.isCompleted()).thenReturn(true);
        clientRemoteHandler.process(clientRemoteSocket, jobIdentifier);

        verify(clientRemoteSocket).close();
    }
}