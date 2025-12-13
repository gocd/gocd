/*
 * Copyright Thoughtworks, Inc.
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

public class ReflectionUtil {
    public static void setField(Object o, String name, Object value) {
        try {
            field(o.getClass(), name).set(o, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setStaticField(Class<?> kls, String name, Object value) {
        try {
            field(kls, name).set(null, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getStaticField(Class<?> kls, String name) {
        try {
            Field field = kls.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getField(Object o, String name) {
        try {
            return (T) field(o.getClass(), name).get(o);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Field field(Class<?> klass, String name) throws NoSuchFieldException {
        if (klass == null) {
            throw new NoSuchFieldException(name + " field not found");
        }
        try {
            Field field = klass.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return field(klass.getSuperclass(), name);
        }
    }
}
