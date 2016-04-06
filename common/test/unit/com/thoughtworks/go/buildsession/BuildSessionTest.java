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
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Iterables.getLast;
import static com.thoughtworks.go.domain.BuildCommand.*;
import static com.thoughtworks.go.domain.JobResult.*;
import static com.thoughtworks.go.domain.JobState.*;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void cancelLongRunningBuild() throws InterruptedException {
        final BuildSession buildSession = newBuildSession();
        Thread buildingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                buildSession.build(compose(
                        execSleepScript(50),
                        echo("build done")));
            }
        });
        try {
            buildingThread.start();
            console.waitForContain("start sleeping", 5);
            assertTrue(buildInfo(), buildSession.cancel(30, TimeUnit.SECONDS));
            assertThat(buildInfo(), getLast(statusReporter.results()), is(Cancelled));
            assertThat(buildInfo(), console.output(), not(containsString("build done")));
        } finally {
            buildingThread.join();
        }
    }

    @Test
    public void cancelLongRunningTestCommand() throws InterruptedException {
        final BuildSession buildSession = newBuildSession();
        Thread buildingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                buildSession.build(compose(
                        echo("after sleep").setTest(execSleepScript(50))));
            }
        });
        try {
            buildingThread.start();
            console.waitForContain("start sleeping", 5);
            assertTrue(buildInfo(), buildSession.cancel(30, TimeUnit.SECONDS));
            assertThat(buildInfo(), getLast(statusReporter.results()), is(Cancelled));
            assertThat(buildInfo(), console.output(), not(containsString("after sleep")));
        } finally {
            buildingThread.join();
        }
    }

    @Test
    public void doubleCancelDoNothing() throws InterruptedException {
        final BuildSession buildSession = newBuildSession();
        Thread buildingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                buildSession.build(execSleepScript(50));
            }
        });
        Runnable cancel = new Runnable() {
            @Override
            public void run() {
                try {
                    buildSession.cancel(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw bomb(e);
                }
            }
        };
        Thread cancelThread1 = new Thread(cancel);
        Thread cancelThread2 = new Thread(cancel);

        try {
            buildingThread.start();
            console.waitForContain("start sleeping", 5);
            cancelThread1.start();
            cancelThread2.start();
            cancelThread1.join();
            cancelThread2.join();
            assertThat(buildInfo(), getLast(statusReporter.results()), is(Cancelled));
            assertThat(buildInfo(), console.output(), not(containsString("after sleep")));
        } finally {
            buildingThread.join();
        }
    }

    @Test
    public void cancelShouldProcessOnCancelCommandOfCommandThatIsRunning() throws InterruptedException {
        final BuildSession buildSession = newBuildSession();
        Thread buildingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                buildSession.build(compose(
                        compose(
                            execSleepScript(50).setOnCancel(echo("exec canceled")),
                            echo("after sleep"))
                                .setOnCancel(echo("inner oncancel"))
                ).setOnCancel(echo("outter oncancel")));
            }
        });

        try {
            buildingThread.start();
            console.waitForContain("start sleeping", 5);
            assertTrue(buildInfo(), buildSession.cancel(30, TimeUnit.SECONDS));
            assertThat(buildInfo(), getLast(statusReporter.results()), is(Cancelled));
            assertThat(buildInfo(), console.output(), not(containsString("after sleep")));
            assertThat(buildInfo(), console.output(), containsString("exec canceled"));
            assertThat(buildInfo(), console.output(), containsString("inner oncancel"));
            assertThat(buildInfo(), console.output(), containsString("outter oncancel"));
        } finally {
            buildingThread.join();
        }
    }
}