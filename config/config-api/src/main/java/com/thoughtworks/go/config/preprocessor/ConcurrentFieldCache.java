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

package com.thoughtworks.go.config.preprocessor;

import org.jetbrains.annotations.TestOnly;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

public class ConcurrentFieldCache {
    private final ConcurrentMap<Class<?>, List<Field>> classToFields = new ConcurrentHashMap<>();

    private List<Field> get(Class<?> klass) {
        return classToFields.computeIfAbsent(klass, k -> allNonStaticOrSyntheticFieldsFor(k).toList());
    }

    private Stream<Field> allNonStaticOrSyntheticFieldsFor(Class<?> klass) {
        return concat(
            stream(klass.getDeclaredFields()).filter(ConcurrentFieldCache::includeField),
            klass.getSuperclass() == Object.class ? Stream.empty() : allNonStaticOrSyntheticFieldsFor(klass.getSuperclass()
            )
        );
    }

    private static boolean includeField(Field field) {
        return !Modifier.isStatic(field.getModifiers()) && !field.isSynthetic();
    }

    private void clear() {
        classToFields.clear();
    }

    private static class Holder {
        private static final ConcurrentFieldCache INSTANCE = new ConcurrentFieldCache();
    }

    public static List<Field> nonStaticOrSyntheticFieldsFor(Class<?> key) {
        return Holder.INSTANCE.get(key);
    }

    public static void reset() {
        Holder.INSTANCE.clear();
    }

    @TestOnly
    static int size() {
        return Holder.INSTANCE.classToFields.size();
    }
}