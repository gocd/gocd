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

import ch.qos.logback.classic.Level;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.ProcessManager;
import com.thoughtworks.go.util.ProcessWrapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.regex.Matcher;

import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommandLineTest {

    private static final String DBL_QUOTE = "\"";

    private static final String EXEC_WITH_SPACES = "dummyExecutable with spaces";

    private static final String ARG_SPACES_NOQUOTES = "arg1='spaced single quoted value'";
    private static final String ARG_NOSPACES = "arg2=value2";
    private static final String ARG_SPACES = "arg3=value for 3";

    @TempDir
    public Path temporaryFolder;
    private File subFolder;

    @BeforeEach
    public void setUp() throws IOException {
        subFolder = Files.createDirectory(temporaryFolder.resolve("subFolder")).toFile();
    }

    @Test
    void testToStringWithSeparator() {
        final String separator = "], [";
        assertEquals("", CommandLine.toString(null, false, separator));

        assertEquals(ARG_SPACES_NOQUOTES,
                CommandLine.toString(new String[]{ARG_SPACES_NOQUOTES}, false, separator));

        assertEquals(ARG_SPACES_NOQUOTES + separator + ARG_NOSPACES,
                CommandLine.toString(new String[]{ARG_SPACES_NOQUOTES, ARG_NOSPACES}, false, separator));

        assertEquals(ARG_SPACES_NOQUOTES + separator + ARG_NOSPACES + separator + ARG_SPACES,
                CommandLine.toString(new String[]{ARG_SPACES_NOQUOTES, ARG_NOSPACES, ARG_SPACES},
                        false, separator));
    }

    @Test
    void testToStrings() {
        final CommandLine cl = CommandLine.createCommandLine(EXEC_WITH_SPACES).withEncoding("utf-8");

        cl.withArg(ARG_SPACES_NOQUOTES);
        cl.withArg(ARG_NOSPACES);
        cl.withArg(ARG_SPACES);

        final String expectedWithQuotes = DBL_QUOTE + EXEC_WITH_SPACES + DBL_QUOTE
                + " " + DBL_QUOTE + ARG_SPACES_NOQUOTES + DBL_QUOTE
                + " " + ARG_NOSPACES
                + " " + DBL_QUOTE + ARG_SPACES + DBL_QUOTE;
        assertEquals(expectedWithQuotes, cl.toString());

        assertEquals(expectedWithQuotes.replaceAll(DBL_QUOTE, ""), cl.toStringForDisplay());

        assertEquals(expectedWithQuotes, cl + "", "Did the impl of CommandLine.toString() change?");
    }

    @Test
    void testToStringMisMatchedQuote() {
        final CommandLine cl2 = CommandLine.createCommandLine(EXEC_WITH_SPACES).withEncoding("utf-8");
        final String argWithMismatchedDblQuote = "argMisMatch='singlequoted\"WithMismatchedDblQuote'";
        cl2.withArg(argWithMismatchedDblQuote);
        assertEquals(DBL_QUOTE + EXEC_WITH_SPACES + DBL_QUOTE + " " +
                        DBL_QUOTE + argWithMismatchedDblQuote.replaceAll("\"", Matcher.quoteReplacement("\\\"")) + DBL_QUOTE, cl2.toString(),
                "Should escape double quotes inside the string");
    }

    @Test
    void shouldReportPasswordsOnTheLogAsStars() {
        CommandLine line = CommandLine.createCommandLine("notexist").withArg(new PasswordArgument("secret")).withEncoding("utf-8");
        assertThat(line.toString(), not(containsString("secret")));
    }

    @Test
    void shouldLogPasswordsOnTheLogAsStars() {
        try (LogFixture logFixture = logFixtureFor(ProcessManager.class, Level.DEBUG)) {
            CommandLine line = CommandLine.createCommandLine("notexist").withArg(new PasswordArgument("secret")).withEncoding("utf-8");
            try {
                line.runOrBomb(null);
            } catch (Exception e) {
                //ignored
            }
            assertThat(logFixture.getLog(), containsString("notexist ******"));
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldNotLogPasswordsFromStream() {
        try (LogFixture logFixture = logFixtureFor(CommandLine.class, Level.DEBUG)) {
            CommandLine line = CommandLine.createCommandLine("/bin/echo").withArg("=>").withArg(new PasswordArgument("secret")).withEncoding("utf-8");
            line.runOrBomb(null);
            assertThat(logFixture.getLog(), not(containsString("secret")));
            assertThat(logFixture.getLog(), containsString("=> ******"));
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldNotLogPasswordsOnExceptionThrown() throws IOException {
        File file = Files.writeString(temporaryFolder.resolve("test.sh"), "echo $1 && exit 10", UTF_8).toFile();

        CommandLine line = CommandLine.createCommandLine("/bin/sh")
                .withArg(file.getAbsolutePath())
                .withArg(new PasswordArgument("secret"))
                .withEncoding("utf-8");

        assertThatThrownBy(() -> line.runOrBomb(null))
                .isExactlyInstanceOf(CommandLineException.class)
                .hasMessageContaining("EXIT CODE (10)")
                .hasMessageNotContaining("secret");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldLogPasswordsOnOutputAsStarsUnderLinux() {
        CommandLine line = CommandLine.createCommandLine("echo")
                .withArg("My Password is:")
                .withArg(new PasswordArgument("secret"))
                .withEncoding("utf-8");
        InMemoryStreamConsumer output = new InMemoryStreamConsumer();
        InMemoryStreamConsumer displayOutputStreamConsumer = InMemoryStreamConsumer.inMemoryConsumer();
        ProcessWrapper processWrapper = line.execute(output, new EnvironmentVariableContext(), null);
        processWrapper.waitForExit();

        assertThat(output.getAllOutput(), containsString("secret"));
        assertThat(displayOutputStreamConsumer.getAllOutput(), not(containsString("secret")));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldLogPasswordsOnOutputAsStarsUnderWindows() {
        CommandLine line = CommandLine.createCommandLine("cmd")
                .withEncoding("utf-8")
                .withArg("/c")
                .withArg("echo")
                .withArg("My Password is:")
                .withArg(new PasswordArgument("secret"));
        InMemoryStreamConsumer output = new InMemoryStreamConsumer();
        InMemoryStreamConsumer displayOutputStreamConsumer = InMemoryStreamConsumer.inMemoryConsumer();
        ProcessWrapper processWrapper = line.execute(output, new EnvironmentVariableContext(), null);
        processWrapper.waitForExit();

        assertThat(output.getAllOutput(), containsString("secret"));
        assertThat(displayOutputStreamConsumer.getAllOutput(), not(containsString("secret")));
    }

    @Test
    void shouldShowPasswordsInToStringForDisplayAsStars() {
        CommandLine line = CommandLine.createCommandLine("echo")
                .withArg("My Password is:")
                .withArg(new PasswordArgument("secret"))
                .withEncoding("utf-8");
        assertThat(line.toStringForDisplay(), not(containsString("secret")));
    }

    @Test
    void shouldShowPasswordsInDescribeAsStars() {
        HashMap<String, String> map = new HashMap<>();
        map.put("password1", "secret");
        map.put("password2", "secret");
        CommandLine line = CommandLine.createCommandLine("echo")
                .withArg("My Password is:")
                .withEnv(map)
                .withArg(new PasswordArgument("secret"))
                .withArg(new PasswordArgument("new-pwd"))
                .withEncoding("utf-8");

        line.addInput(new String[]{"my pwd is: new-pwd "});
        assertThat(line.describe(), not(containsString("secret")));
        assertThat(line.describe(), not(containsString("new-pwd")));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldLogPasswordsOnEnvironmentAsStarsUnderLinux() {
        CommandLine line = CommandLine.createCommandLine("echo")
                .withArg("My Password is:")
                .withArg("secret")
                .withArg(new PasswordArgument("secret"))
                .withEncoding("utf-8");
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        environmentVariableContext.setProperty("ENV_PASSWORD", "secret", false);
        InMemoryStreamConsumer output = new InMemoryStreamConsumer();

        InMemoryStreamConsumer forDisplay = InMemoryStreamConsumer.inMemoryConsumer();
        ProcessWrapper processWrapper = line.execute(output, environmentVariableContext, null);
        processWrapper.waitForExit();


        assertThat(forDisplay.getAllOutput(), not(containsString("secret")));
        assertThat(output.getAllOutput(), containsString("secret"));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldBeAbleToSpecifyEncoding() {
        String chrisWasHere = "?????";
        CommandLine line = CommandLine.createCommandLine("echo")
                .withArg(chrisWasHere)
                .withEncoding("UTF-8");
        InMemoryStreamConsumer output = new InMemoryStreamConsumer();
        ProcessWrapper processWrapper = line.execute(output, new EnvironmentVariableContext(), null);
        processWrapper.waitForExit();

        assertThat(output.getAllOutput(), containsString(chrisWasHere));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldBeAbleToRunCommandsInSubdirectories() throws IOException {
        File shellScript = createScriptInSubFolder("hello-world.sh", "echo ${PWD}");
        assertThat(shellScript.setExecutable(true), is(true));

        CommandLine line = CommandLine.createCommandLine("./hello-world.sh").withWorkingDir(subFolder).withEncoding("utf-8");

        InMemoryStreamConsumer out = new InMemoryStreamConsumer();
        line.execute(out, new EnvironmentVariableContext(), null).waitForExit();

        assertThat(out.getAllOutput().trim(), endsWith("subFolder"));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldBeAbleToRunCommandsInSubdirectoriesWithNoWorkingDir() throws IOException {
        File shellScript = createScriptInSubFolder("hello-world.sh", "echo 'Hello World!'");
        assertThat(shellScript.setExecutable(true), is(true));

        CommandLine line = CommandLine.createCommandLine("subFolder/hello-world.sh").withWorkingDir(temporaryFolder.toFile()).withEncoding("utf-8");

        InMemoryStreamConsumer out = new InMemoryStreamConsumer();
        line.execute(out, new EnvironmentVariableContext(), null).waitForExit();

        assertThat(out.getAllOutput(), containsString("Hello World!"));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldNotRunLocalCommandsThatAreNotExecutable() throws IOException {
        createScriptInSubFolder("echo", "echo 'this should not be here'");

        CommandLine line = CommandLine.createCommandLine("echo")
                .withArg("Using the REAL echo")
                .withWorkingDir(subFolder)
                .withEncoding("utf-8");

        InMemoryStreamConsumer out = new InMemoryStreamConsumer();
        line.execute(out, new EnvironmentVariableContext(), null).waitForExit();

        assertThat(out.getAllOutput(), containsString("Using the REAL echo"));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldBeAbleToRunCommandsFromRelativeDirectories() throws IOException {

        File shellScript = Files.createFile(temporaryFolder.resolve("hello-world.sh")).toFile();

        FileUtils.writeStringToFile(shellScript, "echo ${PWD}", UTF_8);
        assertThat(shellScript.setExecutable(true), is(true));

        CommandLine line = CommandLine.createCommandLine("../hello-world.sh").withWorkingDir(subFolder).withEncoding("utf-8");

        InMemoryStreamConsumer out = new InMemoryStreamConsumer();
        line.execute(out, new EnvironmentVariableContext(), null).waitForExit();

        assertThat(out.getAllOutput().trim(), endsWith("subFolder"));
    }

    private File createScriptInSubFolder(String name, String content) throws IOException {
        File shellScript = new File(subFolder, name);

        FileUtils.writeStringToFile(shellScript, content, UTF_8);
        return shellScript;
    }

    @Test
    void shouldReturnEchoResult() {
        if (SystemUtils.IS_OS_WINDOWS) {
            ConsoleResult result = CommandLine.createCommandLine("cmd").withEncoding("utf-8").runOrBomb(null);
            assertThat(result.outputAsString(), containsString("Windows"));
        } else {
            String expectedValue = "my input";
            ConsoleResult result = CommandLine.createCommandLine("echo").withEncoding("utf-8").withArgs(expectedValue).runOrBomb(null);
            assertThat(result.outputAsString(), is(expectedValue));

        }
    }

    @Test
    void shouldReturnThrowExceptionWhenCommandNotExist() {
        assertThatCode(() -> CommandLine.createCommandLine("something").withEncoding("utf-8").runOrBomb(null))
                .isInstanceOf(Exception.class);

    }

    @Test
    void shouldGetTheCommandFromCommandlineAsIs() throws IOException {
        String file = "originalCommand";
        Files.createFile(temporaryFolder.resolve(file)).toFile().setExecutable(true);

        CommandLine command = CommandLine.createCommandLine(file);
        command.setWorkingDir(new File("."));
        String[] commandLineArgs = command.getCommandLine();
        assertThat(commandLineArgs[0], is(file));
    }

    @Test
    void shouldPrefixStderrOutput() {
        CommandLine line = CommandLine.createCommandLine("git")
                .withArg("clone")
                .withArg("https://foo/bar")
                .withEncoding("utf-8");
        InMemoryStreamConsumer output = new InMemoryStreamConsumer();
        ProcessWrapper processWrapper = line.execute(output, new EnvironmentVariableContext(), null);
        processWrapper.waitForExit();

        assertThat(output.getAllOutput(), containsString("STDERR: "));
    }
}
