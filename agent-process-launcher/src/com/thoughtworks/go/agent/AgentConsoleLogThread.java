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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.agent.common.util.LoggingHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.thoughtworks.go.agent.common.util.LoggingHelper.CONSOLE_NDC;

public class AgentConsoleLogThread extends Thread {
    private static final Log LOG = LogFactory.getLog(AgentConsoleLogThread.class);
    private volatile InputStream consoleError;
    private volatile CONSOLE_NDC ndc;
    private volatile boolean keepRunning;
    private volatile boolean flushedAfterStop;
    private String consoleFileName;

    public AgentConsoleLogThread(InputStream consoleError, CONSOLE_NDC ndc, String consoleFileName) {
        this.setName(ndc + ": Logger" + this.getName());
        this.consoleError = consoleError;
        this.ndc = ndc;
        this.consoleFileName = consoleFileName;
        setDaemon(true);
        keepRunning = true;
        flushedAfterStop = false;
    }

    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(consoleError));
        Appender consoleFileAppender = LoggingHelper.createConsoleFileAppender(ndc, consoleFileName);
        Logger.getRootLogger().addAppender(consoleFileAppender);
        NDC.push(ndc.toString());
        while (true) {
            if (!keepRunning && flushedAfterStop) {
                break;
            }
            try {
                logConsoleError(reader);
                Thread.sleep(1000);
            } catch (Exception e) {
                LOG.error("Error occured while capturing console output from agent process: ", e);
                break;
            }
        }
        NDC.pop();
        Logger.getRootLogger().removeAppender(consoleFileAppender);
    }

    private void logConsoleError(final BufferedReader reader) throws IOException {
        flushedAfterStop = !keepRunning;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            LOG.error(line);
        }
    }

    void stopAndJoin() throws InterruptedException {
        keepRunning = false;
        join();
    }
}
