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

package com.thoughtworks.go.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class MapUtil {

    public interface Predicate<V> {
        boolean apply(V obj);
    }

    public static <T, V> Collection<V> filterValues(Map<T, V> map, Predicate<V> predicate) {
        ArrayList<V> result = new ArrayList<>();
        Collection<V> values = map.values();
        for (V value : values) {
            if (predicate.apply(value)) {
                result.add(value);
            }
        }
        return result;
    }

    public static <T, V, O> Collection<O> collect(Map<T, V> input, ListUtil.Transformer<Map.Entry<T, V>, O> transformer) {
        ArrayList<O> result = new ArrayList<>();

        for (Map.Entry<T, V> entry : input.entrySet()) {
            result.add(transformer.transform(entry));
        }

        return result;
    }
}
