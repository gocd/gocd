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
package com.thoughtworks.go.websocket;

import com.google.gson.annotations.Expose;

import java.util.UUID;

public class Message {

    @Expose
    private final Action action;
    @Expose
    private final String data;
    @Expose
    private final String acknowledgementId;

    public Message(Action action) {
        this(action, null);
    }

    public Message(Action action, String data) {
        this.action = action;
        this.data = data;
        this.acknowledgementId = UUID.randomUUID().toString();
    }

    public Action getAction() {
        return action;
    }

    public String getData() {
        return data;
    }

    public String getAcknowledgementId() {
        return acknowledgementId;
    }

    @Override
    public String toString() {
        return "Message{" +
                "action=" + action +
                ", data=" + data +
                ", acknowledgementId=" + acknowledgementId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        if (action != message.action) return false;
        if (data != null ? !data.equals(message.data) : message.data != null) return false;
        return acknowledgementId != null ? acknowledgementId.equals(message.acknowledgementId) : message.acknowledgementId == null;

    }

    @Override
    public int hashCode() {
        int result = action.hashCode();
        result = 31 * result + (data != null ? data.hashCode() : 0);
        result = 31 * result + (acknowledgementId != null ? acknowledgementId.hashCode() : 0);
        return result;
    }

}
