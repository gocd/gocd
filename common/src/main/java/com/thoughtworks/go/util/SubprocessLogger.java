/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.process.CurrentProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @understands handling subprocesses
 */
public class SubprocessLogger implements Runnable {
    private CurrentProcess currentProcess;
    private static final Logger LOGGER = LoggerFactory.getLogger(SubprocessLogger.class);
    private Thread exitHook;
    private String warnMessage = "Logged all subprocesses.";

    public SubprocessLogger() {
        this(new CurrentProcess());
    }

    SubprocessLogger(CurrentProcess currentProcess) {
        this.currentProcess = currentProcess;
    }

    public void registerAsExitHook(String warnMessage) {
        this.warnMessage = warnMessage;
        Runtime.getRuntime().addShutdownHook(exitHook());
    }

    Thread exitHook() {
        if (exitHook == null) {
            exitHook = new Thread(this);
        }
        return exitHook;
    }

    private void logSubprocess() {
        final StringBuffer processDetails = new StringBuffer();

        currentProcess.immediateChildren().forEach(processInfo ->
                processDetails.append(processInfo.toString()).append("\n")
        );

        if (!processDetails.toString().isEmpty()) {
            LOGGER.warn("{}\n{}", warnMessage, processDetails);
        }
    }

    @Override
    public void run() {
        logSubprocess();
    }
}
