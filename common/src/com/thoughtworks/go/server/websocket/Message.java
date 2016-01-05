/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.websocket;

import com.google.gson.Gson;

public class Message {
    public static String encode(Action obj) {
        Gson gson = new Gson();
        return gson.toJson(new Message(obj.getClass().getCanonicalName(), gson.toJson(obj)), Message.class);
    }

    public static Action decode(String message) {
        Gson gson = new Gson();
        Message msg = gson.fromJson(message, Message.class);
        try {
            Class k = Class.forName(msg.className);
            return (Action) gson.fromJson(msg.json, k);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find class: " + msg.className, e);
        }
    }

    public Message(String className, String json) {
        this.className = className;
        this.json = json;
    }

    private final String className;
    private final String json;

}
