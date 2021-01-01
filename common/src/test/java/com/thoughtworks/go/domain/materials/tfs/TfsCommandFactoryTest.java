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
package com.thoughtworks.go.domain.materials.tfs;

import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.util.command.UrlArgument;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TfsCommandFactoryTest {

    private SubprocessExecutionContext executionContext;
    private TfsCommandFactory tfsCommandFactory;
    private String materialFingerprint = "fingerprint";
    private String computedWorkspaceName = "boo-yaa-goo-moo-foo";
    private String DOMAIN = "domain";
    private String USERNAME = "userName";
    private String PROJECT_PATH = "$/project";
    private final String PASSWORD = "password";
    private final UrlArgument URL = new UrlArgument("url");

    @Before
    public void setup() {
        executionContext = mock(SubprocessExecutionContext.class);
        when(executionContext.getProcessNamespace(materialFingerprint)).thenReturn(computedWorkspaceName);
        tfsCommandFactory = new TfsCommandFactory();
    }

    @Test
    public void shouldReturnSdkCommand() throws Exception {
        TfsCommand expectedTfsCommand = mock(TfsCommand.class);
        TfsCommandFactory spyCommandFactory = spy(tfsCommandFactory);
        TfsSDKCommandBuilder commandBuilder = mock(TfsSDKCommandBuilder.class);
        doReturn(commandBuilder).when(spyCommandFactory).getSDKBuilder();
        when(commandBuilder.buildTFSSDKCommand("fingerprint", URL, DOMAIN, USERNAME, PASSWORD, computedWorkspaceName, PROJECT_PATH)).thenReturn(expectedTfsCommand);
        TfsCommand actualTfsCommand = spyCommandFactory.create(executionContext, URL, DOMAIN, USERNAME, PASSWORD, "fingerprint", PROJECT_PATH);
        assertThat(actualTfsCommand, is(expectedTfsCommand));
        verify(commandBuilder).buildTFSSDKCommand("fingerprint", URL, DOMAIN, USERNAME, PASSWORD, computedWorkspaceName, PROJECT_PATH);
    }

    @Test
    public void shouldReturnExecutionContextsProcessNamespace() {
        String fingerprint = "material-fingerprint";
        when(executionContext.getProcessNamespace(fingerprint)).thenReturn("workspace-name");
        assertThat(executionContext.getProcessNamespace(fingerprint), is("workspace-name"));
        verify(executionContext, times(1)).getProcessNamespace(fingerprint);
    }

    @Test
    public void shouldPassFingerPrintAlongWithExecutionContextWhenCreatingTfsCommand() {
        String fingerprint = "fingerprint";
        when(executionContext.getProcessNamespace(materialFingerprint)).thenReturn(fingerprint);
        TfsCommandFactory spy = spy(tfsCommandFactory);
        TfsSDKCommandBuilder commandBuilder = mock(TfsSDKCommandBuilder.class);
        doReturn(commandBuilder).when(spy).getSDKBuilder();
        TfsCommand tfsCommand = mock(TfsCommand.class);
        when(commandBuilder.buildTFSSDKCommand(materialFingerprint, URL, DOMAIN, USERNAME, PASSWORD, fingerprint, PROJECT_PATH)).thenReturn(tfsCommand);
        spy.create(executionContext, URL, DOMAIN, USERNAME, PASSWORD, materialFingerprint, PROJECT_PATH);
        verify(executionContext).getProcessNamespace(materialFingerprint);
        verify(commandBuilder).buildTFSSDKCommand(materialFingerprint, URL, DOMAIN, USERNAME, PASSWORD, fingerprint, PROJECT_PATH);
    }


}
