/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.config;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigurableSSLSettingsTest {
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.getPropertyImpl("sslconfig")).thenReturn("Y");
    }

    @Test
    public void shouldGetIncludedCiphers() {
        String[] ciphers = {"CIPHER1", "CIPHER2"};
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_INCLUDE_CIPHERS)).thenReturn(ciphers);
        ConfigurableSSLSettings config = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(config.getCipherSuitesToBeIncluded(), is(ciphers));
    }

    @Test
    public void shouldGetExcludedCiphers() {
        String[] ciphers = {"CIPHER1", "CIPHER2"};
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_EXCLUDE_CIPHERS)).thenReturn(ciphers);
        ConfigurableSSLSettings config = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(config.getCipherSuitesToBeExcluded(), is(ciphers));
    }

    @Test
    public void shouldGetIncludedProtocols() {
        String[] protocols = {"PROTO1", "PROTO2"};
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_INCLUDE_PROTOCOLS)).thenReturn(protocols);
        ConfigurableSSLSettings config = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(config.getProtocolsToBeIncluded(), is(protocols));
    }

    @Test
    public void shouldGetExcludedProtocols() {
        String[] protocols = {"PROTO1", "PROTO2"};
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_EXCLUDE_PROTOCOLS)).thenReturn(protocols);
        ConfigurableSSLSettings config = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(config.getProtocolsToBeExcluded(), is(protocols));
    }

    @Test
    public void shouldGetRenegotiationAllowedFlag() {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_RENEGOTIATION_ALLOWED)).thenReturn(true);
        ConfigurableSSLSettings config = new ConfigurableSSLSettings(systemEnvironment);
        assertThat(config.isRenegotiationAllowed(), is(true));
    }
}
