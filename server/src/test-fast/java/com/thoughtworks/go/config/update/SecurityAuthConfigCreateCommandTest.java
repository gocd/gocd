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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecurityAuthConfigCreateCommandTest {

    private AuthorizationExtension extension;

    @BeforeEach
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
    public void shouldInvokePluginValidationsBeforeSave() throws Exception {
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("key", "error"));
        when(extension.validateAuthConfig(eq("aws"), anyMap())).thenReturn(validationResult);
        SecurityAuthConfig newProfile = new SecurityAuthConfig("foo", "aws", new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("val")));
        PluginProfileCommand command = new SecurityAuthConfigCreateCommand(mock(GoConfigService.class), newProfile, extension, null, new HttpLocalizedOperationResult());
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();

        assertThatThrownBy(() -> command.isValid(cruiseConfig))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessageContaining(EntityType.SecurityAuthConfig.notFoundMessage(newProfile.getId()));
    }

}
