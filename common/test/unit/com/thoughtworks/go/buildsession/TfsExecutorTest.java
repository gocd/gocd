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

package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TfsExecutorTest {

    TfsExecutor tfsExecutor;
    TfsMaterial tfsMaterial;
    ConsoleOutputStreamConsumer consoleOutputStreamConsumer;
    SubprocessExecutionContext subprocessExecutionContext;
    BuildCommand buildCommand;
    BuildSession buildSession;
    File workingDir;
    RevisionContext revisionContext;

    @Before
    public void setUp() throws Exception {
        createBuildCommand();
        createBuildSession();
        tfsMaterial = mock(TfsMaterial.class);
        consoleOutputStreamConsumer = mock(ConsoleOutputStreamConsumer.class);
        subprocessExecutionContext = mock(SubprocessExecutionContext.class);
        revisionContext = new RevisionContext(new StringRevision("revision"));

        tfsExecutor = new TfsExecutor(tfsMaterial);
    }

    public void createBuildSession() {
        AgentIdentifier agentIdentifier = mock(AgentIdentifier.class);

        workingDir = mock(File.class);
        when(workingDir.getAbsolutePath()).thenReturn("working dir");

        ProcessOutputStreamConsumer processOutputStreamConsumer = mock(ProcessOutputStreamConsumer.class);

        buildSession = mock(BuildSession.class);
        when(buildSession.resolveRelativeDir("working dir")).thenReturn(workingDir);
        when(buildSession.getAgentIdentifier()).thenReturn(agentIdentifier);
        when(buildSession.processOutputStreamConsumer()).thenReturn(processOutputStreamConsumer);
    }

    public void createBuildCommand() {
        buildCommand = mock(BuildCommand.class);
        when(buildCommand.getStringArg("url")).thenReturn("some url");
        when(buildCommand.getStringArg("username")).thenReturn("username");
        when(buildCommand.getStringArg("password")).thenReturn("password");
        when(buildCommand.getStringArg("domain")).thenReturn("domain");
        when(buildCommand.getStringArg("projectPath")).thenReturn("project path");
        when(buildCommand.getStringArg("revision")).thenReturn("revision");
        when(buildCommand.getWorkingDirectory()).thenReturn("working dir");
    }

    @Test
    public void shouldUpdateTheTfsMaterial() throws Exception {
        boolean result = tfsExecutor.execute(buildCommand, buildSession);

        verify(tfsMaterial).updateTo(any(ConsoleOutputStreamConsumer.class), any(File.class), any(RevisionContext.class),
                any(SubprocessExecutionContext.class));
        assertThat(result, is(true));
    }
}