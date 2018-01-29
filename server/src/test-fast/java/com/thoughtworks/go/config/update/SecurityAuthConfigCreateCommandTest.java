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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecurityAuthConfigCreateCommandTest {

    private AuthorizationExtension extension;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        extension = mock(AuthorizationExtension.class);
    }

    @Test
    public void shouldAddSecurityAuthConfig() throws Exception {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        SecurityAuthConfig authConfig = new SecurityAuthConfig("foo", "ldap");
        SecurityAuthConfigCreateCommand command = new SecurityAuthConfigCreateCommand(null, authConfig, extension, null, null);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("foo"), equalTo(authConfig));
    }

    @Test
    public void shouldInvokePluginValidationsBeforeSave() {
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("key", "some-error"));
        SecurityAuthConfig newProfile = new SecurityAuthConfig("foo", "aws", new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("val")));
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.server().security().securityAuthConfigs().add(newProfile);

        when(extension.validateAuthConfig(eq("aws"), Matchers.<Map<String, String>>any())).thenReturn(validationResult);

        PluginProfileCommand command = new SecurityAuthConfigCreateCommand(mock(GoConfigService.class), newProfile, extension, null, new HttpLocalizedOperationResult());

        command.isValid(cruiseConfig);

        assertThat(newProfile.first().errors().size(), is(1));
        assertThat(newProfile.first().errors().on("key"), is("some-error"));
    }
}
