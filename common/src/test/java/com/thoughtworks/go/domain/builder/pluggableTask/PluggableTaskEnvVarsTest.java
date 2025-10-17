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
package com.thoughtworks.go.domain.builder.pluggableTask;

import com.thoughtworks.go.plugin.api.task.Console;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PluggableTaskEnvVarsTest {

    private PluggableTaskEnvVars envVars;

    private final Map<String, String> props = Map.of(
        "Social Net 1", "Twitter",
        "Social Net 2", "Facebook",
        "Social Net 3", "Mega Upload"
    );

    @BeforeEach
    public void setUp() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        props.forEach((key, value) -> context.setProperty(key, value, keyShouldBeSecure(key)));

        envVars = new PluggableTaskEnvVars(context);
    }

    private static boolean keyShouldBeSecure(String key) {
        return key.hashCode() % 2 != 0;
    }

    @Test
    public void shouldReturnEnvVarsMap() {
        assertThat(envVars.asMap()).containsExactlyInAnyOrderEntriesOf(props);
    }

    @Test
    public void testSecureEnvSpecifier() {
        Console.SecureEnvVarSpecifier secureEnvVarSpecifier = envVars.secureEnvSpecifier();
        for (String key : props.keySet()) {
            assertThat(secureEnvVarSpecifier.isSecure(key)).isEqualTo(keyShouldBeSecure(key));
        }
    }

    @Test
    public void shouldPrintToConsole() {
        Console console = mock(Console.class);
        envVars.writeTo(console);
        verify(console).printEnvironment(envVars.asMap(), envVars.secureEnvSpecifier());
    }
}
