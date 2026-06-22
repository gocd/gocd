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
package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.TaggedStreamConsumer;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class ConsoleOutputTransmitter implements TaggedStreamConsumer, Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleOutputTransmitter.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int MAX_BUFFER_SIZE = 10 * 1024;

    private final CircularFifoQueue<String> buffer = new CircularFifoQueue<>(MAX_BUFFER_SIZE); // maximum 10k lines
    private final ConsoleAppender consoleAppender;
    private final ScheduledThreadPoolExecutor executor;

    public ConsoleOutputTransmitter(ConsoleAppender consoleAppender) {
        this(consoleAppender, new SystemEnvironment().getConsolePublishIntervalSeconds(), TimeUnit.SECONDS, new ScheduledThreadPoolExecutor(1));
    }

    ConsoleOutputTransmitter(ConsoleAppender consoleAppender, long consolePublishInterval, TimeUnit consumePublishIntervalUnit, ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
        this.consoleAppender = consoleAppender;
        this.executor = scheduledThreadPoolExecutor;
        executor.scheduleAtFixedRate(this, 0L, consolePublishInterval, consumePublishIntervalUnit);
    }

    @Override
    public void consumeLine(@NotNull String line) {
        taggedConsumeLine(NOTICE, line);
    }

    @Override
    public void taggedConsumeLine(@NotNull String tag, @NotNull String line) {
        String timestamp = FORMATTER.format(LocalTime.now());

        int approximateLength = tag.length() + timestamp.length() + line.length();
        StringBuilder logLineBuilder = new StringBuilder(approximateLength);

        line.lines().forEach(l -> logLineBuilder
            .append(tag)
            .append('|')
            .append(timestamp)
            .append(' ')
            .append(l)
            .append('\n'));

        logLineBuilder.setLength(logLineBuilder.length() - 1); // remove trailing new line from above
        String logLine = logLineBuilder.toString();
        synchronized (buffer) {
            buffer.add(logLine);
        }
    }

    @Override
    public void run() {
        try {
            flushToServer();
        } catch (Throwable e) {
            LOGGER.warn("Could not send console output to server", e);
        }
    }

    public void flushToServer() {
        if (buffer.isEmpty()) {
            return;
        }

        List<String> toFlush;
        synchronized (buffer) {
            toFlush = new ArrayList<>(buffer);
            buffer.clear();
        }
        if (toFlush.isEmpty()) {
            return;
        }
        try {
            consoleAppender.append(toFlush.stream().collect(Collectors.joining("\n", "", "\n")));
        } catch (IOException e) {
            LOGGER.warn("Could not send console output to server; re-adding logs to buffer", e);
            synchronized (buffer) {
                toFlush.addAll(buffer);
                buffer.clear();
                buffer.addAll(toFlush);
            }
        }
    }

    @Override
    public void close() {
        flushToServer();
        executor.shutdown();
    }
}
