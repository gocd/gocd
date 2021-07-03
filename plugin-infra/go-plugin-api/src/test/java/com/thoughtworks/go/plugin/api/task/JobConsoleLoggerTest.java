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
package com.thoughtworks.go.plugin.api.task;

import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

public class JobConsoleLoggerTest {
    private TaskExecutionContext taskExecutionContext;
    private com.thoughtworks.go.plugin.api.task.Console mockedConsole;
    private JobConsoleLogger consoleLogger;
    private EnvironmentVariables environment;

    @BeforeEach
    public void setup() {
        taskExecutionContext = mock(TaskExecutionContext.class);
        mockedConsole = mock(com.thoughtworks.go.plugin.api.task.Console.class);
        when(taskExecutionContext.console()).thenReturn(mockedConsole);
        environment = mock(EnvironmentVariables.class);
        when(taskExecutionContext.environment()).thenReturn(environment);
        ReflectionUtil.setStaticField(JobConsoleLogger.class, "context", taskExecutionContext);
        consoleLogger = JobConsoleLogger.getConsoleLogger();
    }

    @AfterEach
    public void teardown() {
        ReflectionUtil.setStaticField(JobConsoleLogger.class, "context", null);
    }

    @Test
    public void shouldFailGetLoggerIfContextIsNotSet() {
        ReflectionUtil.setStaticField(JobConsoleLogger.class, "context", null);
        try {
            JobConsoleLogger.getConsoleLogger();
            fail("expected this to fail");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("context is null"));
        }
    }

    @Test
    public void shouldDelegatePrintLineToConsole() {
        String line = "some line";

        consoleLogger.printLine(line);

        verify(mockedConsole).printLine(line);
    }

    @Test
    public void shouldDelegateReadOutputOfToConsole() {
        InputStream inputStream = mock(InputStream.class);

        consoleLogger.readOutputOf(inputStream);

        verify(mockedConsole).readOutputOf(inputStream);
    }

    @Test
    public void shouldDelegateReadErrorOfToConsole() {
        InputStream inputStream = mock(InputStream.class);

        consoleLogger.readErrorOf(inputStream);

        verify(mockedConsole).readErrorOf(inputStream);
    }

    @Test
    public void shouldDelegatePrintEnvironmentToConsole() {
        com.thoughtworks.go.plugin.api.task.Console.SecureEnvVarSpecifier secureEnvVarSpecifier = mock(com.thoughtworks.go.plugin.api.task.Console.SecureEnvVarSpecifier.class);
        when(environment.secureEnvSpecifier()).thenReturn(secureEnvVarSpecifier);
        HashMap<String, String> environmentVariablesMap = new HashMap<>();

        consoleLogger.printEnvironment(environmentVariablesMap);

        verify(mockedConsole).printEnvironment(environmentVariablesMap, secureEnvVarSpecifier);
    }
}
