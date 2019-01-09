/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util;

import ch.qos.logback.classic.Level;
import com.jezhumble.javasysmon.ProcessInfo;
import com.thoughtworks.go.javasysmon.wrapper.DefaultCurrentProcess;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;

import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SubprocessLoggerTest {
    private SubprocessLogger logger;

    @After
    public void tearDown() {
        Runtime.getRuntime().removeShutdownHook(logger.exitHook());
    }

    @Test
    public void shouldNotLogAnythingWhenNoChildProcessesFound() {
        DefaultCurrentProcess currentProcess = mock(DefaultCurrentProcess.class);
        logger = new SubprocessLogger(currentProcess);
        try (LogFixture log = logFixtureFor(SubprocessLogger.class, Level.ALL)) {
            logger.run();
            String result;
            synchronized (log) {
                result = log.getLog();
            }
            assertThat(result, is(""));
        }
    }

    @Test
    public void shouldLogDefaultMessageWhenNoMessageGiven() {
        logger = new SubprocessLogger(stubSysMon());
        String allLogs;
        try (LogFixture log = logFixtureFor(SubprocessLogger.class, Level.ALL)) {
            logger.run();
            String result;
            synchronized (log) {
                result = log.getLog();
            }
            allLogs = result;
        }
        assertThat(allLogs, containsString("Logged all subprocesses."));
    }

    @Test
    public void shouldLogAllTheRunningChildProcesses() {
        logger = new SubprocessLogger(stubSysMon());
        String allLogs;
        try (LogFixture log = logFixtureFor(SubprocessLogger.class, Level.ALL)) {
            logger.registerAsExitHook("foo bar baz");
            logger.run();
            String result;
            synchronized (log) {
                result = log.getLog();
            }
            allLogs = result;
        }
        Assertions.assertThat(allLogs).isEqualToNormalizingNewlines("WARN foo bar baz\n" +
                "  101 name-1       100 owner-1       0Mb    0Mb 00:00:00 command-1              \n" +
                "  103 name-1a      100 owner-1       0Mb    0Mb 00:00:00 command-1a             \n" +
                "\n");
        assertThat(allLogs, not(containsString("102")));
    }

    private DefaultCurrentProcess stubSysMon() {
        DefaultCurrentProcess currentProcess = mock(DefaultCurrentProcess.class);
        when(currentProcess.immediateChildren()).thenReturn(Arrays.asList(
                new ProcessInfo(101, 100, "command-1", "name-1", "owner-1", 100, 200, 400, 800),
                new ProcessInfo(103, 100, "command-1a", "name-1a", "owner-1", 160, 260, 460, 860)
        ));
        return currentProcess;
    }

    @Test
    public void shouldRegisterItselfAsExitHook() {
        logger = new SubprocessLogger(new DefaultCurrentProcess());
        logger.registerAsExitHook("foo");
        try {
            Runtime.getRuntime().addShutdownHook(logger.exitHook());
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Hook previously registered"));
        } finally {
            Runtime.getRuntime().removeShutdownHook(logger.exitHook());
        }
    }
}
