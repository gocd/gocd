/*
 * Copyright 2024 Thoughtworks, Inc.
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

public class BoundedOutputStreamConsumer implements ConsoleOutputStreamConsumer {

    private static final String CROP_NOTICE = "...[ cropped by GoCD ]";

    private final ConsoleOutputStreamConsumer consumer;
    private final int maxLineLength;

    public BoundedOutputStreamConsumer(ConsoleOutputStreamConsumer consumer, int maxLineLength) {
        this.consumer = consumer;
        this.maxLineLength = maxLineLength;
    }

    @Override
    public void taggedStdOutput(String tag, String line) {
        consumer.taggedStdOutput(tag, cropLongLine(line));
    }

    @Override
    public void taggedErrOutput(String tag, String line) {
        consumer.taggedErrOutput(tag, cropLongLine(line));
    }

    @Override
    public void stdOutput(String line) {
        consumer.stdOutput(cropLongLine(line));
    }

    @Override
    public void errOutput(String line) {
        consumer.errOutput(cropLongLine(line));
    }

    private String cropLongLine(String line) {
        if (maxLineLength > 0 && line.length() > maxLineLength) {
            return line.substring(0, Math.max(0, maxLineLength - CROP_NOTICE.length())) + CROP_NOTICE;
        }
        return line;
    }

}
