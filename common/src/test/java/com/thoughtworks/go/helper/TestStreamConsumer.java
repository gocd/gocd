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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.util.command.TaggedStreamConsumer;
import com.thoughtworks.go.utils.Assertions;
import com.thoughtworks.go.utils.Timeout;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BooleanSupplier;

public class TestStreamConsumer implements TaggedStreamConsumer {
    private ConcurrentLinkedDeque<String> lines = new ConcurrentLinkedDeque<>();

    @Override
    public void consumeLine(String line) {
        taggedConsumeLine(null, line);
    }

    public String output() {
        return StringUtils.join(lines, "\n");
    }

    @Override
    public String toString() {
        return output();
    }

    public List<String> asList() {
        return new ArrayList<>(lines);
    }


    public String lastLine() {
        return lines.getLast();
    }

    public String firstLine() {
        return lines.getFirst();
    }

    public int lineCount() {
        return lines.size();
    }

    public void waitForContain(final String content, Timeout timeout) throws InterruptedException {
        Assertions.waitUntil(timeout, new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return output().contains(content);
            }
        }, 250);
    }

    public void clear() {
        lines.clear();
    }

    @Override
    public void taggedConsumeLine(String tag, String line) {
        lines.add(line);
    }
}
