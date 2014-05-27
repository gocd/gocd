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

package com.thoughtworks.go.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProcessManagerTest {

    private ProcessManager processManager;
    private Process processOne;
    private Process processTwo;
    private ProcessWrapper wrapperForProcessOne;
    private ProcessWrapper wrapperForProcessTwo;
    private Process processStartedByManager;
    private ProcessBuilder lastProcessBuilder;

    @Before
    public void setUp() {
        processManager = new ProcessManager() {
            @Override
            Process startProcess(ProcessBuilder processBuilder, String msgCommandInfo) {
                lastProcessBuilder = processBuilder;
                return processStartedByManager;
            }
        };
        processStartedByManager = mock(Process.class);
        when(processStartedByManager.getInputStream()).thenReturn(mock(InputStream.class));
        when(processStartedByManager.getErrorStream()).thenReturn(mock(InputStream.class));
        when(processStartedByManager.getOutputStream()).thenReturn(mock(OutputStream.class));
        processOne = mock(Process.class);
        processTwo = mock(Process.class);
        when(processOne.getInputStream()).thenReturn(mock(InputStream.class));
        when(processOne.getErrorStream()).thenReturn(mock(InputStream.class));
        when(processOne.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(processOne.exitValue()).thenThrow(new IllegalStateException());
        when(processTwo.exitValue()).thenThrow(new IllegalStateException());
        when(processTwo.getInputStream()).thenReturn(mock(InputStream.class));
        when(processTwo.getErrorStream()).thenReturn(mock(InputStream.class));
        when(processTwo.getOutputStream()).thenReturn(mock(OutputStream.class));
        ConcurrentHashMap<Process, ProcessWrapper> processMap = processManager.getProcessMap();
        wrapperForProcessOne = new ProcessWrapper(processOne, "tag1", null, inMemoryConsumer(), null, "ERROR: ");
        processMap.put(processOne, wrapperForProcessOne);
        wrapperForProcessTwo = new ProcessWrapper(processTwo, "tag2", null, inMemoryConsumer(), null, "ERROR: ");
        processMap.put(processTwo, wrapperForProcessTwo);
    }

    private HashMap<String, String> createEnvMap(String... pairs) {
        HashMap<String, String> map = new HashMap<String, String>();

        for (int i = 0; i < pairs.length - 1; i+= 2) {
            map.put(pairs[i], pairs[i + 1]);
        }

        return map;
    }

    private EnvironmentVariableContext createEnvironmentContext(String... pairs) {
        EnvironmentVariableContext context = new EnvironmentVariableContext();

        for (int i = 0; i < pairs.length - 1; i+= 2) {
            context.setProperty(pairs[i], pairs[i + 1], false);
        }

        return context;
    }

    @Test
    public void shouldSupportNoVariables() {
        InMemoryStreamConsumer consumer = inMemoryConsumer();
        ProcessWrapper wrapper = processManager.createProcess(new String[]{"echo", "message"}, "echo 'message'", null, new HashMap<String, String>(), new EnvironmentVariableContext(), consumer, "test-tag", null, "ERROR: ");

        String commandLine = ArrayUtil.join(lastProcessBuilder.command().toArray(), " ");
        assertEquals(commandLine, "echo message");
    }

    @Test
    public void shouldSupportVariableFromCommand() {
        InMemoryStreamConsumer consumer = inMemoryConsumer();
        HashMap<String, String> env = createEnvMap("Message", "message");
        ProcessWrapper wrapper = processManager.createProcess(new String[]{"echo", "${Env.Message}"}, "echo 'message'", null, env, new EnvironmentVariableContext(), consumer, "test-tag", null, "ERROR: ");

        String commandLine = ArrayUtil.join(lastProcessBuilder.command().toArray(), " ");
        assertEquals(commandLine, "echo message");
    }

    @Test
    public void shouldSupportVariableFromEnvironmentVariableContext() {
        InMemoryStreamConsumer consumer = inMemoryConsumer();
        EnvironmentVariableContext environmentVariableContext = createEnvironmentContext("Message", "message");
        ProcessWrapper wrapper = processManager.createProcess(new String[]{"echo", "${Env.Message}"}, "echo 'message'", null, new HashMap<String, String>(), environmentVariableContext, consumer, "test-tag", null, "ERROR: ");
        wrapper.waitForExit();

        String commandLine = ArrayUtil.join(lastProcessBuilder.command().toArray(), " ");
        assertEquals(commandLine, "echo message");
    }

    @Test
    public void shouldSupportVariableFromCommandAndEnvironmentVariableContext() {
        InMemoryStreamConsumer consumer = inMemoryConsumer();
        HashMap<String, String> env = createEnvMap("Message1", "Hello");
        EnvironmentVariableContext environmentVariableContext = createEnvironmentContext("Message2", "World");
        ProcessWrapper wrapper = processManager.createProcess(new String[]{"echo", "${Env.Message1}, ${Env.Message2}!"}, "echo 'message'", null, env, environmentVariableContext, consumer, "test-tag", null, "ERROR: ");

        String commandLine = ArrayUtil.join(lastProcessBuilder.command().toArray(), " ");
        assertEquals(commandLine, "echo Hello, World!");
    }

    @Test
    public void shouldSupportVariableFromCommandOverridesEnvironmentVariableContext() {
        InMemoryStreamConsumer consumer = inMemoryConsumer();
        HashMap<String, String> env = createEnvMap("Message", "Found");
        EnvironmentVariableContext environmentVariableContext = createEnvironmentContext("Message", "Lost");
        ProcessWrapper wrapper = processManager.createProcess(new String[]{"echo", "${Env.Message}"}, "echo 'message'", null, env, environmentVariableContext, consumer, "test-tag", null, "ERROR: ");

        String commandLine = ArrayUtil.join(lastProcessBuilder.command().toArray(), " ");
        assertEquals(commandLine, "echo Found");
    }

    @Test
    public void shouldSupportVariablesCanBeReused() {
        InMemoryStreamConsumer consumer = inMemoryConsumer();
        HashMap<String, String> env = createEnvMap(
            "Test1", "Hello",
            "Test2", "${Env.Test3}World!");
        EnvironmentVariableContext environmentVariableContext = createEnvironmentContext("Test3", "${Env.Test1}, ");
        ProcessWrapper wrapper = processManager.createProcess(new String[]{"echo", "${Env.Test2}"}, "echo 'message'", null, env, environmentVariableContext, consumer, "test-tag", null, "ERROR: ");

        String commandLine = ArrayUtil.join(lastProcessBuilder.command().toArray(), " ");
        assertEquals(commandLine, "echo Hello, World!");
    }

    @Test
    public void shouldSupportVariablesCanBeRepeated() {
        InMemoryStreamConsumer consumer = inMemoryConsumer();
        HashMap<String, String> env = createEnvMap(
                "Test1", "Hello!",
                "Test2", "Good to be back!");
        ProcessWrapper wrapper = processManager.createProcess(new String[]{"echo", "${Env.Test1}", "${Env.Test1}", "${Env.Test2}", "${Env.Test2}"}, "echo 'message'", null, env, new EnvironmentVariableContext(), consumer, "test-tag", null, "ERROR: ");

        String commandLine = ArrayUtil.join(lastProcessBuilder.command().toArray(), " ");
        assertEquals(commandLine, "echo Hello! Hello! Good to be back! Good to be back!");
    }

    @Test
    public void shouldSupportVariablesInCommand() {
        InMemoryStreamConsumer consumer = inMemoryConsumer();
        EnvironmentVariableContext environmentVariableContext = createEnvironmentContext("TOOLS_PATH", "/usr/lib");
        ProcessWrapper wrapper = processManager.createProcess(new String[]{"${Env.TOOLS_PATH}/maven/bin/mvn", "deploy"}, "echo 'message'", null, new HashMap<String, String>(), environmentVariableContext, consumer, "test-tag", null, "ERROR: ");

        String commandLine = ArrayUtil.join(lastProcessBuilder.command().toArray(), " ");
        assertEquals(commandLine, "/usr/lib/maven/bin/mvn deploy");
    }

    @Test
    public void shouldAddToProcessListWhenNewProcessCreated() {
        processManager.createProcess(new String[]{"echo", "message"}, "echo 'message'", null, new HashMap<String, String>(), new EnvironmentVariableContext(), inMemoryConsumer(), "test-tag", null,
                "ERROR: ");
        assertThat(processManager.getProcessMap().size(), is(3));
    }

    @Test
    public void shouldRemoveKilledProcessFromList() {
        processManager.processKilled(processTwo);
        assertThat(processManager.getProcessMap().size(), is(1));
        assertThat(processManager.getProcessMap().containsKey(processOne), is(true));
    }

    @Test
    public void shouldGetIdleTimeForGivenProcess() {
        processManager = new ProcessManager();
        ProcessWrapper processWrapperOne = mock(ProcessWrapper.class);
        Process processOne = mock(Process.class);
        ProcessWrapper processWrapperTwo = mock(ProcessWrapper.class);
        Process processTwo = mock(Process.class);
        ConcurrentHashMap<Process, ProcessWrapper> processMap = processManager.getProcessMap();
        processMap.put(processOne, processWrapperOne);
        processMap.put(processTwo, processWrapperTwo);

        when(processWrapperOne.getProcessTag()).thenReturn("tag1");
        when(processWrapperOne.getIdleTime()).thenReturn(200L);
        when(processWrapperTwo.getProcessTag()).thenReturn("tag2");
        when(processWrapperTwo.getIdleTime()).thenReturn(100L);

        long timeout = processManager.getIdleTimeFor("tag2");
        assertThat(timeout, is(100L));
    }

    @Test
    public void processListForDisplayShouldBeSameAsTheCurrentProcessList() throws Exception {
        processManager = new ProcessManager();
        ProcessWrapper processWrapperOne = mock(ProcessWrapper.class);
        Process processOne = mock(Process.class);
        ProcessWrapper processWrapperTwo = mock(ProcessWrapper.class);
        Process processTwo = mock(Process.class);
        ConcurrentHashMap<Process, ProcessWrapper> processMap = processManager.getProcessMap();
        processMap.put(processOne, processWrapperOne);
        processMap.put(processTwo, processWrapperTwo);

        Collection<ProcessWrapper> processWrappersForDisplay = processManager.currentProcessListForDisplay();
        assertThat(processWrappersForDisplay, is(processMap.values()));
    }
}
