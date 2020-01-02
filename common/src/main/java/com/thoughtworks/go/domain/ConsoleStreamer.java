/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.util.SystemEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Encapsulates a stream of lines from a console log file while keeping track of the number of lines processed
 * as well as the starting line to read.
 */
public class ConsoleStreamer implements ConsoleConsumer {
    private Path path;
    private Stream stream;
    private Iterator iterator;

    private long start;
    private long count = 0L;

    public ConsoleStreamer(Path path, long start) {
        this.path = path;
        this.start = (start < 0L) ? 0L : start;
    }

    /**
     * Applies a lambda (or equivalent {@link Consumer}) to each line and increments the totalLinesConsumed() until
     * EOF. Multiple invocations to this method will continue from where it left off if new content was appended after
     * the last read.
     *
     * @param action the lambda to apply to each line
     * @return the number of lines streamed by this invocation
     * @throws IOException if the file does not exist or is otherwise not readable
     */
    @Override
    public long stream(Consumer<String> action) throws IOException {
        long linesStreamed = 0L;

        if (null == stream) stream = Files.lines(path, new SystemEnvironment().consoleLogCharsetAsCharset()).skip(start);
        if (null == iterator) iterator = stream.iterator();

        while (iterator.hasNext()) {
            action.accept((String) iterator.next());
            ++linesStreamed;
            ++count;
        }

        return linesStreamed;
    }

    @Override
    public void close() {
        if (null != stream) {
            stream.close();
        }

        stream = null;
        iterator = null;
    }

    @Override
    public long totalLinesConsumed() {
        return count;
    }
}
