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

public class ProcessOutputStreamConsumer<T extends StreamConsumer, T2 extends StreamConsumer> implements ConsoleOutputStreamConsumer<T, T2> {
    private T stdConsumer;
    private T2 errorConsumer;

    public ProcessOutputStreamConsumer(T stdConsumer, T2 errorConsumer) {
        this.errorConsumer = errorConsumer;
        this.stdConsumer = stdConsumer;
    }

    protected T getStdConsumer() {
        return stdConsumer;
    }

    protected T2 getErrorConsumer() {
        return errorConsumer;
    }

    public static InMemoryStreamConsumer inMemoryConsumer() {
        return new InMemoryStreamConsumer();
    }

    @Override
    public void stdOutput(String line) {
        taggedStdOutput(TaggedStreamConsumer.OUT, line);
    }

    @Override
    public void errOutput(String line) {
        taggedErrOutput(TaggedStreamConsumer.ERR, line);
    }

    @Override
    public void taggedStdOutput(String tag, String line) {
        taggedOutput(stdConsumer, tag, line);
    }

    @Override
    public void taggedErrOutput(String tag, String line) {
        taggedOutput(errorConsumer, tag, line);
    }

    private void taggedOutput(StreamConsumer consumer, String tag, String line) {
        if (consumer instanceof TaggedStreamConsumer) {
            ((TaggedStreamConsumer) consumer).taggedConsumeLine(tag, line);
        } else {
            consumer.consumeLine(line);
        }
    }
}

