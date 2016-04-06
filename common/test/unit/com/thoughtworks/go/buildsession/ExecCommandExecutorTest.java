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
package com.thoughtworks.go.buildsession;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.util.ArrayUtil;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.SystemUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static com.thoughtworks.go.domain.BuildCommand.*;
import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static com.thoughtworks.go.matchers.ConsoleOutMatcher.printedAppsMissingInfoOnUnix;
import static com.thoughtworks.go.matchers.ConsoleOutMatcher.printedAppsMissingInfoOnWindows;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

@RunWith(JunitExtRunner.class)
public class ExecCommandExecutorTest extends BuildSessionBasedTestCase {
    @Test
    public void execExecuteExternalCommandAndConnectOutputToBuildConsole() {
        runBuild(exec("echo", "foo"), Passed);
        assertThat(console.lastLine(), is("foo"));
    }

    @Test
    public void execShouldFailIfWorkingDirectoryNotExists() {
        runBuild(exec("echo", "should not show").setWorkingDirectory("not-exists"), Failed);
        assertThat(console.lineCount(), is(1));
        assertThat(console.firstLine(), containsString("not-exists\" is not a directory!"));
    }

    @Test
    public void execUseSystemEnvironmentVariables() {
        runBuild(execEchoEnv(pathSystemEnvName()), Passed);
        assertThat(console.output(), is(System.getenv(pathSystemEnvName())));
    }

    @Test
    public void execUsePresetEnvs() {
        BuildSession buildSession = newBuildSession();
        buildSession.setEnv("GO_SERVER_URL", "https://far.far.away/go");
        runBuild(buildSession, execEchoEnv("GO_SERVER_URL"), Passed);
        assertThat(console.output(), is("https://far.far.away/go"));
    }

    @Test
    public void execUseExportedEnv() throws IOException {
        runBuild(compose(
                export("foo", "bar", false),
                execEchoEnv("foo")), Passed);
        assertThat(console.lastLine(), is("bar"));
    }

    @Test
    public void execUseExportedEnvWithOverridden() throws Exception {
        runBuild(compose(
                export("answer", "2", false),
                export("answer", "42", false),
                execEchoEnv("answer")), Passed);
        assertThat(console.lastLine(), is("42"));
    }


    @Test
    public void execUseOverriddenSystemEnvValue() throws Exception {
        runBuild(compose(
                export(pathSystemEnvName(), "/foo/bar", false),
                execEchoEnv(pathSystemEnvName())), Passed);
        assertThat(console.lastLine(), is("/foo/bar"));
    }


    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void execExecuteNotExistExternalCommandOnUnix() {
        runBuild(exec("not-not-not-exist"), Failed);
        assertThat(console.output(), printedAppsMissingInfoOnUnix("not-not-not-exist"));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.WINDOWS})
    public void execExecuteNotExistExternalCommandOnWindows() {
        runBuild(exec("not-not-not-exist"), Failed);
        assertThat(console.output(), printedAppsMissingInfoOnWindows("not-not-not-exist"));
    }

    public void shouldNotLeakSecretsToConsoleLog() {
        runBuild(compose(secret("topsecret"),
                exec("not-not-not-exist", "topsecret")), Failed);
        if (!SystemUtil.isWindows()) {
            assertThat(console.output(), containsString("not-not-not-exist ******"));
        }
        assertThat(console.output(), not(containsString("topsecret")));
    }

    @Test
    public void shouldNotLeakSecretsToLog() {
        LogFixture logFixture = LogFixture.startListening();
        try {
            LogFixture.enableDebug();
            runBuild(compose(secret("topsecret"),
                    exec("not-not-not-exist", "topsecret")), Failed);
            String logs = ArrayUtil.join(logFixture.getMessages());
            assertThat(logs, containsString("not-not-not-exist ******"));
            assertThat(logs, not(containsString("topsecret")));
        } finally {
            logFixture.stopListening();
        }
    }


    private BuildCommand execEchoEnv(final String envname) {
        if (SystemUtil.isWindows()) {
            return exec("echo", "%" + envname + "%");
        } else {
            return exec("/bin/sh", "-c", String.format("echo ${%s}", envname));
        }
    }
}