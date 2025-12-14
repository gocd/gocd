/*
 * Copyright Thoughtworks, Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TriStateTest {

    private final BooleanConsumer consumer = mock();

    @Test
    public void testTrueShouldBeTruthy() {
        TriState triState = TriState.from("tRuE");
        assertTrue(triState.isPresent());
        assertTrue(triState.get());
        triState.ifPresent(consumer);
        verify(consumer).accept(true);
    }

    @Test
    public void testFalseShouldBeTruthy() {
        TriState triState = TriState.from("FaLsE");
        assertTrue(triState.isPresent());
        assertFalse(triState.get());
        triState.ifPresent(consumer);
        verify(consumer).accept(false);
    }

    @Test
    public void testUnsetShouldBeTruthy() {
        TriState triState = TriState.from(null);
        assertFalse(triState.isPresent());
        assertThatThrownBy(triState::get).isInstanceOf(IllegalStateException.class);
        triState.ifPresent(consumer);
        verifyNoInteractions(consumer);
    }

    @Test
    public void testPrimitivesShouldParse() {
        assertThat(TriState.from(true)).isEqualTo(TriState.TRUE);
        assertThat(TriState.from(false)).isEqualTo(TriState.FALSE);
    }

    @Test
    public void testBadStringShouldRaiseError() {
        assertThrows(IllegalArgumentException.class, () -> TriState.from("foo"));
    }
}
