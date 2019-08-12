/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.remote;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnknownOriginTest {
    private ConfigOrigin origin;

    @BeforeEach
    void setUp() {
        origin = new UnknownOrigin();
    }

    @Test
    void canEdit() {
        assertFalse(origin.canEdit());
    }

    @Test
    void isLocal() {
        assertFalse(origin.isLocal());
    }

    @Test
    void displayName() {
        assertEquals("Unknown", origin.displayName());
    }

    @Test
    void isUnknown() {
        assertTrue(origin.isUnknown());
    }

    @Test
    void shouldMatchTheHash() {
        assertEquals(1379812425, origin.hashCode());
    }
}