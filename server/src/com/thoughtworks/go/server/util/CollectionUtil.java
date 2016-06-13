/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionUtil {
    public interface MapFn<O, T> {
        T map(O o);
    }

    public static <O, T> List<T> map(Collection<O> collection, MapFn<O, T> mapFn) {
        List<T> values = new java.util.ArrayList<>();
        for (O obj : collection) {
            values.add(mapFn.map(obj));
        }
        return values;
    }

    public static <K, V> Map<V, Set<K>> reverse(Map<K, List<V>> inputMap) {
        Map<V, Set<K>> resultMap = new HashMap<>();
        CollectionValueMap<V, K> map = collectionValMap(resultMap, new HashSet<K>());
        for (Map.Entry<K, List<V>> entry : inputMap.entrySet()) {
            for (V v : entry.getValue()) {
                map.put(v, entry.getKey());
            }
        }
        return resultMap;
    }

    public static <K, V> CollectionValueMap<K, V> collectionValMap(Map<K, ? extends Collection<V>> map, CollectionCreator<V> collectionCreator) {
        return new CollectionValueMap<>((Map<K, Collection<V>>) map, collectionCreator);
    }

    public static class CollectionValueMap<K, V> {
        private final Map<K, Collection<V>> map;
        private final CollectionCreator<V> collectionCreator;

        public CollectionValueMap(Map<K, Collection<V>> map, CollectionCreator<V> collectionCreator) {
            this.map = map;
            this.collectionCreator = collectionCreator;
        }

        public void put(K k, V v) {
            Collection<V> c;
            if (map.containsKey(k)) {
                c = map.get(k);
            } else {
                c = collectionCreator.create();
                map.put(k, c);
            }
            c.add(v);
        }
    }

    public interface CollectionCreator<T> {
        Collection<T> create();
    }

    public static class HashSet<T> implements CollectionCreator<T> {
        public Collection<T> create() {
            return new java.util.HashSet<>();
        }
    }

    public static class ArrayList<T> implements CollectionCreator<T> {
        public Collection<T> create() {
            return new java.util.ArrayList<>();
        }
    }
}
