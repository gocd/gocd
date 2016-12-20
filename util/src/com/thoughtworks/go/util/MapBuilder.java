/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.util;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MapBuilder {
    public static <S, T> Map<S, T> map() {
        return Collections.emptyMap();
    }

    public static <S, T> Map<S, T> map(S k, T v) {
        Map<S, T> values = new HashMap<>();
        values.put(k, v);
        return values;
    }

    public static <S, T> Map<S, T> map(S k1, T v1, S k2, T v2) {
        Map<S, T> values = map(k1, v1);
        values.put(k2, v2);
        return values;
    }

    public static <S, T> Map<S, T> map(S k1, T v1, S k2, T v2, S k3, T v3) {
        Map<S, T> values = map(k1, v1, k2, v2);
        values.put(k3, v3);
        return values;
    }

    public static <S, T> Map<S, T> map(S k1, T v1, S k2, T v2, S k3, T v3, S k4, T v4) {
        Map<S, T> values = map(k1, v1, k2, v2, k3, v3);
        values.put(k4, v4);
        return values;
    }

    public static <S, T> Map<S, T> map(S k1, T v1, S k2, T v2, S k3, T v3, S k4, T v4, S k5, T v5) {
        Map<S, T> values = map(k1, v1, k2, v2, k3, v3, k4, v4);
        values.put(k5, v5);
        return values;
    }

}
