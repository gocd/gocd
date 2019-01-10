/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CollectionUtilTest {
    @Test
    public void shouldReverseKeyToCollectionMap() {
        Map<String, List<Integer>> stringIntMap = new HashMap<>();
        stringIntMap.put("foo", Arrays.asList(1, 2, 3, 4));
        stringIntMap.put("bar", Arrays.asList(3, 4, 5, 6));
        Map<Integer, Set<String>> reversedMap = CollectionUtil.reverse(stringIntMap);
        assertThat(reversedMap.get(1), is(new HashSet<>(Arrays.asList("foo"))));
        assertThat(reversedMap.get(2), is(new HashSet<>(Arrays.asList("foo"))));
        assertThat(reversedMap.get(3), is(new HashSet<>(Arrays.asList("foo", "bar"))));
        assertThat(reversedMap.get(4), is(new HashSet<>(Arrays.asList("foo", "bar"))));
        assertThat(reversedMap.get(5), is(new HashSet<>(Arrays.asList("bar"))));
        assertThat(reversedMap.get(6), is(new HashSet<>(Arrays.asList("bar"))));
    }

    @Test
    public void shouldPopulateMapWithInitialization() {
        Map<String, List<Integer>> mapWithCollection = new HashMap<>();
        CollectionUtil.CollectionValueMap<String, Integer> cVM = CollectionUtil.collectionValMap(mapWithCollection, new CollectionUtil.CollectionCreator<Integer>() {
            public Collection<Integer> create() {
                return new ArrayList<>();
            }
        });
        cVM.put("foo", 10);
        cVM.put("bar", 20);
        cVM.put("foo", 30);
        assertThat(mapWithCollection.get("foo"), is(Arrays.asList(10, 30)));
        assertThat(mapWithCollection.get("bar"), is(Arrays.asList(20)));
    }

    @Test
    public void shouldMapWithGivenMappingFn() {
        List<String> firstChar = CollectionUtil.map(Arrays.asList("foo", "bar", "quux"), new CollectionUtil.MapFn<String, String>() {
            public String map(String s) {
                return s.substring(0, 1);
            }
        });

        assertThat(firstChar, is(Arrays.asList("f", "b", "q")));
    }
}
