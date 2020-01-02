/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

public class MapBuilderTest {

    @Test
    public void testEmptyMap() {
        assertThat(MapBuilder.map(), is(Collections.emptyMap()));
    }

    @Test
    public void testLengthTwo() {
        Map<String, Integer> expected = new HashMap<>();
        expected.put("foo", 23);
        expected.put("s", 42);
        assertThat(MapBuilder.map("foo", 23, "s", 42), is(expected));
    }

    @Test
    public void testLengthThree() {
        Map<String, Integer> expected = new HashMap<>();
        expected.put("foo", 23);
        expected.put("s", 42);
        expected.put("t", 42);
        assertThat(MapBuilder.map("foo", 23, "s", 42, "t", 42), is(expected));
    }

    @Test
    public void testLengthFour() {
        Map<String, Integer> expected = new HashMap<>();
        expected.put("foo", 23);
        expected.put("s", 42);
        expected.put("t", 42);
        expected.put("x", 43);
        assertThat(MapBuilder.map("foo", 23, "s", 42, "t", 42, "x", 43), is(expected));
    }

    @Test
    public void testLengthFive() {
        Map<String, Integer> expected = new HashMap<>();
        expected.put("foo", 23);
        expected.put("s", 42);
        expected.put("t", 42);
        expected.put("x", 43);
        expected.put("y", 44);
        assertThat(MapBuilder.map("foo", 23, "s", 42, "t", 42, "x", 43, "y", 44), is(expected));
    }

    @Test
    public void testLengthSix() {
        Map<String, Integer> expected = new HashMap<>();
        expected.put("foo", 23);
        expected.put("s", 42);
        expected.put("t", 42);
        expected.put("x", 43);
        expected.put("y", 44);
        expected.put("z", 45);
        assertThat(MapBuilder.map("foo", 23, "s", 42, "t", 42, "x", 43, "y", 44, "z", 45), is(expected));
    }

    @Test
    public void testLengthSeven() {
        Map<String, Integer> expected = new HashMap<>();
        expected.put("foo", 23);
        expected.put("s", 42);
        expected.put("t", 42);
        expected.put("x", 43);
        expected.put("y", 44);
        expected.put("z", 45);
        expected.put("q", 46);
        assertThat(MapBuilder.map("foo", 23, "s", 42, "t", 42, "x", 43, "y", 44, "z", 45, "q", 46), is(expected));
    }

}
