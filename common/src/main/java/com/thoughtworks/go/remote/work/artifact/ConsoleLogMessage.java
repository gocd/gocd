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
package com.thoughtworks.go.remote.work.artifact;

import com.google.gson.Gson;

public class ConsoleLogMessage {
    private static final Gson GSON = new Gson();
    private String message;
    private LogLevel logLevel;

    public String getMessage() {
        return message;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public static ConsoleLogMessage fromJSON(String json) {
        return GSON.fromJson(json, ConsoleLogMessage.class);
    }

    enum LogLevel {
        INFO, ERROR
    }
}
