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

package com.thoughtworks.go.domain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Created by marqueslee on 3/11/17.
 */
public class ConsoleStreamer {
    private Path path;
    private long start;
    private long count = 0L;

    public ConsoleStreamer(Path path, long start) {
        this.path = path;
        this.start = (start < 0L) ? 0L : start;
    }

    public void stream(Consumer<String> action) throws IOException {
        try (Stream<String> raw = Files.lines(path, StandardCharsets.UTF_8); Stream<String> lines = raw.skip(start)) {
            Iterator<String> it = lines.iterator();
            while (it.hasNext()) {
                action.accept(it.next());
                count++;
            }
        }
    }

    public long processedLineCount() {
        return count;
    }
}
