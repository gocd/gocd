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


import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static com.thoughtworks.go.domain.BuildCommand.*;
import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.domain.JobState.*;
import static org.assertj.core.api.Assertions.assertThat;

class BuildSessionTest extends BuildSessionBasedTestCase {
    @Test
    void resolveRelativeDir() {
        BuildSession buildSession = newBuildSession();
        assertThat(buildSession.resolveRelativeDir("foo")).isEqualTo(new File(sandbox, "foo"));
        assertThat(buildSession.resolveRelativeDir("foo", "bar")).isEqualTo(new File(sandbox, "foo/bar"));
        assertThat(buildSession.resolveRelativeDir("", "bar")).isEqualTo(new File(sandbox, "bar"));
    }

    @Test
    void echoCommandAppendContentToConsole() {
        runBuild(echo("o1o2"), Passed);
        assertThat(console.asList()).isEqualTo(Collections.singletonList("o1o2"));
    }

    @Test
    void testReportCurrentStatus() {
        runBuild(compose(
                reportCurrentStatus(Preparing),
                reportCurrentStatus(Building),
                reportCurrentStatus(Completing)), Passed);
        assertThat(statusReporter.status()).isEqualTo(Arrays.asList(Preparing, Building, Completing, Completed));
    }

    @Test
    void testReportCompleting() {
        runBuild(reportCompleting(), Passed);
        assertThat(statusReporter.results()).isEqualTo(Arrays.asList(Passed, Passed));
    }

    @Test
    void resultShouldBeFailedWhenCommandFailed() {
        runBuild(fail("force build failure"), Failed);
        assertThat(statusReporter.singleResult()).isEqualTo(Failed);
    }

    @Test
    void forceBuildFailWithMessage() {
        runBuild(fail("force failure"), Failed);
        assertThat(console.output()).isEqualTo("force failure");
    }

    @Test
    void composeRunAllSubCommands() {
        runBuild(compose(echo("hello"), echo("world")), Passed);
        assertThat(console.asList()).isEqualTo(Arrays.asList("hello", "world"));
    }

    @Test
    void shouldNotRunCommandWithRunIfFailedIfBuildIsPassing() {
        runBuild(compose(
                echo("on pass"),
                echo("on failure").runIf("failed")), Passed);
        assertThat(console.asList()).isEqualTo(Collections.singletonList("on pass"));
    }

    @Test
    void shouldRunCommandWithRunIfFailedIfBuildIsFailed() {
        runBuild(compose(
                fail("force failure"),
                echo("on failure").runIf("failed")), Failed);
        assertThat(console.lastLine()).isEqualTo("on failure");
    }

    @Test
    void shouldRunCommandWithRunIfAnyRegardlessOfBuildResult() {
        runBuild(compose(
                echo("foo"),
                echo("on passing").runIf("any"),
                fail("force failure"),
                echo("on failure").runIf("any")), Failed);
        assertThat(console.asList()).isEqualTo(Arrays.asList("foo", "on passing", "force failure", "on failure"));
    }


    @Test
    void echoWithBuildVariableSubstitution() {
        runBuild(echo("hello ${test.foo}"), Passed);
        assertThat(console.lastLine()).isEqualTo("hello ${test.foo}");
        buildVariables.put("test.foo", "world");
        runBuild(echo("hello ${test.foo}"), Passed);
        assertThat(console.lastLine()).isEqualTo("hello world");
    }
}
