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
package com.thoughtworks.go.domain;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class ConsoleStreamerTest {
    @Test
    public void streamProcessesAllLines() throws Exception {
        String[] expected = new String[]{
                "First line",
                "Second line",
                "Third line"
        };
        final ArrayList<String> actual = new ArrayList<>();

        try (ConsoleStreamer console = new ConsoleStreamer(makeConsoleFile(expected).toPath(), 0L)) {
            console.stream(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    actual.add(s);
                }
            });
            assertArrayEquals(expected, actual.toArray());
            assertEquals(3L, console.totalLinesConsumed());
        }
    }

    @Test
    public void streamSkipsToStartLine() throws Exception {
        final ArrayList<String> actual = new ArrayList<>();

        try (ConsoleStreamer console = new ConsoleStreamer(makeConsoleFile("first", "second", "third", "fourth").toPath(), 2L)) {
            console.stream(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    actual.add(s);
                }
            });
            assertArrayEquals(new String[]{"third", "fourth"}, actual.toArray());
            assertEquals(2L, console.totalLinesConsumed());
        }
    }

    @Test
    public void streamAssumesNegativeStartLineIsZero() throws Exception {
        String[] expected = new String[]{
                "First line",
                "Second line",
                "Third line"
        };
        final ArrayList<String> actual = new ArrayList<>();

        try (ConsoleStreamer console = new ConsoleStreamer(makeConsoleFile(expected).toPath(), -1L)) {
            console.stream(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    actual.add(s);
                }
            });
            assertArrayEquals(expected, actual.toArray());
            assertEquals(3L, console.totalLinesConsumed());
        }
    }

    @Test
    public void processesNothingWhenStartLineIsBeyondEOF() throws Exception {
        final ArrayList<String> actual = new ArrayList<>();

        try (ConsoleStreamer console = new ConsoleStreamer(makeConsoleFile("first", "second").toPath(), 5L)) {
            console.stream(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    actual.add(s);
                }
            });
            assertTrue(actual.isEmpty());
            assertEquals(0L, console.totalLinesConsumed());
        }
    }

    private File makeConsoleFile(String... message) throws IOException {
        File console = File.createTempFile("console", ".log");
        console.deleteOnExit();

        Files.write(console.toPath(), StringUtils.join(message, "\n").getBytes());
        return console;
    }

}
