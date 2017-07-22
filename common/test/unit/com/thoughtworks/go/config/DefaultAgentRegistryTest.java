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

package com.thoughtworks.go.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class DefaultAgentRegistryTest {
    private DefaultAgentRegistry agentRegistry;
    private static final String GUID = "guid";

    @Before public void setUp() throws Exception {
        agentRegistry = new DefaultAgentRegistry();
        GuidService.storeGuid(GUID);
    }

    @After public void tearDown() throws Exception {
        GuidService.deleteGuid();
    }

    @Test public void shouldCreateGuidIfOneNotAlreadySet() throws Exception {
        GuidService.deleteGuid();
        String guid = agentRegistry.uuid();
        assertNotNull(guid);
        assertThat(guid, is(agentRegistry.uuid()));
        assertThat(guid, is(not(GUID)));
    }

    @Test public void shouldUseGuidThatAlreadyExists() throws Exception {
        assertThat(agentRegistry.uuid(), is(GUID));
    }
}
