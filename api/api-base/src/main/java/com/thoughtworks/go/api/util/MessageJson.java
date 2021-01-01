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
package com.thoughtworks.go.api.util;

import com.thoughtworks.go.api.base.JsonOutputWriter;
import com.thoughtworks.go.api.base.OutputWriter;

import java.io.StringWriter;
import java.util.function.Consumer;

public class MessageJson {

    private final String message;
    private final Consumer<OutputWriter> json;

    private MessageJson(String message, Consumer<OutputWriter> json) {
        this.message = message;
        this.json = json;
    }

    public static String create(String message, Consumer<OutputWriter> json) {
        return new MessageJson(message, json).toString();
    }

    public static String create(String message) {
        return create(message, null);
    }

    @Override
    public String toString() {
        StringWriter buffer = new StringWriter(1024);
        new JsonOutputWriter(buffer, null).forTopLevelObject((OutputWriter writer) -> {
            writer.add("message", message);
            if (json != null) {
                writer.addChild("data", json);
            }
        });

        return buffer.toString();
    }
}
