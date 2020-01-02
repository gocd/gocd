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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

public class DefaultAgentRegistryTest {
    private DefaultAgentRegistry agentRegistry;
    private static final String GUID = "guid";
    private static final String TOKEN = "token";
    private GuidService guidService;
    private TokenService tokenService;

    @Before
    public void setUp() throws Exception {
        agentRegistry = new DefaultAgentRegistry();
        guidService = new GuidService();
        tokenService = new TokenService();

        guidService.store(GUID);
        tokenService.store(TOKEN);
    }

    @After
    public void tearDown() throws Exception {
        guidService.delete();
        tokenService.delete();
    }

    @Test
    public void shouldCreateGuidIfOneNotAlreadySet() throws Exception {
        guidService.delete();
        String guid = agentRegistry.uuid();
        assertNotNull(guid);
        assertThat(guid, is(agentRegistry.uuid()));
        assertThat(guid, is(not(GUID)));
    }

    @Test
    public void shouldUseGuidThatAlreadyExists() throws Exception {
        assertThat(agentRegistry.uuid(), is(GUID));
    }

    @Test
    public void shouldCheckGuidPresent() throws Exception {
        assertTrue(agentRegistry.guidPresent());

        guidService.delete();
        assertFalse(agentRegistry.guidPresent());
    }

    @Test
    public void shouldGetTokenFromFile() throws Exception {
        assertThat(agentRegistry.token(), is(TOKEN));
    }

    @Test
    public void shouldCheckTokenPresent() throws Exception {
        assertTrue(agentRegistry.tokenPresent());

        tokenService.delete();

        assertFalse(agentRegistry.tokenPresent());
    }

    @Test
    public void shouldStoreTokenToDisk() throws Exception {
        assertThat(agentRegistry.token(), is(TOKEN));

        agentRegistry.storeTokenToDisk("foo-token");

        assertThat(agentRegistry.token(), is("foo-token"));
    }

    @Test
    public void shouldDeleteTokenFromDisk() throws Exception {
        assertThat(agentRegistry.token(), is(TOKEN));
        assertTrue(agentRegistry.tokenPresent());

        agentRegistry.deleteToken();

        assertFalse(agentRegistry.tokenPresent());
    }
}
