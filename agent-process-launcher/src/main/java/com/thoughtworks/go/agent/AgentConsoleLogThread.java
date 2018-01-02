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

package com.thoughtworks.go.agent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AgentConsoleLogThread extends Thread {
    private final InputStream inputStream;
    private final AgentOutputAppender agentOutputAppender;

    private volatile boolean keepRunning;
    private volatile boolean flushedAfterStop;

    public AgentConsoleLogThread(InputStream inputStream, AgentOutputAppender agentOutputAppender) {
        this.inputStream = inputStream;
        this.agentOutputAppender = agentOutputAppender;

        setDaemon(true);
        keepRunning = true;
        flushedAfterStop = false;
    }

    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            pipeLogs(reader);
        } finally {
            agentOutputAppender.close();
        }
    }

    private void pipeLogs(BufferedReader reader) {
        while (true) {
            if (!keepRunning && flushedAfterStop) {
                break;
            }
            try {
                logConsoleError(reader);
                Thread.sleep(100);
            } catch (Exception e) {
                System.err.println("Error occured while capturing console output from agent process: ");
                e.printStackTrace();
                break;
            }
        }
    }

    private void logConsoleError(final BufferedReader reader) {
        flushedAfterStop = !keepRunning;
        try {
            reader.lines().forEach(agentOutputAppender::write);
        } catch (Exception ignore) {
            // we failed to log, mostly because we're shutting down
        }
    }

    void stopAndJoin() throws InterruptedException {
        keepRunning = false;
        Thread.sleep(1000); // give some time to allow any logs to be flushed out.
        agentOutputAppender.close();
        join();
    }
}
