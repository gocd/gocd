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

package com.thoughtworks.go.work;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.publishers.GoArtifactsManipulator;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.work.ConsoleAppender;
import com.thoughtworks.go.remote.work.ConsoleOutputTransmitter;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DefaultGoPublisherTest {
    private static final String PROPERTY_NAME = "PROPERTY_NAME";
    private static final String PROPERTY_VALUE = "property value";
    private static final String NEW_VALUE = "new value";

    @Mock
    ConsoleAppender consoleAppender;
    @Mock
    AgentIdentifier agentIdentifier;
    @Mock
    JobIdentifier jobIdentifier;
    @Mock
    GoArtifactsManipulator goArtifactsManipulator;
    @Mock
    BuildRepositoryRemote buildRepositoryRemote;
    @Mock
    AgentRuntimeInfo agentRuntimeInfo;

    ConsoleOutputTransmitter consoleOutputTransmitter;
    DefaultGoPublisher defaultGoPublisher;

    @Before
    public void setUp() {
        initMocks(this);

        new SystemEnvironment().setProperty(SystemEnvironment.INTERVAL, "60"); // so the thread does not wake up

        consoleOutputTransmitter = new ConsoleOutputTransmitter(consoleAppender);

        when(agentRuntimeInfo.getIdentifier()).thenReturn(agentIdentifier);
        when(goArtifactsManipulator.createConsoleOutputTransmitter(jobIdentifier, agentIdentifier)).thenReturn(consoleOutputTransmitter);

        defaultGoPublisher = new DefaultGoPublisher(goArtifactsManipulator, jobIdentifier, buildRepositoryRemote, agentRuntimeInfo);
    }

    @After
    public void tearDown() {
        consoleOutputTransmitter.stop();
    }

    @Test
    public void shouldReportWhenAVariableIsSet() throws Exception {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        context.setProperty(PROPERTY_NAME, PROPERTY_VALUE, false);

        defaultGoPublisher.reportEnvironmentVariables(context);
        consoleOutputTransmitter.flushToServer();

        String line1 = "[go] setting environment variable 'PROPERTY_NAME' to value 'property value'\n";
        verifyConsoleOutput(line1);
    }

    @Test
    public void shouldReportSecureVariableAsMaskedValue() throws Exception {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        context.setProperty(PROPERTY_NAME, PROPERTY_VALUE, true);

        defaultGoPublisher.reportEnvironmentVariables(context);
        consoleOutputTransmitter.flushToServer();

        String line1 = String.format("[go] setting environment variable 'PROPERTY_NAME' to value '%s'\n", EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE);
        verifyConsoleOutput(line1);
    }

    @Test
    public void shouldReportWhenAVariableIsOverridden() throws Exception {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        context.setProperty(PROPERTY_NAME, PROPERTY_VALUE, false);
        context.setProperty(PROPERTY_NAME, NEW_VALUE, false);

        defaultGoPublisher.reportEnvironmentVariables(context);
        consoleOutputTransmitter.flushToServer();

        String line1 = "[go] setting environment variable 'PROPERTY_NAME' to value 'property value'\n";
        String line2 = "[go] overriding environment variable 'PROPERTY_NAME' with value 'new value'\n";
        verifyConsoleOutput(line1, line2);
    }

    @Test
    public void shouldMaskOverRiddenSecureVariable() throws Exception {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        context.setProperty(PROPERTY_NAME, PROPERTY_VALUE, true);
        context.setProperty(PROPERTY_NAME, NEW_VALUE, true);

        defaultGoPublisher.reportEnvironmentVariables(context);
        consoleOutputTransmitter.flushToServer();

        String line1 = String.format("[go] setting environment variable 'PROPERTY_NAME' to value '%s'\n", EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE);
        String line2 = String.format("[go] overriding environment variable 'PROPERTY_NAME' with value '%s'\n", EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE);
        verifyConsoleOutput(line1, line2);
    }

    private void verifyConsoleOutput(String... lines) throws IOException {
        verify(consoleAppender).append(join(lines));
    }

    private String join(String... lines) {
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            result.append(line);
        }
        return result.toString();
    }
}
