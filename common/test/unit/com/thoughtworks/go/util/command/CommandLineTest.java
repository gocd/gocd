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

package com.thoughtworks.go.util.command;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.googlecode.junit.ext.checkers.OSChecker;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.util.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(JunitExtRunner.class)
public class CommandLineTest {

    private static final String DBL_QUOTE = "\"";

    private static final String EXEC_WITH_SPACES = "dummyExecutable with spaces";

    private static final String ARG_SPACES_NOQUOTES = "arg1='spaced single quoted value'";
    private static final String ARG_NOSPACES = "arg2=value2";
    private static final String ARG_SPACES = "arg3=value for 3";

    private final ArrayList<File> toDelete = new ArrayList<File>();
    private File tempFolder;
    private File subFolder;

    @Before
    public void setUp() throws Exception {
        tempFolder = TestFileUtil.createTempFolder("tempCommandLineTestFolder-" + System.currentTimeMillis());
        toDelete.add(tempFolder);

        subFolder = new File(tempFolder, "subFolder");
        subFolder.mkdirs();
        File file = new File("./originalCommand");
        file.createNewFile();
        file.setExecutable(true);
        toDelete.add(file);
    }

    @After
    public void tearDown() throws Exception {
        for (File folder : toDelete) {
            FileUtil.deleteFolder(folder);
        }
    }

    @Test
    public void testToStringWithSeparator() throws Exception {
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
    public void testToStrings() throws Exception {
        final CommandLine cl = CommandLine.createCommandLine(EXEC_WITH_SPACES);

        cl.withArg(ARG_SPACES_NOQUOTES);
        cl.withArg(ARG_NOSPACES);
        cl.withArg(ARG_SPACES);

        final String expectedWithQuotes = DBL_QUOTE + EXEC_WITH_SPACES + DBL_QUOTE
                + " " + DBL_QUOTE + ARG_SPACES_NOQUOTES + DBL_QUOTE
                + " " + ARG_NOSPACES
                + " " + DBL_QUOTE + ARG_SPACES + DBL_QUOTE;
        assertEquals(expectedWithQuotes, cl.toString());

        assertEquals(expectedWithQuotes.replaceAll(DBL_QUOTE, ""), cl.toStringForDisplay());

        assertEquals("Did the impl of CommandLine.toString() change?", expectedWithQuotes, cl + "");
    }

    @Test
    public void testToStringMisMatchedQuote() {
        final CommandLine cl2 = CommandLine.createCommandLine(EXEC_WITH_SPACES);
        final String argWithMismatchedDblQuote = "argMisMatch='singlequoted\"WithMismatchedDblQuote'";
        cl2.withArg(argWithMismatchedDblQuote);
        assertEquals("Did behavior of mismatched quotes change? Previously it would truncate args.",
                DBL_QUOTE + EXEC_WITH_SPACES + DBL_QUOTE + " ", cl2.toString());
    }

    @Test
    public void shouldReportPasswordsOnTheLogAsStars() {
        CommandLine line = CommandLine.createCommandLine("notexist").withArg(new PasswordArgument("secret"));
        assertThat(line.toString(), not(containsString("secret")));
    }

    @Test
    public void shouldLogPasswordsOnTheLogAsStars() {
        LogFixture logFixture = LogFixture.startListening();
        LogFixture.enableDebug();
        CommandLine line = CommandLine.createCommandLine("notexist").withArg(new PasswordArgument("secret"));
        try {
            line.runOrBomb(null);
        } catch (Exception e) {
            //ignored
        }
        assertThat(ArrayUtil.join(logFixture.getMessages()), containsString("notexist ******"));
        logFixture.stopListening();
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, OSChecker.WINDOWS})
    public void shouldNotLogPasswordsFromStream() {
        LogFixture logFixture = LogFixture.startListening();
        LogFixture.enableDebug();
        CommandLine line = CommandLine.createCommandLine("/bin/echo").withArg("=>").withArg(new PasswordArgument("secret"));
        line.runOrBomb(null);

        assertThat(ArrayUtil.join(logFixture.getMessages()), not(containsString("secret")));
        assertThat(ArrayUtil.join(logFixture.getMessages()), containsString("=> ******"));
        logFixture.stopListening();
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, OSChecker.WINDOWS})
    public void shouldNotLogPasswordsOnExceptionThrown() throws IOException {
        File dir = FileUtil.createTempFolder();
        File file = new File(dir, "test.sh");
        FileOutputStream out = new FileOutputStream(file);
        out.write("echo $1 && exit 10".getBytes());
        out.close();

        CommandLine line = CommandLine.createCommandLine("/bin/sh").withArg(file.getAbsolutePath()).withArg(new PasswordArgument("secret"));
        try {
            line.runOrBomb(null);
        } catch (CommandLineException e) {
            assertThat(e.getMessage(), not(containsString("secret")));
        }
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, OSChecker.WINDOWS})
    public void shouldLogPasswordsOnOutputAsStarsUnderLinux() throws IOException {
        CommandLine line = CommandLine.createCommandLine("echo")
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
    @RunIf(value = OSChecker.class, arguments = OSChecker.WINDOWS)
    public void shouldLogPasswordsOnOutputAsStarsUnderWindows() throws IOException {
        CommandLine line = CommandLine.createCommandLine("cmd")
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
    public void shouldShowPasswordsInToStringForDisplayAsStars() throws IOException {
        CommandLine line = CommandLine.createCommandLine("echo")
                .withArg("My Password is:")
                .withArg(new PasswordArgument("secret"));
        assertThat(line.toStringForDisplay(), not(containsString("secret")));
    }

    @Test
    public void shouldShowPasswordsInDescribeAsStars() throws IOException {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("password1", "secret");
        map.put("password2", "secret");
        CommandLine line = CommandLine.createCommandLine("echo")
                .withArg("My Password is:")
                .withEnv(map)
                .withArg(new PasswordArgument("secret"))
                .withArg(new PasswordArgument("new-pwd"));

        line.addInput(new String[]{"my pwd is: new-pwd "});
        assertThat(line.describe(), not(containsString("secret")));
        assertThat(line.describe(), not(containsString("new-pwd")));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, OSChecker.WINDOWS})
    public void shouldLogPasswordsOnEnvironemntAsStarsUnderLinux() throws IOException {
        CommandLine line = CommandLine.createCommandLine("echo")
                .withArg("My Password is:")
                .withArg("secret")
                .withArg(new PasswordArgument("secret"));
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
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, OSChecker.WINDOWS})
    public void shouldBeAbleToSpecifyEncoding() throws IOException {
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
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, OSChecker.WINDOWS})
    public void shouldBeAbleToRunCommandsInSubdirectories() throws IOException {

        File shellScript = createScript("hello-world.sh", "echo ${PWD}");
        assertThat(shellScript.setExecutable(true), is(true));

        CommandLine line = CommandLine.createCommandLine("./hello-world.sh").withWorkingDir(subFolder);

        InMemoryStreamConsumer out = new InMemoryStreamConsumer();
        line.execute(out, new EnvironmentVariableContext(), null).waitForExit();

        assertThat(out.getAllOutput().trim(), endsWith("subFolder"));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, OSChecker.WINDOWS})
    public void shouldBeAbleToRunCommandsInSubdirectoriesWithNoWorkingDir() throws IOException {

        File shellScript = createScript("hello-world.sh", "echo 'Hello World!'");
        assertThat(shellScript.setExecutable(true), is(true));

        CommandLine line = CommandLine.createCommandLine("subFolder/hello-world.sh").withWorkingDir(tempFolder);

        InMemoryStreamConsumer out = new InMemoryStreamConsumer();
        line.execute(out, new EnvironmentVariableContext(), null).waitForExit();

        assertThat(out.getAllOutput(), containsString("Hello World!"));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, OSChecker.WINDOWS})
    public void shouldNotRunLocalCommandsThatAreNotExecutable() throws IOException {
        createScript("echo", "echo 'this should not be here'");

        CommandLine line = CommandLine.createCommandLine("echo")
                .withArg("Using the REAL echo")
                .withWorkingDir(subFolder);

        InMemoryStreamConsumer out = new InMemoryStreamConsumer();
        line.execute(out, new EnvironmentVariableContext(), null).waitForExit();

        assertThat(out.getAllOutput(), containsString("Using the REAL echo"));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, OSChecker.WINDOWS})
    public void shouldBeAbleToRunCommandsFromRelativeDirectories() throws IOException {
        File shellScript = new File(tempFolder, "hello-world.sh");
        FileUtil.writeContentToFile("echo ${PWD}", shellScript);
        assertThat(shellScript.setExecutable(true), is(true));

        CommandLine line = CommandLine.createCommandLine("../hello-world.sh").withWorkingDir(subFolder);

        InMemoryStreamConsumer out = new InMemoryStreamConsumer();
        line.execute(out, new EnvironmentVariableContext(), null).waitForExit();

        assertThat(out.getAllOutput().trim(), endsWith("subFolder"));
    }

    private File createScript(String name, String content) throws IOException {
        File shellScript = new File(subFolder, name);

        FileUtil.writeContentToFile(content, shellScript);
        return shellScript;
    }

    @Test public void shouldReturnEchoResult() throws Exception {
        if (SystemUtil.isWindows()) {
            ConsoleResult result = CommandLine.createCommandLine("cmd").runOrBomb(null);
            assertThat(result.outputAsString(), containsString("Windows"));
        } else {
            String expectedValue = "my input";
            ConsoleResult result = CommandLine.createCommandLine("echo").withArgs(expectedValue).runOrBomb(null);
            assertThat(result.outputAsString(), is(expectedValue));

        }
    }

    @Test(expected = Exception.class)
    public void shouldReturnThrowExceptionWhenCommandNotExist() throws Exception {
        CommandLine.createCommandLine("something").runOrBomb(null);

    }

    @Test
    public void shouldGetTheCommandFromCommandlineAsIs() throws IOException {
        String file = "originalCommand";
        CommandLine command = CommandLine.createCommandLine(file);
        command.setWorkingDir(new File("."));
        String[] commandLineArgs = command.getCommandLine();
        assertThat(commandLineArgs[0], is(file));
    }

    @Test
    public void shouldPrefixStderrOutput() {
        CommandLine line = CommandLine.createCommandLine("rmdir").withArg("/a/directory/that/does/not/exist");
        InMemoryStreamConsumer output = new InMemoryStreamConsumer();
        ProcessWrapper processWrapper = line.execute(output, new EnvironmentVariableContext(), null);
        processWrapper.waitForExit();

        assertThat(output.getAllOutput(), containsString("STDERR: "));
    }
}
