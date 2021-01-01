/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.util.command;

import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemTimeClock;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;

public class StreamPumper implements Runnable {

    private Reader in;

    private boolean completed;
    private final StreamConsumer streamConsumer;
    private final String prefix;
    private long lastHeard;
    private final Clock clock;

    private StreamPumper(InputStream in, StreamConsumer streamConsumer, String prefix, String encoding) {
        this(in, streamConsumer, prefix, encoding, new SystemTimeClock());
    }

    StreamPumper(InputStream in, StreamConsumer streamConsumer, String prefix, String encoding, Clock clock) {
        this.streamConsumer = streamConsumer;
        this.prefix = prefix;
        this.clock = clock;
        this.lastHeard = System.currentTimeMillis();
        try {
            this.in = new InputStreamReader(in, encoding);
        } catch (UnsupportedEncodingException e) {
            bomb(format("Unable to use [%s] to decode stream.", encoding));
        }
    }

    @Override
    public void run() {
        try (LineIterator lineIterator = IOUtils.lineIterator(in)) {
            while (lineIterator.hasNext()) {
                consumeLine(lineIterator.nextLine());
            }
        } catch (Exception ignore) {
        } finally {
            completed = true;
        }
    }

    private void consumeLine(String line) {
        lastHeard = System.currentTimeMillis();
        if (streamConsumer != null) {
            if (StringUtils.isBlank(prefix)) {
                streamConsumer.consumeLine(line);
            } else {
                streamConsumer.consumeLine(prefix + line);
            }
        }
    }


    public void readToEnd() {
        while (!completed) {
            try {
                clock.sleepForMillis(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static StreamPumper pump(InputStream stream, StreamConsumer streamConsumer, String prefix, String encoding) {
        StreamPumper pumper = new StreamPumper(stream, streamConsumer, prefix, encoding);
        new Thread(pumper).start();
        return pumper;
    }

    private Long timeSinceLastLine(TimeUnit unit) {
        long now = clock.currentTimeMillis();
        return unit.convert(now - lastHeard, TimeUnit.MILLISECONDS);
    }

    public boolean didTimeout(long duration, TimeUnit unit) {
        if (completed) { return false; }
        return timeSinceLastLine(unit) > duration;
    }

    public long getLastHeard() {
        return lastHeard;
    }
}
