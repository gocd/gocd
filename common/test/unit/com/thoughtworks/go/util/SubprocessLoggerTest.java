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

package com.thoughtworks.go.util;

import com.jezhumble.javasysmon.JavaSysMon;
import com.jezhumble.javasysmon.OsProcess;
import com.jezhumble.javasysmon.ProcessInfo;
import com.jezhumble.javasysmon.ProcessVisitor;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import org.apache.log4j.Level;
import org.junit.After;

import static org.junit.Assert.assertThat;

import org.junit.Test;

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
        JavaSysMon sysMon = mock(JavaSysMon.class);
        logger = new SubprocessLogger(sysMon);
        try (LogFixture log = new LogFixture(SubprocessLogger.class, Level.ALL)) {
            logger.run();
            assertThat(log.allLogs(), is(""));
        }
    }

    @Test
    public void shouldLogDefaultMessageWhenNoMessageGiven() {
        logger = new SubprocessLogger(stubSysMon());
        String allLogs;
        try (LogFixture log = new LogFixture(SubprocessLogger.class, Level.ALL)) {
            logger.run();
            allLogs = log.allLogs();
        }
        assertThat(allLogs, containsString("Logged all subprocesses."));
    }

    @Test
    public void shouldLogAllTheRunningChildProcesses() {
        logger = new SubprocessLogger(stubSysMon());
        String allLogs;
        try (LogFixture log = new LogFixture(SubprocessLogger.class, Level.ALL)) {
            logger.registerAsExitHook("foo bar baz");
            logger.run();
            allLogs = log.allLogs();
        }
        assertThat(allLogs, containsString("foo bar baz\n\tPID: 101\tname: name-1\towner: owner-1\tcommand: command-1\n\tPID: 103\tname: name-1a\towner: owner-1\tcommand: command-1a"));
        assertThat(allLogs, not(containsString("PID: 102")));
    }

    private JavaSysMon stubSysMon() {
        final OsProcess process1 = mock(OsProcess.class);
        when(process1.processInfo()).thenReturn(new ProcessInfo(101, 100, "command-1", "name-1", "owner-1", 100, 200, 400, 800));
        final OsProcess process1a = mock(OsProcess.class);
        when(process1a.processInfo()).thenReturn(new ProcessInfo(103, 100, "command-1a", "name-1a", "owner-1", 160, 260, 460, 860));
        final OsProcess process2 = mock(OsProcess.class);
        when(process2.processInfo()).thenReturn(new ProcessInfo(102, 101, "command-2", "name-2", "owner-1", 150, 250, 450, 850));
        JavaSysMon sysMon = new JavaSysMon() {
            @Override
            public void visitProcessTree(int pid, ProcessVisitor processVisitor) {
                processVisitor.visit(process2, 2);
                processVisitor.visit(process1, 1);
                processVisitor.visit(process1a, 1);
            }

            @Override
            public int currentPid() {
                return 100;
            }
        };
        return sysMon;
    }

    @Test
    public void shouldRegisterItselfAsExitHook() {
        logger = new SubprocessLogger(new JavaSysMon());
        logger.registerAsExitHook("foo");
        try {
            Runtime.getRuntime().addShutdownHook(logger.exitHook());
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Hook previously registered"));
        }
    }
}
