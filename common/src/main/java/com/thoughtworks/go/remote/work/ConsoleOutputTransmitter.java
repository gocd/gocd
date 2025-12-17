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

import static java.lang.String.format;

public final class ConsoleOutputTransmitter implements TaggedStreamConsumer, Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleOutputTransmitter.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final CircularFifoQueue<String> buffer = new CircularFifoQueue<>(10 * 1024); // maximum 10k lines
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
    public void consumeLine(String line) {
        taggedConsumeLine(null, line);
    }

    @Override
    public void taggedConsumeLine(String tag, String line) {
        if (tag == null) {
            tag = "  ";
        }
        String taggedDate = format("%s|%s", tag, FORMATTER.format(LocalTime.now()));
        String logLine = format("%s %s", taggedDate, line).replace("\n", "\n" + taggedDate + " ");
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
        try {
            consoleAppender.append(toFlush.stream().collect(Collectors.joining("\n", "", "\n")));
        } catch (IOException e) {
            LOGGER.warn("Could not send console output to server", e);
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
