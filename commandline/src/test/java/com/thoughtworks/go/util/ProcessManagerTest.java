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
package com.thoughtworks.go.util;

import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessManagerTest {

    private ProcessManager processManager;
    private Process processOne;
    private Process processTwo;
    private ProcessWrapper wrapperForProcessOne;
    private ProcessWrapper wrapperForProcessTwo;
    private Process processStartedByManager;
    private ProcessTag tag1;
    private ProcessTag tag2;

    @BeforeEach
    void setUp() {
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
        tag1 = mock(ProcessTag.class);
        wrapperForProcessOne = new ProcessWrapper(processOne, tag1, null, inMemoryConsumer(), "utf-8", "ERROR: ");
        processMap.put(processOne, wrapperForProcessOne);
        tag2 = mock(ProcessTag.class);
        wrapperForProcessTwo = new ProcessWrapper(processTwo, tag2, null, inMemoryConsumer(), "utf-8", "ERROR: ");
        processMap.put(processTwo, wrapperForProcessTwo);
    }

    @Test
    void shouldAddToProcessListWhenNewProcessCreated() {
        processManager.createProcess(new String[]{"echo", "message"}, "echo 'message'", null, new HashMap<>(), new EnvironmentVariableContext(), inMemoryConsumer(), null, "utf-8",
                "ERROR: ");
        assertThat(processManager.getProcessMap().size()).isEqualTo(3);
    }

    @Test
    void shouldRemoveKilledProcessFromList() {
        processManager.processKilled(processTwo);
        assertThat(processManager.getProcessMap().size()).isEqualTo(1);
        assertThat(processManager.getProcessMap().containsKey(processOne)).isTrue();
    }

    @Test
    void shouldGetIdleTimeForGivenProcess() {
        processManager = new ProcessManager();
        ProcessWrapper processWrapperOne = mock(ProcessWrapper.class);
        Process processOne = mock(Process.class);
        ProcessWrapper processWrapperTwo = mock(ProcessWrapper.class);
        Process processTwo = mock(Process.class);
        ConcurrentMap<Process, ProcessWrapper> processMap = processManager.getProcessMap();
        processMap.put(processOne, processWrapperOne);
        processMap.put(processTwo, processWrapperTwo);

        when(processWrapperOne.getProcessTag()).thenReturn(tag1);
        when(processWrapperOne.getIdleTime()).thenReturn(200L);
        when(processWrapperTwo.getProcessTag()).thenReturn(tag2);
        when(processWrapperTwo.getIdleTime()).thenReturn(100L);

        long timeout = processManager.getIdleTimeFor(tag2);
        assertThat(timeout).isEqualTo(100L);
    }

    @Test
    void processListForDisplayShouldBeSameAsTheCurrentProcessList() {
        processManager = new ProcessManager();
        ProcessWrapper processWrapperOne = mock(ProcessWrapper.class);
        Process processOne = mock(Process.class);
        ProcessWrapper processWrapperTwo = mock(ProcessWrapper.class);
        Process processTwo = mock(Process.class);
        ConcurrentMap<Process, ProcessWrapper> processMap = processManager.getProcessMap();
        processMap.put(processOne, processWrapperOne);
        processMap.put(processTwo, processWrapperTwo);

        Collection<ProcessWrapper> processWrappersForDisplay = processManager.currentProcessListForDisplay();
        assertThat(processWrappersForDisplay).isEqualTo(processMap.values());
    }

    @Test
    void canGetProcessLevelEnvironmentVariableNames() {
        final String path = processManager.environmentVariableNames().stream().filter(item -> item.equalsIgnoreCase("path")).findFirst().orElse(null);

        assertThat(path).isEqualToIgnoringCase("PATH");
    }
}
