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

package com.thoughtworks.go.util;

import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.core.Is.is;
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

    @Before
    public void setUp() {
        processManager = new ProcessManager() {
            @Override
            Process startProcess(ProcessBuilder processBuilder, String msgCommandInfo) {
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
        ConcurrentMap<Process, ProcessWrapper> processMap = processManager.getProcessMap();
        wrapperForProcessOne = new ProcessWrapper(processOne, "tag1", null, inMemoryConsumer(), null, "ERROR: ");
        processMap.put(processOne, wrapperForProcessOne);
        wrapperForProcessTwo = new ProcessWrapper(processTwo, "tag2", null, inMemoryConsumer(), null, "ERROR: ");
        processMap.put(processTwo, wrapperForProcessTwo);
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
        ConcurrentMap<Process, ProcessWrapper> processMap = processManager.getProcessMap();
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
        ConcurrentMap<Process, ProcessWrapper> processMap = processManager.getProcessMap();
        processMap.put(processOne, processWrapperOne);
        processMap.put(processTwo, processWrapperTwo);

        Collection<ProcessWrapper> processWrappersForDisplay = processManager.currentProcessListForDisplay();
        assertThat(processWrappersForDisplay, is(processMap.values()));
    }

    @Test
    public void canGetProcessLevelEnvironmentVariableNames() {
        ListUtil.find(processManager.environmentVariableNames(), new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                return ((String) item).equalsIgnoreCase("path");
            }
        });

    }
}
