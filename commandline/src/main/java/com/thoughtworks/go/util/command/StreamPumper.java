/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Originally from Maven projects. Modifications copyright Thoughtworks and respective GoCD contributors.
 */
package com.thoughtworks.go.util.command;

import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemTimeClock;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;

import static java.time.Duration.between;

public class StreamPumper implements Runnable {

    private final Reader in;
    private final StreamConsumer streamConsumer;
    private final String prefix;
    private final Clock clock;

    private Instant lastHeard;
    private boolean completed;

    private StreamPumper(InputStream in, StreamConsumer streamConsumer, String prefix, Charset encoding, Clock clock) {
        this.streamConsumer = streamConsumer;
        this.prefix = prefix;
        this.clock = clock;
        this.lastHeard = clock.currentTime();
        this.in = new InputStreamReader(in, encoding);
    }

    @Override
    public void run() {
        try (LineIterator lineIterator = IOUtils.lineIterator(in)) {
            while (lineIterator.hasNext()) {
                consumeLine(lineIterator.next());
            }
        } catch (Exception ignore) {
        } finally {
            completed = true;
        }
    }

    private void consumeLine(String line) {
        lastHeard = clock.currentTime();
        if (streamConsumer != null) {
            if (prefix == null || prefix.isBlank()) {
                streamConsumer.consumeLine(line);
            } else {
                streamConsumer.consumeLine(prefix + line);
            }
        }
    }

    public void readToEnd() {
        while (!completed && !Thread.currentThread().isInterrupted()) {
            try {
                clock.sleepForMillis(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static StreamPumper pump(InputStream stream, StreamConsumer streamConsumer, String prefix, Charset encoding) {
        return pump(stream, streamConsumer, prefix, encoding, new SystemTimeClock());
    }

    @VisibleForTesting
    static StreamPumper pump(InputStream stream, StreamConsumer streamConsumer, String prefix, Charset encoding, Clock clock) {
        StreamPumper pumper = new StreamPumper(stream, streamConsumer, prefix, encoding, clock);
        Thread thread = Thread.ofVirtual().unstarted(pumper);
        thread.setName("StreamPumper" + thread.getName());
        thread.start();
        return pumper;
    }

    @TestOnly
    boolean completedInLessThan(Duration duration) {
        if (completed) {
            return false;
        }
        return duration.compareTo(between(lastHeard, clock.currentTime())) < 0;
    }

    public Instant getLastHeard() {
        return lastHeard;
    }
}
