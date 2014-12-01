package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.api.task.EnvironmentVariables;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConsoleWrapperTest {
    private TaskExecutionContext taskExecutionContext;
    private com.thoughtworks.go.plugin.api.task.Console mockedConsole;
    private ConsoleWrapper consoleWrapper;
    private EnvironmentVariables environment;

    @Before
    public void setup() {
        taskExecutionContext = mock(TaskExecutionContext.class);
        mockedConsole = mock(com.thoughtworks.go.plugin.api.task.Console.class);
        when(taskExecutionContext.console()).thenReturn(mockedConsole);
        environment = mock(EnvironmentVariables.class);
        when(taskExecutionContext.environment()).thenReturn(environment);
        consoleWrapper = new ConsoleWrapper(taskExecutionContext);
    }

    @Test
    public void shouldDelegatePrintLineToConsole() {
        String line = "some line";

        consoleWrapper.printLine(line);

        verify(mockedConsole).printLine(line);
    }

    @Test
    public void shouldDelegateReadOutputOfToConsole() {
        InputStream inputStream = mock(InputStream.class);

        consoleWrapper.readOutputOf(inputStream);

        verify(mockedConsole).readOutputOf(inputStream);
    }

    @Test
    public void shouldDelegateReadErrorOfToConsole() {
        InputStream inputStream = mock(InputStream.class);

        consoleWrapper.readErrorOf(inputStream);

        verify(mockedConsole).readErrorOf(inputStream);
    }

    @Test
    public void shouldDelegatePrintEnvironmentToConsole() {
        com.thoughtworks.go.plugin.api.task.Console.SecureEnvVarSpecifier secureEnvVarSpecifier = mock(com.thoughtworks.go.plugin.api.task.Console.SecureEnvVarSpecifier.class);
        when(environment.secureEnvSpecifier()).thenReturn(secureEnvVarSpecifier);
        HashMap<String, String> environmentVariablesMap = new HashMap<String, String>();

        consoleWrapper.printEnvironment(environmentVariablesMap);

        verify(mockedConsole).printEnvironment(environmentVariablesMap, secureEnvVarSpecifier);
    }
}