/*************************GO-LICENSE-START*********************************
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util.command;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.googlecode.junit.ext.checkers.OSChecker;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(JunitExtRunner.class)
public class ScriptRunnerTest {
    @Test
    @RunIf(value = OSChecker.class, arguments = OSChecker.LINUX)
    public void shouldReplaceSecretsOnTheOutputUnderLinux() throws CheckedCommandLineException {
        CommandLine command = CommandLine.createCommandLine("echo").withArg("My password is ").withArg(
                new PasswordArgument("secret")).withEncoding("utf-8");
        InMemoryConsumer output = new InMemoryConsumer();

        command.runScript(new ExecScript("FOO"), output, new EnvironmentVariableContext(), null);
        assertThat(output.toString(), not(containsString("secret")));
    }

    @Test
    @RunIf(value = OSChecker.class, arguments = OSChecker.WINDOWS)
    public void shouldReplaceSecretsOnTheOutputUnderWindows() throws CheckedCommandLineException {
        CommandLine command = CommandLine.createCommandLine("cmd")
                .withArg("/c")
                .withArg("echo")
                .withArg("My password is ")
                .withArg(new PasswordArgument("secret"))
                .withEncoding("utf-8");
        InMemoryConsumer output = new InMemoryConsumer();

        command.runScript(new ExecScript("FOO"), output, new EnvironmentVariableContext(), null);
        assertThat(output.toString(), not(containsString("secret")));
    }

    @Test
    public void shouldReplaceSecretsInErrors() throws CheckedCommandLineException {
        CommandLine command = CommandLine.createCommandLine("notexist").withEncoding("utf-8").withArg("My password is ").withArg(
                new PasswordArgument("secret"));
        InMemoryConsumer output = new InMemoryConsumer();
        try {

            command.runScript(new ExecScript("FOO"), output, new EnvironmentVariableContext(), null);
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e.getMessage(), not(containsString("secret")));
        }
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, OSChecker.WINDOWS})
    public void shouldBeAbleToSpecifyEncoding() throws CheckedCommandLineException {
        String chrisWasHere = "司徒空在此";
        CommandLine command = CommandLine.createCommandLine("echo").withArg(chrisWasHere).withEncoding("UTF-8");
        InMemoryConsumer output = new InMemoryConsumer();
        ExecScript script = new ExecScript("FOO");

        command.runScript(script, output, new EnvironmentVariableContext(), null);
        assertThat(output.toString(), containsString(chrisWasHere));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, OSChecker.WINDOWS})
    public void shouldMaskOutOccuranceOfSecureEnvironmentVariablesValuesInTheScriptOutputOnLinux() throws CheckedCommandLineException {
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        environmentVariableContext.setProperty("secret", "the_secret_password", true);
        CommandLine command = CommandLine.createCommandLine("echo").withArg("the_secret_password").withEncoding("utf-8");
        InMemoryConsumer output = new InMemoryConsumer();
        ExecScript script = new ExecScript("ERROR_STRING");

        command.runScript(script, output, environmentVariableContext, null);
        System.out.println("\n\n\nOutput: " + output.asList().toString());
        assertThat(script.getExitCode(), is(0));
        assertThat(output.toString(), output.contains("the_secret_password"), is(false));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = OSChecker.WINDOWS)
    public void shouldMaskOutOccuranceOfSecureEnvironmentVariablesValuesInTheScriptOutput() throws CheckedCommandLineException {
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
        System.out.println("\n\n\nOutput: " + output.asList().toString());
        assertThat(script.getExitCode(), is(0));
        assertThat(output.toString(), output.contains("the_secret_password"), is(false));
    }
}
