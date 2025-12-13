/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.ErrorConsumer;
import com.thoughtworks.go.util.command.OutputConsumer;
import com.thoughtworks.go.util.command.StreamPumper;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ProcessWrapper {

    private final Process process;
    private final StreamPumper processOutputStream;
    private final StreamPumper processErrorStream;
    private final PrintWriter processInputStream;
    private final long startTime;
    private final ProcessTag processTag;
    private final String command;

    ProcessWrapper(Process process, ProcessTag processTag, String command, ConsoleOutputStreamConsumer consumer, Charset encoding, String errorPrefix) {
        this.process = process;
        this.processTag = processTag;
        this.command = command;
        this.startTime = System.currentTimeMillis();
        this.processOutputStream = StreamPumper.pump(process.getInputStream(), new OutputConsumer(consumer), "", encoding);
        this.processErrorStream = StreamPumper.pump(process.getErrorStream(), new ErrorConsumer(consumer), errorPrefix, encoding);
        this.processInputStream = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
    }

    @SuppressWarnings("try")
    public int waitForExit() {
        int returnValue = -1;
        try (InputStream ignored = process.getInputStream();
             InputStream ignored1 = process.getErrorStream();
             OutputStream ignored2 = process.getOutputStream()) {

            returnValue = process.waitFor();
            processOutputStream.readToEnd();
            processErrorStream.readToEnd();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
        } finally {
            process.destroy();
            ProcessManager.getInstance().processKilled(process);
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
        if (processErrorStream == null && processOutputStream == null) {
            return System.currentTimeMillis();
        }
        if (processErrorStream == null) {
            return processOutputStream.getLastHeard();
        }
        if (processOutputStream == null) {
            return processErrorStream.getLastHeard();
        }
        return Math.min(processOutputStream.getLastHeard(), processErrorStream.getLastHeard());
    }

    public void closeOutputStream() throws IOException {
        process.getOutputStream().close();
    }

    public boolean isRunning() {
        try {
            process.exitValue();
        } catch (IllegalThreadStateException e) {
            return true;
        }
        return false;
    }

    public String getStartTimeForDisplay() {
        return new SimpleDateFormat("dd/MM/yy - H:mm:ss:S").format(new Date(startTime));
    }

    public ProcessTag getProcessTag() {
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

        return Objects.equals(process, that.process);
    }

    @Override
    public int hashCode() {
        return process != null ? process.hashCode() : 0;
    }

}
