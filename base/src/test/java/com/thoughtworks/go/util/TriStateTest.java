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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TriStateTest {

    @Test
    public void testTrueShouldBeTruthy() {
        TriState triState = TriState.from("tRuE");
        assertTrue(triState.isTrue());
        assertTrue(triState.isTruthy());
        assertFalse(triState.isFalsy());
        assertFalse(triState.isFalse());
    }

    @Test
    public void testFalseShouldBeTruthy() {
        TriState triState = TriState.from("FaLsE");
        assertTrue(triState.isFalsy());
        assertTrue(triState.isFalse());
        assertFalse(triState.isTrue());
        assertFalse(triState.isTruthy());
    }


    @Test
    public void testUnsetShouldBeTruthy() {
        TriState triState = TriState.from(null);
        assertTrue(triState.isFalsy());
        assertFalse(triState.isFalse());
        assertFalse(triState.isTrue());
        assertFalse(triState.isTruthy());
    }

    @Test
    public void testBadStringShouldRaiseError() {
        assertThrows(IllegalArgumentException.class, () -> TriState.from("foo"));
    }
}
