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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.IO;
import com.thoughtworks.go.util.command.StreamConsumer;
import com.thoughtworks.go.util.command.StreamPumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessWrapper {

    private final Process process;
    private StreamPumper processOutputStream;
    private StreamPumper processErrorStream;
    private PrintWriter processInputStream;
    private long startTime;
    private String processTag;
    private String command;
    private ConsoleOutputStreamConsumer consumer;
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessWrapper.class);

    ProcessWrapper(Process process, String processTag, String command, ConsoleOutputStreamConsumer consumer, String encoding, String errorPrefix) {
        this.process = process;
        this.processTag = processTag;
        this.command = command;
        this.consumer = consumer;
        this.startTime = System.currentTimeMillis();
        this.processOutputStream = StreamPumper.pump(process.getInputStream(), new OutputConsumer(), "", encoding);
        this.processErrorStream = StreamPumper.pump(process.getErrorStream(), new ErrorConsumer(), errorPrefix, encoding);
        this.processInputStream = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
    }

    public int waitForExit() {
        int returnValue = -1;
        try {
            returnValue = process.waitFor();
            processOutputStream.readToEnd();
            processErrorStream.readToEnd();
        } catch (InterruptedException ignored) {
            LOGGER.warn(ignored.getMessage(), ignored);
        } finally {
            close();
        }
        return returnValue;
    }

    public void typeInputToConsole(List<String> inputs) {
        for (String input : inputs) {
            processInputStream.println(input);
            processInputStream.flush();
        }
        processInputStream.close();
    }


    private long lastHeardTime() {
        if (processErrorStream == null & processOutputStream == null) {
            return System.currentTimeMillis();
        }
        if (processErrorStream == null) {
            return processOutputStream.getLastHeard();
        }
        if (processOutputStream == null) {
            return processErrorStream.getLastHeard();
        }
        return processOutputStream.getLastHeard() < processErrorStream.getLastHeard() ? processOutputStream.getLastHeard() : processErrorStream.getLastHeard();
    }


    public void closeOutputStream() throws IOException {
        process.getOutputStream().close();
    }

    public void close() {
        IO.close(process);
        ProcessManager.getInstance().processKilled(process);
    }

    public boolean isRunning() {
        try {
            process.exitValue();
        } catch (IllegalThreadStateException e) {
            return true;
        }
        return false;
    }

    private class OutputConsumer implements StreamConsumer {
        public void consumeLine(String line) {
            consumer.stdOutput(line);
        }
    }

    private class ErrorConsumer implements StreamConsumer {
        public void consumeLine(String line) {
            consumer.errOutput(line);
        }
    }

    public String getStartTimeForDisplay() {
        return new SimpleDateFormat("dd/MM/yy - H:mm:ss:S").format(new Date(startTime));
    }

    public String getProcessTag() {
        return processTag;
    }

    public long getIdleTime() {
        return System.currentTimeMillis() - lastHeardTime();
    }

    public String getCommand() {
        return command;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProcessWrapper that = (ProcessWrapper) o;

        if (process != null ? !process.equals(that.process) : that.process != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return process != null ? process.hashCode() : 0;
    }
}
