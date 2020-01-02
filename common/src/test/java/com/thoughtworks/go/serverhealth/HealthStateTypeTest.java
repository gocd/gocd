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
package com.thoughtworks.go.serverhealth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class HealthStateTypeTest {
    @Test
    public void shouldDifferentiateTypeUsingSubkey() {
        HealthStateScope scope = HealthStateScope.fromPlugin("plugin-id");

        assertEquals(HealthStateType.general(scope), HealthStateType.general(scope));
        assertEquals(HealthStateType.withSubkey(scope, "subkey1"), HealthStateType.withSubkey(scope, "subkey1"));

        assertNotEquals(HealthStateType.withSubkey(scope, "subkey1"), HealthStateType.withSubkey(scope, "subkey2"));
        assertNotEquals(HealthStateType.withSubkey(scope, "subkey1"), HealthStateType.general(scope));
    }
}