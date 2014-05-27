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

import org.apache.commons.lang.text.StrSubstitutor;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vagrant on 5/8/14.
 */
public class MapUtil {
    public static <T> void putWithPrefix(Map<String, T> map, String prefix, String key, T value) {
        map.put(prefix + key, value);
    }

    public static <T> void putAllWithPrefix(Map<String, T> map, String prefix, Map<String, T> source) {
        for (Map.Entry<String, T> entry : source.entrySet()) {
            map.put(prefix + entry.getKey(), entry.getValue());
        }
    }

    public static <T> Map<T, String> expandVaraibles(Map<T, String> map, StrSubstitutor substitutor) {
        Map<T, String> processedMap = new HashMap<T, String>();

        for (Map.Entry<T, String> entry : map.entrySet()) {
            processedMap.put(entry.getKey(), substitutor.replace(entry.getValue()));
        }

        return processedMap;
    }
}
