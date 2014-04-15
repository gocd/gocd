/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.remote.work;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.StreamConsumer;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.log4j.Logger;

public final class ConsoleOutputTransmitter implements StreamConsumer, Runnable {
    private static final Logger LOGGER = Logger.getLogger(ConsoleOutputTransmitter.class);

    private volatile boolean isStopped = false;
    private CircularFifoBuffer buffer = new CircularFifoBuffer(10 * 1024); // maximum 10k lines
    private Integer sleepInSeconds;
    private final ConsoleAppender consoleAppender;


    public ConsoleOutputTransmitter(ConsoleAppender consoleAppender) {
        this.consoleAppender = consoleAppender;
        sleepInSeconds = new SystemEnvironment().getConsolePublishInterval();
        new Thread(this).start();
    }


    public void consumeLine(String line) {
        synchronized (buffer) {
            buffer.add(line);
        }
    }

    public void run() {
        while (!isStopped) {
            try {
                Thread.sleep(sleepInSeconds * 1000);
            } catch (InterruptedException ignore) {
            }
            flushToServer();
        }
    }

    public void flushToServer() {
        if (buffer.isEmpty()) { return; }

        List sent = new ArrayList();
        try {
            synchronized (buffer) {
                while (!buffer.isEmpty()) {
                    sent.add(buffer.remove());
                }
            }
            StringBuilder result = new StringBuilder();
            for (Object string : sent) {
                result.append(string);
                result.append("\n");
            }
            consoleAppender.append(result.toString());
        } catch (IOException e) {
            LOGGER.warn("Could not send console output to server", e);
            //recreate buffer
            synchronized (buffer) {
                sent.addAll(buffer);
                buffer = new CircularFifoBuffer(10 * 1024);
                buffer.addAll(sent);
            }
        }
    }

    public void stop() {
        isStopped = true;
        flushToServer();
    }

}
