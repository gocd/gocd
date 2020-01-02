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
package com.thoughtworks.go.config.validation;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TokenGenerationKeyImmutabilityValidatorTest {
    private TokenGenerationKeyImmutabilityValidator tokenGenerationKeyImmutabilityValidator;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        final SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.enforceServerImmutability()).thenReturn(true);

        tokenGenerationKeyImmutabilityValidator = new TokenGenerationKeyImmutabilityValidator(systemEnvironment);
    }

    @Test
    public void shouldRememberTokenGenerationKeyOnStartup() throws Exception {
        final BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();

        tokenGenerationKeyImmutabilityValidator.validate(cruiseConfig);

        assertThat(tokenGenerationKeyImmutabilityValidator.getTokenGenerationKey(), is(cruiseConfig.server().getTokenGenerationKey()));
    }

    @Test
    public void shouldErrorOutIfTokenGenerationKeyIsChanged() throws Exception {
        final BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();

        tokenGenerationKeyImmutabilityValidator.validate(cruiseConfig);
        assertThat(tokenGenerationKeyImmutabilityValidator.getTokenGenerationKey(), is(cruiseConfig.server().getTokenGenerationKey()));

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("The value of 'tokenGenerationKey' cannot be modified while the server is online. If you really want to make this change, you may do so while the server is offline. Please note: updating 'tokenGenerationKey' will invalidate all registration tokens issued to the agents so far.");

        tokenGenerationKeyImmutabilityValidator.validate(GoConfigMother.defaultCruiseConfig());
    }

    @Test
    public void shouldAllowSaveIfTokenGenerationKeyIsUnChanged() throws Exception {
        final BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();

        tokenGenerationKeyImmutabilityValidator.validate(cruiseConfig);
        assertThat(tokenGenerationKeyImmutabilityValidator.getTokenGenerationKey(), is(cruiseConfig.server().getTokenGenerationKey()));

        cruiseConfig.server().useSecurity(new SecurityConfig());

        tokenGenerationKeyImmutabilityValidator.validate(cruiseConfig);
        assertThat(tokenGenerationKeyImmutabilityValidator.getTokenGenerationKey(), is(cruiseConfig.server().getTokenGenerationKey()));
    }
}
