/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.websocket;

import com.google.gson.annotations.Expose;

import java.util.UUID;

public class Message {

    @Expose
    private final Action action;
    @Expose
    private final String data;
    @Expose
    private String ackId;

    public Message(Action action) {
        this(action, null);
    }

    public Message(Action action, String data) {
        this(action, data, null);
    }

    public Message(Action action, String data, String ackId) {
        this.action = action;
        this.data = data;
        this.ackId = ackId;
    }


    public Action getAction() {
        return action;
    }

    public String getData() {
        return data;
    }

    public String getAckId() {
        return ackId;
    }

    @Override
    public String toString() {
        return "Message{" +
                "action=" + action +
                ", data=" + data +
                ", ackId=" + ackId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        if (action != message.action) return false;
        if (data != null ? !data.equals(message.data) : message.data != null) return false;
        return ackId != null ? ackId.equals(message.ackId) : message.ackId == null;

    }

    @Override
    public int hashCode() {
        int result = action.hashCode();
        result = 31 * result + (data != null ? data.hashCode() : 0);
        result = 31 * result + (ackId != null ? ackId.hashCode() : 0);
        return result;
    }

    public void generateAckId() {
        this.ackId = UUID.randomUUID().toString();
    }
}
