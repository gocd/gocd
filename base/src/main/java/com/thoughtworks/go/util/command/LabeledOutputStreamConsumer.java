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
package com.thoughtworks.go.util.command;

public class LabeledOutputStreamConsumer implements ConsoleOutputStreamConsumer {
    private String tag;
    private String errorTag;
    private ConsoleOutputStreamConsumer consumer;

    public LabeledOutputStreamConsumer(String tag, String errorTag, ConsoleOutputStreamConsumer consumer) {
        this.tag = tag;
        this.errorTag = errorTag;
        this.consumer = consumer;
    }

    @Override
    public void stdOutput(String line) {
        consumer.taggedStdOutput(tag, line);
    }

    @Override
    public void errOutput(String line) {
        consumer.taggedErrOutput(errorTag, line);
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
