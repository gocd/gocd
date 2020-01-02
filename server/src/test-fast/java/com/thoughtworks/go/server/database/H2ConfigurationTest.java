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
package com.thoughtworks.go.server.database;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class H2ConfigurationTest {

    private SystemEnvironment systemEnvironment;
    private H2Configuration configuration;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        configuration = new H2Configuration(systemEnvironment);
    }

    @Test
    public void shouldReturnH2ConfigurationFromSystemEnvironment() throws Exception {
        String host = "host";
        int port = 1234;
        String name = "name";
        String user = "user";
        String password = "password";
        int maxIdle = 42;
        int maxActive = 4242;
        when(systemEnvironment.get(SystemEnvironment.GO_DATABASE_HOST)).thenReturn(host);
        when(systemEnvironment.getDatabaseSeverPort()).thenReturn(port);
        when(systemEnvironment.get(SystemEnvironment.GO_DATABASE_NAME)).thenReturn(name);
        when(systemEnvironment.get(SystemEnvironment.GO_DATABASE_USER)).thenReturn(user);
        when(systemEnvironment.get(SystemEnvironment.GO_DATABASE_PASSWORD)).thenReturn(password);
        when(systemEnvironment.get(SystemEnvironment.GO_DATABASE_MAX_IDLE)).thenReturn(maxIdle);
        when(systemEnvironment.get(SystemEnvironment.GO_DATABASE_MAX_ACTIVE)).thenReturn(maxActive);

        assertThat(configuration.getHost(), is(host));
        assertThat(configuration.getPort(), is(port));
        assertThat(configuration.getName(), is(name));
        assertThat(configuration.getUser(), is(user));
        assertThat(configuration.getPassword(), is(password));
        assertThat(configuration.getMaxIdle(), is(maxIdle));
        assertThat(configuration.getMaxActive(), is(maxActive));

        verify(systemEnvironment).get(SystemEnvironment.GO_DATABASE_HOST);
        verify(systemEnvironment).getDatabaseSeverPort();
        verify(systemEnvironment).get(SystemEnvironment.GO_DATABASE_NAME);
        verify(systemEnvironment).get(SystemEnvironment.GO_DATABASE_USER);
        verify(systemEnvironment).get(SystemEnvironment.GO_DATABASE_PASSWORD);
        verify(systemEnvironment).get(SystemEnvironment.GO_DATABASE_MAX_ACTIVE);
        verify(systemEnvironment).get(SystemEnvironment.GO_DATABASE_MAX_IDLE);
    }
}
