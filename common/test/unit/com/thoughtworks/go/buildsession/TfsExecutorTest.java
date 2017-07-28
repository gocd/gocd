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
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TfsExecutorTest {

    private TfsMaterial tfsMaterial;
    private BuildCommand buildCommand;
    private BuildSession buildSession;
    private File workingDir;

    @Before
    public void setUp() throws Exception {
        workingDir = mock(File.class);
        tfsMaterial = mock(TfsMaterial.class);

        createBuildCommand();
        createBuildSession();
    }

    @Test
    public void shouldUpdateTheTfsMaterial() throws Exception {
        TfsExecutor tfsExecutor = new TfsExecutor() {
            @Override
            protected TfsMaterial createMaterial(String url, String username, String password, String domain, String projectPath) {
                assertThat(url, is("some url"));
                assertThat(username, is("username"));
                assertThat(password, is("password"));
                assertThat(domain, is("domain"));
                assertThat(projectPath, is("project path"));

                return tfsMaterial;
            }
        };

        boolean result = tfsExecutor.execute(buildCommand, buildSession);

        ArgumentCaptor<RevisionContext> revisionCaptor = ArgumentCaptor.forClass(RevisionContext.class);
        ArgumentCaptor<File> workingDirCaptor = ArgumentCaptor.forClass(File.class);
        verify(tfsMaterial).updateTo(any(ConsoleOutputStreamConsumer.class), workingDirCaptor.capture(), revisionCaptor.capture(),
                any(SubprocessExecutionContext.class));

        assertThat(revisionCaptor.getValue().getLatestRevision().getRevision(), is("revision1"));
        assertThat(workingDirCaptor.getValue(), is(workingDir));
        assertThat(result, is(true));
    }

    private void createBuildSession() {
        AgentIdentifier agentIdentifier = mock(AgentIdentifier.class);

        when(workingDir.getAbsolutePath()).thenReturn("working dir");

        ProcessOutputStreamConsumer processOutputStreamConsumer = mock(ProcessOutputStreamConsumer.class);

        buildSession = mock(BuildSession.class);
        when(buildSession.resolveRelativeDir("working dir")).thenReturn(workingDir);
        when(buildSession.getAgentIdentifier()).thenReturn(agentIdentifier);
        when(buildSession.processOutputStreamConsumer()).thenReturn(processOutputStreamConsumer);
    }

    private void createBuildCommand() {
        buildCommand = mock(BuildCommand.class);
        when(buildCommand.getStringArg("url")).thenReturn("some url");
        when(buildCommand.getStringArg("username")).thenReturn("username");
        when(buildCommand.getStringArg("password")).thenReturn("password");
        when(buildCommand.getStringArg("domain")).thenReturn("domain");
        when(buildCommand.getStringArg("projectPath")).thenReturn("project path");
        when(buildCommand.getStringArg("revision")).thenReturn("revision1");
        when(buildCommand.getWorkingDirectory()).thenReturn("working dir");
    }
}