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

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.*;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class Message implements Serializable {

    public static byte[] encode(Message msg) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            GZIPOutputStream zip = new GZIPOutputStream(bytes);
            ObjectOutputStream output = new ObjectOutputStream(zip);
            try {
                output.writeObject(msg);
            } finally {
                output.close();
            }
        } catch (IOException e) {
            throw bomb(e);
        }
        return bytes.toByteArray();
    }

    public static Message decode(InputStream input) {
        try {
            ObjectInputStream stream = new ObjectInputStream(new GZIPInputStream(input));
            try {
                return (Message) stream.readObject();
            } finally {
                stream.close();
            }
        } catch (Exception e) {
            throw bomb(e);
        }
    }

    private final Action action;
    private final Object data;
    private String ackId;

    public Message(Action action) {
        this(action, null);
    }

    public Message(Action action, Object data) {
        this.action = action;
        this.data = data;
    }

    public Action getAction() {
        return action;
    }

    public Object getData() {
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
