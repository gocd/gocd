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
package com.thoughtworks.go.util.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class ScriptRunnerTest {
    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldReplaceSecretsOnTheOutputUnderLinux() throws CheckedCommandLineException {
        CommandLine command = CommandLine.createCommandLine("echo").withArg("My password is ").withArg(
                new PasswordArgument("secret")).withEncoding("utf-8");
        InMemoryConsumer output = new InMemoryConsumer();

        command.runScript(new ExecScript("FOO"), output, new EnvironmentVariableContext(), null);
        assertThat(output.toString()).doesNotContain("secret");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldReplaceSecretsOnTheOutputUnderWindows() throws CheckedCommandLineException {
        CommandLine command = CommandLine.createCommandLine("cmd")
                .withArg("/c")
                .withArg("echo")
                .withArg("My password is ")
                .withArg(new PasswordArgument("secret"))
                .withEncoding("utf-8");
        InMemoryConsumer output = new InMemoryConsumer();

        command.runScript(new ExecScript("FOO"), output, new EnvironmentVariableContext(), null);
        assertThat(output.toString()).doesNotContain("secret");
    }

    @Test
    void shouldReplaceSecretsInErrors() {
        CommandLine command = CommandLine.createCommandLine("notexist").withEncoding("utf-8").withArg("My password is ").withArg(
                new PasswordArgument("secret"));
        InMemoryConsumer output = new InMemoryConsumer();
        try {

            command.runScript(new ExecScript("FOO"), output, new EnvironmentVariableContext(), null);
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e.getMessage()).doesNotContain("secret");
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldBeAbleToSpecifyEncoding() throws CheckedCommandLineException {
        String chrisWasHere = "司徒空在此";
        CommandLine command = CommandLine.createCommandLine("echo").withArg(chrisWasHere).withEncoding("UTF-8");
        InMemoryConsumer output = new InMemoryConsumer();
        ExecScript script = new ExecScript("FOO");

        command.runScript(script, output, new EnvironmentVariableContext(), null);
        assertThat(output.toString()).contains(chrisWasHere);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldMaskOutOccuranceOfSecureEnvironmentVariablesValuesInTheScriptOutputOnLinux() throws CheckedCommandLineException {
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        environmentVariableContext.setProperty("secret", "the_secret_password", true);
        CommandLine command = CommandLine.createCommandLine("echo").withArg("the_secret_password").withEncoding("utf-8");
        InMemoryConsumer output = new InMemoryConsumer();
        ExecScript script = new ExecScript("ERROR_STRING");

        command.runScript(script, output, environmentVariableContext, null);
        assertThat(script.getExitCode()).isEqualTo(0);
        assertThat(output.contains("the_secret_password")).as(output.toString()).isFalse();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldMaskOutOccuranceOfSecureEnvironmentVariablesValuesInTheScriptOutput() throws CheckedCommandLineException {
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        environmentVariableContext.setProperty("secret", "the_secret_password", true);
        CommandLine command = CommandLine.createCommandLine("cmd")
                .withArg("/c")
                .withArg("echo")
                .withArg("the_secret_password")
                .withEncoding("utf-8");

        InMemoryConsumer output = new InMemoryConsumer();
        ExecScript script = new ExecScript("ERROR_STRING");

        command.runScript(script, output, environmentVariableContext, null);
        assertThat(script.getExitCode()).isEqualTo(0);
        assertThat(output.contains("the_secret_password")).as(output.toString()).isFalse();
    }
}
