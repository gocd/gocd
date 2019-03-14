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

import com.jezhumble.javasysmon.JavaSysMon;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.javasysmon.wrapper.DefaultCurrentProcess;
import com.thoughtworks.go.utils.Timeout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Iterables.getLast;
import static com.thoughtworks.go.domain.BuildCommand.*;
import static com.thoughtworks.go.domain.JobResult.Cancelled;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.utils.Assertions.waitUntil;
import static org.assertj.core.api.Assertions.assertThat;

class BuildSessionCancelingTest extends BuildSessionBasedTestCase {

    @Test
    void cancelLongRunningBuild() throws InterruptedException {
        final BuildSession buildSession = newBuildSession();
        Thread buildingThread = new Thread(() -> buildSession.build(compose(
                execSleepScript(50),
                echo("build done"))));
        buildingThread.start();
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), true);
        assertThat(buildSession.cancel(30, TimeUnit.SECONDS)).as(buildInfo()).isTrue();
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), false);
        assertThat(getLast(statusReporter.results())).as(buildInfo()).isEqualTo(Cancelled);
        assertThat(console.output()).as(buildInfo()).doesNotContain("build done");
        buildingThread.join();
    }

    @Test
    void cancelLongRunningTestCommand() throws InterruptedException {
        final BuildSession buildSession = newBuildSession();
        Thread buildingThread = new Thread(() -> buildSession.build(compose(
                echo("after sleep").setTest(execSleepScript(50)))));
        buildingThread.start();
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), true);

        assertThat(buildSession.cancel(30, TimeUnit.SECONDS)).as(buildInfo()).isTrue();
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), false);
        assertThat(getLast(statusReporter.results())).as(buildInfo()).isEqualTo(Cancelled);
        assertThat(console.output()).as(buildInfo()).doesNotContain("after sleep");
        buildingThread.join();
    }

    @Test
    void doubleCancelDoNothing() throws InterruptedException {
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
        assertThat(getLast(statusReporter.results())).as(buildInfo()).isEqualTo(Cancelled);
        assertThat(console.output()).as(buildInfo()).doesNotContain("after sleep");
        buildingThread.join();
    }

    @Test
    void cancelShouldProcessOnCancelCommandOfCommandThatIsRunning() throws InterruptedException {
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
        assertThat(buildSession.cancel(30, TimeUnit.SECONDS)).as(buildInfo()).isTrue();
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), false);
        DefaultCurrentProcess currentProcess = new DefaultCurrentProcess();
        assertThat(currentProcess.immediateChildren()).isEmpty();

        assertThat(getLast(statusReporter.results())).as(buildInfo()).isEqualTo(Cancelled);
        assertThat(console.output()).as(buildInfo()).doesNotContain("after sleep");
        assertThat(console.output()).as(buildInfo()).contains("exec canceled");
        assertThat(console.output()).as(buildInfo()).contains("inner oncancel");
        assertThat(console.output()).as(buildInfo()).contains("outter oncancel");
        buildingThread.join();
    }


    @Test
    @DisabledOnOs(OS.WINDOWS)
    void cancelTaskShouldBeProcessedBeforeKillChildProcess() throws InterruptedException {
        final BuildSession buildSession = newBuildSession();
        final BuildCommand printSubProcessCount = exec("/bin/bash", "-c", "pgrep -P " + new DefaultCurrentProcess().currentPid() + " | wc -l");
        Thread buildingThread = new Thread(() -> buildSession.build(compose(
                compose(execSleepScript(50),
                        echo("after sleep"))
                        .setOnCancel(printSubProcessCount))));

        buildingThread.start();
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), true);
        assertThat(buildSession.cancel(30, TimeUnit.SECONDS)).as(buildInfo()).isTrue();
        waitUntilSubProcessExists(execSleepScriptProcessCommand(), false);
        assertThat(Integer.parseInt(console.lastLine().trim())).isGreaterThan(0);
        buildingThread.join();
    }


    private void waitUntilSubProcessExists(final String processName, final boolean expectExist) {
        try {
            waitUntil(Timeout.FIVE_SECONDS, () -> subProcessNames().contains(processName) == expectExist, 250);
        } catch (RuntimeException e) {
            throw new RuntimeException("timeout waiting for subprocess " + (expectExist ? "exists" : "not exists") + ", current sub processes are: " + subProcessNames());
        }
    }

    private List<String> subProcessNames() {
        JavaSysMon javaSysMon = new JavaSysMon();
        final List<String> names = new ArrayList<>();
        final int currentPid = javaSysMon.currentPid();
        javaSysMon.visitProcessTree(currentPid, (osProcess, i) -> {
            if (osProcess.processInfo().getPid() != currentPid) {
                names.add(osProcess.processInfo().getName());
            }
            return false;
        });
        return names;
    }
}
