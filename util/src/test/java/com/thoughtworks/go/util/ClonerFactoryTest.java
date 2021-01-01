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

import com.rits.cloning.Cloner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClonerFactoryTest {
    private Cloner cloner;

    @BeforeEach
    void setup() {
        cloner = ClonerFactory.instance();
    }

    @Test
    void cloningList12_one_item() {
        final List<Boolean> list = List.of(true); // JDK 15 has an issue cloning List.of() with exactly one element
        final List<Boolean> dupe = cloner.deepClone(list);

        assertEquals(1, dupe.size());
        assertEquals(list.get(0), dupe.get(0));
        assertEquals(list, dupe);
    }

    @Test
    void cloningList12_two_items() {
        final List<Boolean> list = List.of(true, false);
        final List<Boolean> dupe = cloner.deepClone(list);

        assertEquals(2, dupe.size());
        assertEquals(list.get(0), dupe.get(0));
        assertEquals(list.get(1), dupe.get(1));
        assertEquals(list, dupe);
    }

    @Test
    void cloningSet12_one_item() {
        final Set<Boolean> set = Set.of(true); // JDK 15 has an issue cloning Set.of() with exactly one element
        final Set<Boolean> dupe = cloner.deepClone(set);

        assertEquals(1, dupe.size());
        assertTrue(dupe.contains(true));
        assertEquals(set, dupe);
    }

    @Test
    void cloningSet12_two_items() {
        final Set<Boolean> set = Set.of(true, false);
        final Set<Boolean> dupe = cloner.deepClone(set);

        assertEquals(2, dupe.size());
        assertTrue(dupe.contains(true));
        assertTrue(dupe.contains(false));
        assertEquals(set, dupe);
    }

    @Test
    void cloneBiggerList() {
        final List<Integer> list = List.of(1, 2, 3, 4, 5);
        final List<Integer> dupe = cloner.deepClone(list);

        assertEquals(5, dupe.size());
        assertEquals(list, dupe);
    }

    @Test
    void cloneBiggerSet() {
        final Set<Integer> set = Set.of(1, 2, 3, 4, 5);
        final Set<Integer> dupe = cloner.deepClone(set);

        assertEquals(5, dupe.size());
        assertEquals(set, dupe);
    }

}