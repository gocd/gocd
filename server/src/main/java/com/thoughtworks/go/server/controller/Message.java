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
package com.thoughtworks.go.server.controller;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

public final class Message {
    private final String level;
    private final String key;
    private final String message;
    private static final String DEFAULT_KEY = "message";

    private Message(String level, String message) {
        this(level, DEFAULT_KEY, message);
    }

    private Message(String level, String key, String message) {
        this.level = level;
        this.key = key;
        this.message = message;
    }

    public String getLevel() {
        return level;
    }

    public String getKey() {
        return key;
    }

    public String getMessage() {
        return message;
    }

    public static Message error(String key, String message) {
        return new Message("error", key, message);
    }

    public static Message info(String key, String message) {
        return new Message("info", key, message);
    }

    public void populateModel(HashMap<String, Object> data) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        data.put(key, this);
    }
}
