/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import org.apache.commons.io.IOUtils;

public final class ObjectUtil {
    private ObjectUtil() {
    }

    public static <T> T defaultIfNull(T original, T defaultValue) {
        if (original == null) {
            return defaultValue;
        }
        return original;
    }

    public static boolean nullSafeEquals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    public static Object readObject(File file) throws ClassNotFoundException, IOException {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return new ObjectInputStream(inputStream).readObject();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public static void writeObject(Object o, File file) throws IOException {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            writeObject(o, outputStream);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    public static void writeObject(Object o, OutputStream outputStream) throws IOException {
        new ObjectOutputStream(outputStream).writeObject(o);
    }

    public static byte[] serialize(Object o) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(buffer).writeObject(o);
        } catch (IOException e) {
            throw ExceptionUtils.bomb(e);
        }
        return buffer.toByteArray();
    }

    public static Object deserialize(byte[] bytes) {
        try {
            return new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
        } catch (Exception e) {
            throw ExceptionUtils.bomb(e);
        }
    }

    public static boolean equal(Object one, Object other) {
        if (one != null ? !one.equals(other) : other != null) {
            return false;
        }
        return true;
    }
}
