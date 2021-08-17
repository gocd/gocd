/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.util.Pair.pair;
import static com.thoughtworks.go.util.Pair.twins;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class PairTest {

    @Test
    public void equalities(){
        assertEquals(pair("a", "b"), pair("a", "b"));
        assertNotEquals(pair("a", "b"), pair("a", "c"));
        assertEquals(pair("a", pair("b", pair("c", null))), pair("a", pair("b", pair("c", null))));
    }

    @Test
    public void creatingTwins() {
        assertEquals(pair("a","a"), twins("a"));
    }
}
