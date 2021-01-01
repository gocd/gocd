/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain.builder.pluggableTask;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.plugin.api.task.Console;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PluggableTaskEnvVarsTest {

    private EnvironmentVariableContext context;
    private PluggableTaskEnvVars envVars;
    private List<String> keys = Arrays.asList("Social Net 1", "Social Net 2", "Social Net 3");
    private List<String> values = Arrays.asList("Twitter", "Facebook", "Mega Upload");

    @Before
    public void setUp() throws Exception {
        context = new EnvironmentVariableContext();
        for (int i = 0; i < keys.size(); i++) {
            context.setProperty(keys.get(i), values.get(i), i % 2 != 0);
        }
        envVars = new PluggableTaskEnvVars(context);
    }

    @Test
    public void shouldReturnEnvVarsMap() throws Exception {
        Map<String, String> envMap = envVars.asMap();
        assertThat(envMap.keySet().containsAll(keys), is(true));
        assertThat(envMap.values().containsAll(values), is(true));
        for (int i = 0; i < keys.size(); i++) {
            assertThat(envMap.get(keys.get(i)), is(values.get(i)));
        }
    }

    @Test
    public void testSecureEnvSpecifier() throws Exception {
        Console.SecureEnvVarSpecifier secureEnvVarSpecifier = envVars.secureEnvSpecifier();
        for (int i = 0; i < keys.size(); i++) {
            assertThat(secureEnvVarSpecifier.isSecure(keys.get(i)), is(i % 2 != 0));
        }
    }

    @Test
    public void shouldPrintToConsole() throws Exception {
        Console console = mock(Console.class);
        envVars.writeTo(console);
        verify(console).printEnvironment(envVars.asMap(), envVars.secureEnvSpecifier());
    }
}
