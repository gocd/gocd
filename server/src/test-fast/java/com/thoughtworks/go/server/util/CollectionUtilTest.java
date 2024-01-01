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
package com.thoughtworks.go.server.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CollectionUtilTest {
    @Test
    public void shouldReverseKeyToCollectionMap() {
        Map<String, List<Integer>> stringIntMap = new HashMap<>();
        stringIntMap.put("foo", List.of(1, 2, 3, 4));
        stringIntMap.put("bar", List.of(3, 4, 5, 6));
        Map<Integer, Set<String>> reversedMap = CollectionUtil.reverse(stringIntMap);
        assertThat(reversedMap.get(1), is(Set.of("foo")));
        assertThat(reversedMap.get(2), is(Set.of("foo")));
        assertThat(reversedMap.get(3), is(Set.of("foo", "bar")));
        assertThat(reversedMap.get(4), is(Set.of("foo", "bar")));
        assertThat(reversedMap.get(5), is(Set.of("bar")));
        assertThat(reversedMap.get(6), is(Set.of("bar")));
    }

    @Test
    public void shouldPopulateMapWithInitialization() {
        Map<String, List<Integer>> mapWithCollection = new HashMap<>();
        CollectionUtil.CollectionValueMap<String, Integer> cVM = CollectionUtil.collectionValMap(mapWithCollection, new CollectionUtil.ArrayList<>());
        cVM.put("foo", 10);
        cVM.put("bar", 20);
        cVM.put("foo", 30);
        assertThat(mapWithCollection.get("foo"), is(List.of(10, 30)));
        assertThat(mapWithCollection.get("bar"), is(List.of(20)));
    }
}
