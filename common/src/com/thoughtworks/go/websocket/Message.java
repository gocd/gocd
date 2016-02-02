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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class Message {

    public static void send(Socket socket, Message msg) throws IOException {
        int bufferSize = socket.getMaxMessageBufferSize();
        byte[] bytes = encode(msg);
        boolean last;
        for (int offset=0; offset < bytes.length; offset+=bufferSize) {
            last = bytes.length <= offset + bufferSize;
            int length = last ? bytes.length - offset : bufferSize;
            socket.sendPartialBytes(ByteBuffer.wrap(bytes, offset, length), last);
        }
    }

    public static byte[] encode(Message msg) {
        String encode = JsonMessage.encode(msg);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            GZIPOutputStream out = new GZIPOutputStream(bytes);
            out.write(encode.getBytes(StandardCharsets.UTF_8));
            out.finish();
        } catch (IOException e) {
            throw bomb(e);
        }
        return bytes.toByteArray();
    }

    public static Message decode(InputStream input) {
        try {
            GZIPInputStream zipStream = new GZIPInputStream(input);
            return JsonMessage.decode(new String(IOUtils.toByteArray(zipStream), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw bomb(e);
        }
    }

    private final Action action;
    private final Object data;

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

    @Override
    public String toString() {
        return "Message{" +
                "action=" + action +
                ", data=" + data +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        if (action != message.action) return false;
        return data != null ? data.equals(message.data) : message.data == null;

    }

    @Override
    public int hashCode() {
        int result = action != null ? action.hashCode() : 0;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

}
