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
package com.thoughtworks.go.config.registry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Understands mapping the implementers of a given Config interface.
 */
@Component
public class ConfigElementImplementationRegistry implements ConfigElementRegistry {

    private final ConcurrentMap<Class<?>, List<Class<?>>> registry;

    @Autowired
    public ConfigElementImplementationRegistry() {
        this.registry = new ConcurrentHashMap<>();
    }

    @Override
    public <T> List<Class<? extends T>> implementersOf(Class<T> type) {
        List<Class<? extends T>> toReturn = new ArrayList<>();
        for (Class<?> impl : registry.get(type)) {
            //noinspection unchecked
            toReturn.add((Class<? extends T>) impl);
        }
        return toReturn;
    }

    @SafeVarargs
    public final <T> void registerImplementer(Class<T> configInterface, Class<? extends T>... implementation) {
        List<Class<?>> set;
        if (registry.containsKey(configInterface)) {
            set = registry.get(configInterface);
        } else {//TODO: concurrency issue -jj (someone sets set before putIfAbsent)
            List<Class<?>> newSet = Collections.synchronizedList(new ArrayList<>());
            set = registry.putIfAbsent(configInterface, newSet);
            if (set == null) {
                set = newSet;
            }
        }
        set.addAll(Arrays.asList(implementation));
    }
}
