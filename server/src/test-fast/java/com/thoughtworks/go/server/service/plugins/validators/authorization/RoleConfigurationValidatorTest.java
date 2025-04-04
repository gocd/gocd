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
package com.thoughtworks.go.server.service.plugins.validators.authorization;

import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class RoleConfigurationValidatorTest {

    private AuthorizationExtension extension;
    private RoleConfigurationValidator validator;

    @BeforeEach
    public void setUp() {
        extension = mock(AuthorizationExtension.class);
        validator = new RoleConfigurationValidator(extension);
        when(extension.validateRoleConfiguration(any(String.class), any())).thenReturn(new ValidationResult());
    }

    @Test
    public void shouldValidateRoleConfigurationWithPlugin() {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue("view"));
        PluginRoleConfig roleConfig = new PluginRoleConfig("admin", "auth_id", property);

        validator.validate(roleConfig, "pluginId");

        verify(extension).validateRoleConfiguration("pluginId", Map.of("username", "view"));
    }

    @Test
    public void shouldMapValidationErrorsToRoleConfiguration() {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue("view"));
        PluginRoleConfig roleConfig = new PluginRoleConfig("admin", "auth_id", property);
        ValidationResult result = new ValidationResult();

        result.addError(new ValidationError("username", "username format is incorrect"));
        when(extension.validateRoleConfiguration("pluginId", Map.of("username", "view"))).thenReturn(result);

        validator.validate(roleConfig, "pluginId");

        assertTrue(roleConfig.hasErrors());
        assertThat(roleConfig.getProperty("username").errors().get("username").get(0)).isEqualTo("username format is incorrect");
    }

    @Test
    public void shouldAddConfigurationAndMapErrorsInAbsenceOfConfiguration() {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue("view"));
        PluginRoleConfig roleConfig = new PluginRoleConfig("admin", "auth_id", property);
        ValidationResult result = new ValidationResult();

        result.addError(new ValidationError("password", "password is required"));
        when(extension.validateRoleConfiguration("pluginId", Map.of("username", "view"))).thenReturn(result);

        validator.validate(roleConfig, "pluginId");

        assertTrue(roleConfig.hasErrors());
        assertThat(roleConfig.getProperty("password").errors().get("password").get(0)).isEqualTo("password is required");
        assertNull(roleConfig.getProperty("password").getValue());
    }

    @Test
    public void shouldAddErrorsInAbsenceOfPlugin() {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue("view"));
        PluginRoleConfig roleConfig = new PluginRoleConfig("admin", "auth_id", property);

        when(extension.validateRoleConfiguration("pluginId", Map.of("username", "view"))).thenThrow(new RecordNotFoundException("not found"));

        validator.validate(roleConfig, "pluginId");

        assertTrue(roleConfig.hasErrors());
        assertThat(roleConfig.errors().get("pluginRole").get(0)).isEqualTo("Unable to validate `pluginRole` configuration, missing plugin: pluginId");
    }
}
