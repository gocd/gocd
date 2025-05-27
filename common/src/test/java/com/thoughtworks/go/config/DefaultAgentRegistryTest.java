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
package com.thoughtworks.go.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultAgentRegistryTest {
    private DefaultAgentRegistry agentRegistry;
    private static final String GUID = "guid";
    private static final String TOKEN = "token";
    private GuidService guidService;
    private TokenService tokenService;

    @BeforeEach
    public void setUp() {
        agentRegistry = new DefaultAgentRegistry();
        guidService = new GuidService();
        tokenService = new TokenService();

        guidService.store(GUID);
        tokenService.store(TOKEN);
    }

    @AfterEach
    public void tearDown() throws IOException {
        guidService.delete();
        tokenService.delete();
    }

    @Test
    public void shouldCreateGuidIfOneNotAlreadySet() throws IOException {
        guidService.delete();
        String guid = agentRegistry.uuid();
        assertNotNull(guid);
        assertThat(guid).isEqualTo(agentRegistry.uuid());
        assertThat(guid).isNotEqualTo(GUID);
    }

    @Test
    public void shouldUseGuidThatAlreadyExists() {
        assertThat(agentRegistry.uuid()).isEqualTo(GUID);
    }

    @Test
    public void shouldCheckGuidPresent() throws IOException {
        assertTrue(agentRegistry.guidPresent());

        guidService.delete();
        assertFalse(agentRegistry.guidPresent());
    }

    @Test
    public void shouldGetTokenFromFile() {
        assertThat(agentRegistry.token()).isEqualTo(TOKEN);
    }

    @Test
    public void shouldCheckTokenPresent() throws IOException {
        assertTrue(agentRegistry.tokenPresent());

        tokenService.delete();

        assertFalse(agentRegistry.tokenPresent());
    }

    @Test
    public void shouldStoreTokenToDisk() {
        assertThat(agentRegistry.token()).isEqualTo(TOKEN);

        agentRegistry.storeTokenToDisk("foo-token");

        assertThat(agentRegistry.token()).isEqualTo("foo-token");
    }

    @Test
    public void shouldDeleteTokenFromDisk() throws IOException {
        assertThat(agentRegistry.token()).isEqualTo(TOKEN);
        assertTrue(agentRegistry.tokenPresent());

        agentRegistry.deleteToken();

        assertFalse(agentRegistry.tokenPresent());
    }
}
