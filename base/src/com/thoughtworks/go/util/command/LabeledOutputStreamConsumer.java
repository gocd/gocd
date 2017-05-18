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

package com.thoughtworks.go.util.command;

import com.thoughtworks.go.util.Pair;

/**
 * Wraps a {@link ConsoleOutputStreamConsumer} to override the STDOUT and STDERR tags
 */
public class LabeledOutputStreamConsumer implements ConsoleOutputStreamConsumer {
    private String stdoutTag;
    private String stderrTag;
    private ConsoleOutputStreamConsumer consumer;

    public LabeledOutputStreamConsumer(Pair<String, String> tags, ConsoleOutputStreamConsumer consumer) {
        this.stdoutTag = tags.first();
        this.stderrTag = tags.last();
        this.consumer = consumer;
    }

    @Override
    public void stdOutput(String line) {
        consumer.taggedStdOutput(stdoutTag, line);
    }

    @Override
    public void errOutput(String line) {
        consumer.taggedErrOutput(stderrTag, line);
    }

    @Override
    public void taggedStdOutput(String tag, String line) {
        consumer.taggedStdOutput(tag, line);
    }

    @Override
    public void taggedErrOutput(String tag, String line) {
        consumer.taggedErrOutput(tag, line);
    }
}
