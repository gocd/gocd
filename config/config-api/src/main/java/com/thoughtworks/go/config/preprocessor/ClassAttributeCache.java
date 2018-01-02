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

package com.thoughtworks.go.config.preprocessor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class ClassAttributeCache<K, T> {
    private ConcurrentMap<K, T> valueCache = new ConcurrentHashMap<>();

    public T valuesFor(K key) {
        T value = valueCache.get(key);
        if (value == null) {
            value = loadValues(key);
            valueCache.putIfAbsent(key, value);
        }
        return value;
    }

    abstract T loadValues(K key);

    public static class FieldCache extends ClassAttributeCache<Class, List<Field>> {
        List<Field> loadValues(Class klass) {
            List<Field> fields = new ArrayList<>();
            populateValueInto(klass, fields);
            return fields;
        }

        void populateValueInto(Class klass, List<Field> fields) {
            fields.addAll(Arrays.asList(klass.getDeclaredFields()));
            Class superClass = klass.getSuperclass();
            if (superClass != Object.class) {
                populateValueInto(superClass, fields);
            }
        }
    }

    public static class AssignableCache extends ClassAttributeCache<Map.Entry<Class, Class>, Boolean> {
        Boolean loadValues(Map.Entry<Class, Class> entry) {
            return entry.getKey().isAssignableFrom(entry.getValue());
        }
    }

}
