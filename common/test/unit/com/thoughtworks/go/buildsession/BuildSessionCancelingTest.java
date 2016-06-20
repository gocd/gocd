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
import com.jezhumble.javasysmon.JavaSysMon;
import com.jezhumble.javasysmon.OsProcess;
import com.jezhumble.javasysmon.ProcessVisitor;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.utils.Assertions;
import com.thoughtworks.go.utils.Timeout;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Iterables.getLast;
import static com.thoughtworks.go.domain.BuildCommand.*;
import static com.thoughtworks.go.domain.JobResult.Cancelled;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.utils.Assertions.waitUntil;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertTrue;

@RunWith(JunitExtRunner.class)
public class BuildSessionCancelingTest extends BuildSessionBasedTestCase {

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
        buildingThread.start();
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), true);
        assertTrue(buildInfo(), buildSession.cancel(30, TimeUnit.SECONDS));
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), false);
        assertThat(buildInfo(), getLast(statusReporter.results()), is(Cancelled));
        assertThat(buildInfo(), console.output(), not(containsString("build done")));
        buildingThread.join();
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
        buildingThread.start();
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), true);

        assertTrue(buildInfo(), buildSession.cancel(30, TimeUnit.SECONDS));
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), false);
        assertThat(buildInfo(), getLast(statusReporter.results()), is(Cancelled));
        assertThat(buildInfo(), console.output(), not(containsString("after sleep")));
        buildingThread.join();
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

        buildingThread.start();
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), true);
        cancelThread1.start();
        cancelThread2.start();
        cancelThread1.join();
        cancelThread2.join();
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), false);
        assertThat(buildInfo(), getLast(statusReporter.results()), is(Cancelled));
        assertThat(buildInfo(), console.output(), not(containsString("after sleep")));
        buildingThread.join();
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

        buildingThread.start();
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), true);
        assertTrue(buildInfo(), buildSession.cancel(30, TimeUnit.SECONDS));
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), false);
        JavaSysMon javaSysMon = new JavaSysMon();
        final boolean[] exists = {false};
        javaSysMon.visitProcessTree(javaSysMon.currentPid(), new ProcessVisitor() {
            @Override
            public boolean visit(OsProcess osProcess, int i) {
                String command = osProcess.processInfo().getName();
                if (execSleepScriptProcessCommand().equals(command)) {
                    exists[0] = true;
                }
                return false;
            }
        });
        assertThat(exists[0], is(false));

        assertThat(buildInfo(), getLast(statusReporter.results()), is(Cancelled));
        assertThat(buildInfo(), console.output(), not(containsString("after sleep")));
        assertThat(buildInfo(), console.output(), containsString("exec canceled"));
        assertThat(buildInfo(), console.output(), containsString("inner oncancel"));
        assertThat(buildInfo(), console.output(), containsString("outter oncancel"));
        buildingThread.join();
    }


    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void cancelTaskShouldBeProcessedBeforeKillChildProcess() throws InterruptedException {
        final BuildSession buildSession = newBuildSession();
        final BuildCommand printSubProcessCount = exec("/bin/bash", "-c", "pgrep -P " + new JavaSysMon().currentPid() + " | wc -l");
        Thread buildingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                buildSession.build(compose(
                        compose(execSleepScript(50),
                                echo("after sleep"))
                                .setOnCancel(printSubProcessCount)));
            }
        });

        buildingThread.start();
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), true);
        assertTrue(buildInfo(), buildSession.cancel(30, TimeUnit.SECONDS));
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), false);
        assertThat(Integer.parseInt(console.lastLine().trim()), greaterThan(0));
        buildingThread.join();
    }


    private void waitUntilSubProcessExists(final String processName, final boolean expectExist) {
        try {
            waitUntil(Timeout.FIVE_SECONDS, new Assertions.Predicate() {
                @Override
                public boolean call() throws Exception {
                    return subProcessNames().contains(processName) == expectExist;
                }
            }, 250);
        } catch (RuntimeException e) {
            throw new RuntimeException("timeout waiting for subprocess " + (expectExist ? "exists" : "not exists") + ", current sub processes are: " + subProcessNames());
        }
    }

    private List<String> subProcessNames() {
        JavaSysMon javaSysMon = new JavaSysMon();
        final List<String> names = new ArrayList<>();
        final int currentPid = javaSysMon.currentPid();
        javaSysMon.visitProcessTree(currentPid, new ProcessVisitor() {
            @Override
            public boolean visit(OsProcess osProcess, int i) {
                if(osProcess.processInfo().getPid() != currentPid) {
                    names.add(osProcess.processInfo().getName());
                }
                return false;
            }
        });
        return names;
    }
}