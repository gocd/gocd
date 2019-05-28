/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


public class EnvironmentVariableTest {
    @Test
    public void shouldCreateEnvironmentVariableFromEnvironmentVariableConfig() {
        final EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig("foo", "bar");
        assertThat(new EnvironmentVariable(environmentVariableConfig), is(new EnvironmentVariable("foo", "bar")));

        final EnvironmentVariableConfig secureEnvironmentVariableConfig = new EnvironmentVariableConfig(new GoCipher(), "foo", "bar", true);
        assertThat(new EnvironmentVariable(secureEnvironmentVariableConfig), is(new EnvironmentVariable("foo", "bar", true)));
    }

    @Test
    public void addTo_shouldAddEnvironmentVariableToEnvironmentVariableContext() {
        final EnvironmentVariableContext environmentVariableContext = mock(EnvironmentVariableContext.class);
        final EnvironmentVariable environmentVariable = new EnvironmentVariable("foo", "bar");

        environmentVariable.addTo(environmentVariableContext);

        verify(environmentVariableContext, times(1)).setProperty("foo", "bar", false);
    }

    @Test
    public void addToIfExists_shouldAddEnvironmentVariableToEnvironmentVariableContextWhenVariableIsAlreadyExistInContext() {
        final EnvironmentVariableContext environmentVariableContext = mock(EnvironmentVariableContext.class);
        final EnvironmentVariable environmentVariable = new EnvironmentVariable("foo", "bar");

        when(environmentVariableContext.hasProperty("foo")).thenReturn(true);

        environmentVariable.addToIfExists(environmentVariableContext);

        verify(environmentVariableContext, times(1)).setProperty("foo", "bar", false);
    }

    @Test
    public void addToIfExists_shouldNotAddEnvironmentVariableToEnvironmentVariableContextWhenVariableIDoesNotExistInContext() {
        final EnvironmentVariableContext environmentVariableContext = mock(EnvironmentVariableContext.class);
        final EnvironmentVariable environmentVariable = new EnvironmentVariable("foo", "bar");

        when(environmentVariableContext.hasProperty("foo")).thenReturn(false);

        environmentVariable.addToIfExists(environmentVariableContext);

        verify(environmentVariableContext, times(0)).setProperty("foo", "bar", false);
    }

    @Test
    public void getDisplayValue_shouldReturnMaskedValueIfVariableIsSecure() {
        final EnvironmentVariable environmentVariable = new EnvironmentVariable("foo", "bar", true);

        assertThat(environmentVariable.getDisplayValue(), is("****"));
    }

    @Test
    public void getDisplayValue_shouldReturnOriginalValueIfVariableIsNotSecure() {
        final EnvironmentVariable environmentVariable = new EnvironmentVariable("foo", "bar", false);

        assertThat(environmentVariable.getDisplayValue(), is("bar"));
    }
}