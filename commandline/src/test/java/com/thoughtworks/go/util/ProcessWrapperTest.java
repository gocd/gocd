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

import com.thoughtworks.go.util.command.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessWrapperTest {

    @Test
    void shouldReturnTrueWhenAProcessIsRunning() {
        Process process = getMockedProcess(mock(OutputStream.class));
        when(process.exitValue()).thenThrow(new IllegalThreadStateException());
        ProcessWrapper processWrapper = new ProcessWrapper(process, null, "", inMemoryConsumer(), "utf-8", null);
        assertThat(processWrapper.isRunning()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenAProcessHasExited() {
        Process process = getMockedProcess(mock(OutputStream.class));
        when(process.exitValue()).thenReturn(1);
        ProcessWrapper processWrapper = new ProcessWrapper(process, null, "", inMemoryConsumer(), "utf-8", null);
        assertThat(processWrapper.isRunning()).isFalse();
    }

    @Test
    void shouldTypeInputToConsole() {
        OutputStream processInputStream = new ByteArrayOutputStream();// mock(OutputStream.class);
        Process process = getMockedProcess(processInputStream);
        ProcessWrapper processWrapper = new ProcessWrapper(process, null, "", inMemoryConsumer(), "utf-8", null);
        ArrayList<String> inputs = new ArrayList<>();
        inputs.add("input1");
        inputs.add("input2");
        processWrapper.typeInputToConsole(inputs);

        String input = processInputStream.toString();
        String[] parts = input.split("\\r?\\n");
        assertThat(parts[0]).isEqualTo("input1");
        assertThat(parts[1]).isEqualTo("input2");
    }

    @Test
    void shouldThrowExceptionWhenExecutableDoesNotExist() {
        CommandLine line = CommandLine.createCommandLine("doesnotexist").withEncoding("utf-8");
        ProcessOutputStreamConsumer outputStreamConsumer = inMemoryConsumer();
        final CommandLineException exception = assertThrows(CommandLineException.class, () -> line.execute(outputStreamConsumer, new EnvironmentVariableContext(), null));
        assertThat(exception)
                .isInstanceOf(CommandLineException.class)
                .hasMessageContaining("Make sure this command can execute manually.")
                .hasMessageContaining("doesnotexist");

        assertThat(exception.getResult()).isNotNull();
    }

    @Test
    void shouldTryCommandWithTimeout() {
        CommandLine line = CommandLine.createCommandLine("doesnotexist").withEncoding("utf-8");
        assertThatCode(() -> line.waitForSuccess(100))
                .hasMessageContaining("Timeout after 0.1 seconds waiting for command 'doesnotexist'");
    }


    @Test
    void shouldCollectOutput() {
        String output = "SYSOUT: Hello World!";
        String error = "SYSERR: Some error happened!";
        CommandLine line = CommandLine.createCommandLine("ruby").withEncoding("utf-8").withArgs(script("echo"), output, error);
        ConsoleResult result = run(line);

        assertThat(result.returnValue()).as("Errors: " + result.errorAsString()).isEqualTo(0);
        assertThat(result.output()).contains(output);
        assertThat(result.error().toString()).contains(error);
    }

    private String script(final String name) {
        return "../util/src/test/resources/executables/" + name + ".rb";
    }

    @Test
    void shouldAcceptInputString() {
        String input = "SYSIN: Hello World!";
        CommandLine line = CommandLine.createCommandLine("ruby").withEncoding("utf-8").withArgs(script("echo-input"));
        ConsoleResult result = run(line, input);
        assertThat(result.output()).contains(input);
        assertThat(result.error().size()).isEqualTo(0);
    }

    @Test
    void shouldBeAbleToCompleteInput() {
        String input1 = "SYSIN: Line 1!";
        String input2 = "SYSIN: Line 2!";
        CommandLine line = CommandLine.createCommandLine("ruby").withEncoding("utf-8").withArgs(script("echo-all-input"));
        ConsoleResult result = run(line, input1, input2);
        assertThat(result.returnValue()).isEqualTo(0);
        assertThat(result.output()).contains("You said: " + input1);
        assertThat(result.output()).contains("You said: " + input2);
        assertThat(result.error().size()).isEqualTo(0);
    }

    @Test
    void shouldReportReturnValueIfProcessFails() {
        CommandLine line = CommandLine.createCommandLine("ruby").withEncoding("utf-8").withArgs(script("nonexistent-script"));
        ConsoleResult result = run(line);
        assertThat(result.returnValue()).isEqualTo(1);
    }

    @Test
    void shouldSetGoServerVariablesIfTheyExist() {
        System.setProperty("GO_DEPENDENCY_LABEL_PIPELINE_NAME", "999");
        CommandLine line = CommandLine.createCommandLine("ruby").withEncoding("utf-8").withArgs(script("dump-environment"));
        ConsoleResult result = run(line);
        assertThat(result.returnValue()).as("Errors: " + result.errorAsString()).isEqualTo(0);
        assertThat(result.output()).contains("GO_DEPENDENCY_LABEL_PIPELINE_NAME=999");
    }

    private ConsoleResult run(CommandLine line, String... inputs) {
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();

        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        environmentVariableContext.setProperty("GO_DEPENDENCY_LABEL_PIPELINE_NAME", "999", false);
        line.addInput(inputs);
        ProcessWrapper processWrapper = line.execute(outputStreamConsumer, environmentVariableContext, null);
        return new ConsoleResult(processWrapper.waitForExit(),
                outputStreamConsumer.getStdLines(),
                outputStreamConsumer.getErrLines(), line.getArguments(), new ArrayList<>());
    }

    private Process getMockedProcess(OutputStream outputStream) {
        Process process = mock(Process.class);
        when(process.getErrorStream()).thenReturn(mock(InputStream.class));
        when(process.getInputStream()).thenReturn(mock(InputStream.class));
        when(process.getOutputStream()).thenReturn(outputStream);
        return process;
    }

}
