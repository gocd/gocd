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
package com.thoughtworks.go.config;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.thoughtworks.go.config.EnvironmentAgentConfig.UUID;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;

class EnvironmentAgentConfigTest {
    @Test
    void shouldValidateToTrueIfTheUUIDAssociatedWithEnvironmentIsPresentInTheSystem() {
        String uuidThatWillBeValidated = "uuid2";
        EnvironmentAgentConfig envAgentConf = new EnvironmentAgentConfig(uuidThatWillBeValidated);

        Set<String> setOfUUIDs = new HashSet<>(asList("uuid1", uuidThatWillBeValidated, "uuid3"));
        boolean isPresent = envAgentConf.validateUuidPresent(new CaseInsensitiveString("env1"), setOfUUIDs);

        assertTrue(isPresent);
    }

    @Test
    void shouldValidateToFalseIfTheUUIDAssociatedWithEnvironmentIsNull() {
        String uuidThatWillBeValidated = null;
        EnvironmentAgentConfig envAgentConf = new EnvironmentAgentConfig(uuidThatWillBeValidated);

        Set<String> setOfUUIDs = new HashSet<>(asList("uuid1", "uuid2", "uuid3"));
        boolean isPresent = envAgentConf.validateUuidPresent(new CaseInsensitiveString("env1"), setOfUUIDs);
        assertFalse(isPresent);
        assertEquals("Environment 'env1' has an invalid agent uuid 'null'", envAgentConf.errors().on(UUID));
    }

    @Test
    void shouldValidateToFalseIfTheSetOfUUIDsSpecifiedIsNull() {
        String uuidThatWillBeValidated = "uuid";
        EnvironmentAgentConfig envAgentConf = new EnvironmentAgentConfig(uuidThatWillBeValidated);

        boolean isPresent = envAgentConf.validateUuidPresent(new CaseInsensitiveString("env1"), null);
        assertFalse(isPresent);
        assertEquals("Environment 'env1' has an invalid agent uuid 'uuid'", envAgentConf.errors().on(UUID));
    }

    @Test
    void shouldValidateToFalseIfTheSetOfUUIDsSpecifiedIsEmpty() {
        String uuidThatWillBeValidated = "uuid";
        EnvironmentAgentConfig envAgentConf = new EnvironmentAgentConfig(uuidThatWillBeValidated);

        boolean isPresent = envAgentConf.validateUuidPresent(new CaseInsensitiveString("env1"), emptySet());
        assertFalse(isPresent);
        assertEquals("Environment 'env1' has an invalid agent uuid 'uuid'", envAgentConf.errors().on(UUID));
    }
}