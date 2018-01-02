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

import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.SSLSocketFactory;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoSSLConfigTest {
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
    }

    @Test
    public void shouldUseWeakConfigWhenSslConfigFlagTurnedOff() {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_ALLOW)).thenReturn(false);
        GoSSLConfig goSSLConfig = new GoSSLConfig(mock(SSLSocketFactory.class), systemEnvironment);
        SSLConfig config = (SSLConfig) ReflectionUtil.getField(goSSLConfig, "config");
        assertThat(config instanceof WeakSSLConfig, is(true));
    }

    @Test
    public void shouldUseConfigurableSSLSettingsWhenAllowSslConfigFlagIsTurnedOn() {
        when(systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_ALLOW)).thenReturn(true);
        GoSSLConfig goSSLConfig = new GoSSLConfig(mock(SSLSocketFactory.class), systemEnvironment);
        SSLConfig config = (SSLConfig) ReflectionUtil.getField(goSSLConfig, "config");
        assertThat(config instanceof ConfigurableSSLSettings, is(true));
    }
}
