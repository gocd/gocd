/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import ch.qos.logback.classic.Level;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.util.LogFixture;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static com.thoughtworks.go.domain.BuildCommand.*;
import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.matchers.ConsoleOutMatcherJunit5.assertConsoleOut;
import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static org.assertj.core.api.Assertions.assertThat;

class ExecCommandExecutorTest extends BuildSessionBasedTestCase {
    @Test
    void execExecuteExternalCommandAndConnectOutputToBuildConsole() {
        runBuild(exec("echo", "foo"), Passed);
        assertThat(console.lastLine()).isEqualTo("foo");
    }

    @Test
    void execShouldFailIfWorkingDirectoryNotExists() {
        runBuild(exec("echo", "should not show").setWorkingDirectory("not-exists"), Failed);
        assertThat(console.lineCount()).isEqualTo(1);
        assertThat(console.firstLine()).contains("not-exists\" is not a directory!");
    }

    @Test
    void execUseSystemEnvironmentVariables() {
        runBuild(execEchoEnv(pathSystemEnvName()), Passed);
        assertThat(console.output()).isEqualTo(System.getenv(pathSystemEnvName()));
    }

    @Test
    void execUsePresetEnvs() {
        BuildSession buildSession = newBuildSession();
        buildSession.setEnv("GO_SERVER_URL", "https://far.far.away/go");
        runBuild(buildSession, execEchoEnv("GO_SERVER_URL"), Passed);
        assertThat(console.output()).isEqualTo("https://far.far.away/go");
    }

    @Test
    void execUseExportedEnv() {
        runBuild(compose(
                export("foo", "bar", false),
                execEchoEnv("foo")), Passed);
        assertThat(console.lastLine()).isEqualTo("bar");
    }

    @Test
    void execUseExportedEnvWithOverridden() {
        runBuild(compose(
                export("answer", "2", false),
                export("answer", "42", false),
                execEchoEnv("answer")), Passed);
        assertThat(console.lastLine()).isEqualTo("42");
    }


    @Test
    void execUseOverriddenSystemEnvValue() {
        runBuild(compose(
                export(pathSystemEnvName(), "/foo/bar", false),
                execEchoEnv(pathSystemEnvName())), Passed);
        assertThat(console.lastLine()).isEqualTo("/foo/bar");
    }


    @Test
    @DisabledOnOs(OS.WINDOWS)
    void execExecuteNotExistExternalCommandOnUnix() {
        runBuild(exec("not-not-not-exist"), Failed);
        assertConsoleOut(console.output()).printedAppsMissingInfoOnUnix("not-not-not-exist");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void execExecuteNotExistExternalCommandOnWindows() {
        runBuild(exec("not-not-not-exist"), Failed);
        assertConsoleOut(console.output()).printedAppsMissingInfoOnWindows("not-not-not-exist");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldNotLeakSecretsToConsoleLog() {
        runBuild(compose(secret("topsecret"),
                exec("not-not-not-exist", "topsecret")), Failed);
        assertThat(console.output()).contains("not-not-not-exist ******");
        assertThat(console.output()).doesNotContain("topsecret");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldNotLeakSecretsToLog() {
        try (LogFixture logFixture = logFixtureFor(ExecCommandExecutor.class, Level.DEBUG)) {
            runBuild(compose(secret("topsecret"),
                    exec("not-not-not-exist", "topsecret")), Failed);
            String logs = logFixture.getLog();
            assertThat(logs).contains("not-not-not-exist ******");
            assertThat(logs).doesNotContain("topsecret");
        }
    }

    private BuildCommand execEchoEnv(final String envname) {
        if (SystemUtils.IS_OS_WINDOWS) {
            return exec("echo", "%" + envname + "%");
        } else {
            return exec("/bin/sh", "-c", String.format("echo ${%s}", envname));
        }
    }
}
