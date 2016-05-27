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

package com.thoughtworks.go.server.service.support.garage;

import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.CommandLineException;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class GarageServiceTest {

    private SystemEnvironment systemEnvironment;
    private GarageService garageService;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        garageService = new GarageService(systemEnvironment);
    }

    @Test
    public void shouldReturnData() throws Exception {
        File configDir = mock(File.class);
        when(systemEnvironment.getConfigRepoDir()).thenReturn(configDir);
        GarageService spy = spy(garageService);
        String size = "42MB";
        doReturn(size).when(spy).getDirectorySize(configDir);

        GarageData data = spy.getData();
        assertThat(data.getConfigRepositorySize(), is(size));

        verify(systemEnvironment).getConfigRepoDir();
        verify(spy).getDirectorySize(configDir);
    }

    @Test
    public void shouldFailWhenGitIsNotFoundInPath() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        CommandLine commandLine = mock(CommandLine.class);
        GarageService spy = spy(garageService);
        doReturn(commandLine).when(spy).getGit();
        doReturn(commandLine).when(commandLine).withArg(anyString());
        doReturn(commandLine).when(commandLine).withWorkingDir(any(File.class));
        when(commandLine.run(any(InMemoryStreamConsumer.class), eq(GarageService.PROCESS_TAG))).thenThrow(new CommandLineException("failed to execute git"));

        spy.gc(result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.hasMessage(), is(true));

        verify(spy, times(1)).getGit();
    }

    @Test
    public void shouldFailWhenGitExecutionFailsWithANonZeroReturnCode() throws Exception {
        CommandLine firstCommand = mock(CommandLine.class);
        doReturn(firstCommand).when(firstCommand).withArg(anyString());
        doReturn(firstCommand).when(firstCommand).withWorkingDir(any(File.class));
        when(firstCommand.run(any(InMemoryStreamConsumer.class), eq(GarageService.PROCESS_TAG))).thenReturn(0);
        CommandLine secondCommand = mock(CommandLine.class);
        doReturn(secondCommand).when(secondCommand).withArg(anyString());
        doReturn(secondCommand).when(secondCommand).withWorkingDir(any(File.class));
        when(secondCommand.run(any(InMemoryStreamConsumer.class), eq(GarageService.PROCESS_TAG))).thenReturn(129);
        final List<CommandLine> commands = new LinkedList<CommandLine>();
        commands.add(firstCommand);
        commands.add(secondCommand);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        GarageService spy = spy(garageService);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                CommandLine remove = commands.get(0);
                commands.remove(remove);
                return remove;
            }
        }).when(spy).getGit();

        spy.gc(result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.hasMessage(), is(true));

        verify(spy, times(2)).getGit();
        verify(firstCommand).run(any(InMemoryStreamConsumer.class), eq(GarageService.PROCESS_TAG));
        verify(secondCommand).run(any(InMemoryStreamConsumer.class), eq(GarageService.PROCESS_TAG));
    }

    @Test
    public void shouldSucceedWhenGitGCIsCompleted() throws Exception {
        CommandLine firstCommand = mock(CommandLine.class);
        doReturn(firstCommand).when(firstCommand).withArg(anyString());
        doReturn(firstCommand).when(firstCommand).withWorkingDir(any(File.class));
        when(firstCommand.run(any(InMemoryStreamConsumer.class), eq(GarageService.PROCESS_TAG))).thenReturn(0);
        CommandLine secondCommand = mock(CommandLine.class);
        doReturn(secondCommand).when(secondCommand).withArg(anyString());
        doReturn(secondCommand).when(secondCommand).withWorkingDir(any(File.class));
        when(secondCommand.run(any(InMemoryStreamConsumer.class), eq(GarageService.PROCESS_TAG))).thenReturn(0);
        final List<CommandLine> commands = new LinkedList<CommandLine>();
        commands.add(firstCommand);
        commands.add(secondCommand);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        GarageService spy = spy(garageService);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                CommandLine remove = commands.get(0);
                commands.remove(remove);
                return remove;
            }
        }).when(spy).getGit();

        spy.gc(result);

        assertThat(result.isSuccessful(), is(true));
        assertThat(result.hasMessage(), is(true));

        verify(spy, times(2)).getGit();
        verify(firstCommand).run(any(InMemoryStreamConsumer.class), eq(GarageService.PROCESS_TAG));
        verify(secondCommand).run(any(InMemoryStreamConsumer.class), eq(GarageService.PROCESS_TAG));
    }

    @Test
    public void shouldCreateGitCommand() throws Exception {
        CommandLine git = garageService.getGit();
        assertThat(git.getExecutable(), is("git"));
    }
}
