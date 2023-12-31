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
package com.thoughtworks.go.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtil {
    public static void setField(Object o, String name, Object value) {
        try {
            field(o, name).set(o, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setStaticField(Class<?> kls, String name, Object value) {
        try {
            Field field = kls.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getStaticField(Class<?> kls, String name) {
        try {
            Field field = kls.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getField(Object o, String name) {
        try {
            return (T) field(o, name).get(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Method method(String name, Class<?> klass) {
        if (klass == null) {
            return null;
        }
        Method[] fields = klass.getDeclaredMethods();
        for (Method field : fields) {
            if (field.getName().equals(name)) {
                field.setAccessible(true);
                return field;
            }
        }
        return method(name, klass.getSuperclass());
    }

    @SuppressWarnings("unchecked")
    public static <T> T invoke(Object o, String method, Object... args) throws Exception {
        Method m = method(method, o.getClass());
        m.setAccessible(true);
        return (T) m.invoke(o, args);
    }

    private static Field field(Object o, String name) {
        return field(name, o.getClass());
    }

    private static Field field(String name, Class<?> klass) {
        if (klass == null) {
            return null;
        }
        Field[] fields = klass.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals(name)) {
                field.setAccessible(true);
                return field;
            }
        }
        return field(name, klass.getSuperclass());
    }
}
