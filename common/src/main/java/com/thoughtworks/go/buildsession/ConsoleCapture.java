/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.util.command.TaggedStreamConsumer;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

class ConsoleCapture implements TaggedStreamConsumer {
    private final ArrayList<String> captured;

    public ConsoleCapture() {
        this.captured = new ArrayList<>();
    }

    @Override
    public void consumeLine(String line) {
        taggedConsumeLine(null, line);
    }

    @Override
    public void taggedConsumeLine(String tag, String line) {
        captured.add(line);
    }

    public String captured() {
        return StringUtils.join(captured, "\n");
    }
}
