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

import com.jezhumble.javasysmon.JavaSysMon;
import com.jezhumble.javasysmon.ProcessVisitor;
import com.jezhumble.javasysmon.OsProcess;
import com.jezhumble.javasysmon.ProcessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @understands handling subprocesses
 */
public class SubprocessLogger implements Runnable {
    private JavaSysMon sysMon;
    private static final Logger LOGGER = LoggerFactory.getLogger(SubprocessLogger.class);
    private Thread exitHook;
    private String warnMessage = "Logged all subprocesses.";

    public SubprocessLogger() {
        this(new JavaSysMon());
    }

    SubprocessLogger(JavaSysMon sysMon) {
        this.sysMon = sysMon;
    }

    public void registerAsExitHook(String warnMessage) {
        this.warnMessage = warnMessage;
        Runtime.getRuntime().addShutdownHook(exitHook());
    }

    Thread exitHook() {
        if (exitHook == null)
            exitHook = new Thread(this);
        return exitHook;
    }

    private void logSubprocess() {
        final StringBuffer processDetails = new StringBuffer();
        sysMon.visitProcessTree(sysMon.currentPid(), new ProcessVisitor() {
            public boolean visit(OsProcess process, int level) {
                if (level == 1) {
                    ProcessInfo processInfo = process.processInfo();
                    processDetails.append(String.format("\n\tPID: %s\tname: %s\towner: %s\tcommand: %s", processInfo.getPid(), processInfo.getName(), processInfo.getOwner(), processInfo.getCommand()));
                }
                return false;
            }
        });
        if (!processDetails.toString().isEmpty()) {
            LOGGER.warn("{}{}", warnMessage, processDetails);
        }
    }

    public void run() {
        logSubprocess();
    }
}
