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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DataStructureUtils {
    public static <K, V> Map<K, V> m(K firstKey, V firstValue, Object... alternateKeyValues) {
        Map<K, V> map = new HashMap<>();
        //noinspection unchecked
        map.put(firstKey, firstValue);
        return populateMap(map, alternateKeyValues);
    }

    public static <K, V> Map<K, V> m(Map<K, V> src, Object... alternateKeyValues) {
        Map<K, V> map = new HashMap<>();
        //noinspection unchecked
        map.putAll(src);
        return populateMap(map, alternateKeyValues);
    }

    public static <K, V> Map<K, V> m(Class<K> keyType, Class<V> valType) {
        Map<K, V> map = new HashMap<>();
        //noinspection unchecked
        return populateMap(map, new Object[0]);
    }

    public static Map m() {
        return new HashMap();
    }

    private static Map populateMap(Map map, Object[] alternateKeyValues) {
        for(int i = 0; i < alternateKeyValues.length; i++) {
            if (i%2 == 1) {
                map.put(alternateKeyValues[i - 1], alternateKeyValues[i]);
            }
        }
        return map;
    }

    public static <T> List<T> a(T... items) {
        return Arrays.asList(items);
    }

    public static <T> Set<T> s(T... items) {
        HashSet<T> set = new HashSet<>();
        for (T item : items) {
            set.add(item);
        }
        return set;
    }

    public static <T> List<T> listOf(Iterator<T> iterator) {
        ArrayList<T> list = new ArrayList<>();
        while(iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }
}
