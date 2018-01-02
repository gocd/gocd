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

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static com.thoughtworks.go.domain.BuildCommand.*;
import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.domain.JobState.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class BuildSessionTest extends BuildSessionBasedTestCase {
    @Test
    public void resolveRelativeDir() throws IOException {
        BuildSession buildSession = newBuildSession();
        assertThat(buildSession.resolveRelativeDir("foo"), is(new File(sandbox, "foo")));
        assertThat(buildSession.resolveRelativeDir("foo", "bar"), is(new File(sandbox, "foo/bar")));
        assertThat(buildSession.resolveRelativeDir("", "bar"), is(new File(sandbox, "bar")));
    }

    @Test
    public void echoCommandAppendContentToConsole() {
        runBuild(echo("o1o2"), Passed);
        assertThat(console.asList(), is(Collections.singletonList("o1o2")));
    }

    @Test
    public void testReportCurrentStatus() {
        runBuild(compose(
                reportCurrentStatus(Preparing),
                reportCurrentStatus(Building),
                reportCurrentStatus(Completing)), Passed);
        assertThat(statusReporter.status(), is(Arrays.asList(Preparing, Building, Completing, Completed)));
    }

    @Test
    public void testReportCompleting() {
        runBuild(reportCompleting(), Passed);
        assertThat(statusReporter.results(), is(Arrays.asList(Passed, Passed)));
    }

    @Test
    public void resultShouldBeFailedWhenCommandFailed() {
        runBuild(fail("force build failure"), Failed);
        assertThat(statusReporter.singleResult(), is(Failed));
    }

    @Test
    public void forceBuildFailWithMessage() {
        runBuild(fail("force failure"), Failed);
        assertThat(console.output(), is("force failure"));
    }

    @Test
    public void composeRunAllSubCommands() {
        runBuild(compose(echo("hello"), echo("world")), Passed);
        assertThat(console.asList(), is(Arrays.asList("hello", "world")));
    }

    @Test
    public void shouldNotRunCommandWithRunIfFailedIfBuildIsPassing() {
        runBuild(compose(
                echo("on pass"),
                echo("on failure").runIf("failed")), Passed);
        assertThat(console.asList(), is(Collections.singletonList("on pass")));
    }

    @Test
    public void shouldRunCommandWithRunIfFailedIfBuildIsFailed() {
        runBuild(compose(
                fail("force failure"),
                echo("on failure").runIf("failed")), Failed);
        assertThat(console.lastLine(), is("on failure"));
    }

    @Test
    public void shouldRunCommandWithRunIfAnyRegardlessOfBuildResult() {
        runBuild(compose(
                echo("foo"),
                echo("on passing").runIf("any"),
                fail("force failure"),
                echo("on failure").runIf("any")), Failed);
        assertThat(console.asList(), is(Arrays.asList("foo", "on passing", "force failure", "on failure")));
    }


    @Test
    public void echoWithBuildVariableSubstitution() {
        runBuild(echo("hello ${test.foo}"), Passed);
        assertThat(console.lastLine(), is("hello ${test.foo}"));
        buildVariables.put("test.foo", "world");
        runBuild(echo("hello ${test.foo}"), Passed);
        assertThat(console.lastLine(), is("hello world"));
    }
}