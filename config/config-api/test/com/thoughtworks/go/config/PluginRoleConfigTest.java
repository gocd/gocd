/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.helper.ValidationContextMother;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class PluginRoleConfigTest {
    @Test
    public void validate_shouldValidatePresenceOfRoleName(){
        validatePresenceOfRoleName(new Validator() {
            @Override
            public void validate(PluginRoleConfig pluginRoleConfig, ValidationContext context) {
                pluginRoleConfig.validate(context);
            }
        });
    }

    @Test
    public void validate_shouldValidateNullRoleName(){
        validateNullRoleName(new Validator() {
            @Override
            public void validate(PluginRoleConfig pluginRoleConfig, ValidationContext context) {
                pluginRoleConfig.validate(context);
            }
        });
    }

    @Test
    public void validate_presenceAuthConfigId(){
        validatePresenceAuthConfigId(new Validator() {
            @Override
            public void validate(PluginRoleConfig pluginRoleConfig, ValidationContext context) {
                pluginRoleConfig.validate(context);
            }
        });
    }

    @Test
    public void validate_presenceOfAuthConfigIdInSecurityConfig() throws Exception {
        validatePresenceOfAuthConfigIdInSecurityConfig(new Validator() {
            @Override
            public void validate(PluginRoleConfig pluginRoleConfig, ValidationContext context) {
                pluginRoleConfig.validate(context);
            }
        });
    }

    @Test
    public void validate_uniquenessOfRoleName() throws Exception {
        validateUniquenessOfRoleName(new Validator() {
            @Override
            public void validate(PluginRoleConfig pluginRoleConfig, ValidationContext context) {
                pluginRoleConfig.validate(context);
            }
        });
    }

    @Test
    public void validateTree_shouldValidatePresenceOfRoleName(){
        validatePresenceOfRoleName(new Validator() {
            @Override
            public void validate(PluginRoleConfig pluginRoleConfig, ValidationContext context) {
                assertFalse(pluginRoleConfig.validateTree(context));
            }
        });
    }

    @Test
    public void validateTree_shouldValidateNullRoleName(){
        validateNullRoleName(new Validator() {
            @Override
            public void validate(PluginRoleConfig pluginRoleConfig, ValidationContext context) {
                pluginRoleConfig.validateTree(context);
            }
        });
    }

    @Test
    public void validateTree_presenceAuthConfigId(){
        validatePresenceAuthConfigId(new Validator() {
            @Override
            public void validate(PluginRoleConfig pluginRoleConfig, ValidationContext context) {
                assertFalse(pluginRoleConfig.validateTree(context));
            }
        });
    }

    @Test
    public void validateTree_presenceOfAuthConfigIdInSecurityConfig() throws Exception {
        validatePresenceOfAuthConfigIdInSecurityConfig(new Validator() {
            @Override
            public void validate(PluginRoleConfig pluginRoleConfig, ValidationContext context) {
                assertFalse(pluginRoleConfig.validateTree(context));
            }
        });
    }

    @Test
    public void validateTree_uniquenessOfRoleName() throws Exception {
        validateUniquenessOfRoleName(new Validator() {
            @Override
            public void validate(PluginRoleConfig pluginRoleConfig, ValidationContext context) {
                assertFalse(pluginRoleConfig.validateTree(context));
            }
        });
    }

    @Test
    public void hasErrors_shouldBeTrueIfRoleHasErrors() throws Exception {
        Role role = new PluginRoleConfig("", "auth_config_id");

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("auth_config_id", "plugin_id"));

        role.validate(ValidationContextMother.validationContext(securityConfig));

        assertTrue(role.hasErrors());
    }

    @Test
    public void hasErrors_shouldBeTrueIfConfigurationPropertiesHasErrors() throws Exception {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue("view"));
        PluginRoleConfig roleConfig = new PluginRoleConfig("admin", "auth_id", property);

        property.addError("username", "username format is incorrect");

        assertTrue(roleConfig.hasErrors());
        assertTrue(roleConfig.errors().isEmpty());
    }

    private void validatePresenceOfRoleName(Validator v) {
        PluginRoleConfig role = new PluginRoleConfig("", "auth_config_id");

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("auth_config_id", "plugin_id"));

        v.validate(role, ValidationContextMother.validationContext(securityConfig));

        assertTrue(role.hasErrors());
        assertThat(role.errors().size(), is(1));
        assertThat(role.errors().get("name").get(0), is("Invalid role name name ''. This must be alphanumeric and can" +
                " contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    private void validateNullRoleName(Validator v) {
        PluginRoleConfig role = new PluginRoleConfig("", "auth_config_id");
        role.setName(null);

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("auth_config_id", "plugin_id"));

        v.validate(role, ValidationContextMother.validationContext(securityConfig));

        assertTrue(role.hasErrors());
        assertThat(role.errors().size(), is(1));
        assertThat(role.errors().get("name").get(0), is("Invalid role name name 'null'. This must be alphanumeric and can" +
                " contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    public void validatePresenceAuthConfigId(Validator v){
        PluginRoleConfig role = new PluginRoleConfig("admin", "");

        SecurityConfig securityConfig = new SecurityConfig();

        v.validate(role, ValidationContextMother.validationContext(securityConfig));

        assertThat(role.errors().size(), is(1));
        assertThat(role.errors().get("authConfigId").size(), is(1));
        assertThat(role.errors().get("authConfigId").get(0), is("Invalid plugin role authConfigId name ''. This must be alphanumeric and can" +
                " contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    public void validatePresenceOfAuthConfigIdInSecurityConfig(Validator v) throws Exception {
        PluginRoleConfig role = new PluginRoleConfig("admin", "auth_config_id");
        SecurityConfig securityConfig = new SecurityConfig();

        v.validate(role, ValidationContextMother.validationContext(securityConfig));

        assertThat(role.errors().size(), is(1));
        assertThat(role.errors().get("authConfigId").size(), is(1));
        assertThat(role.errors().get("authConfigId").get(0), is("No such security auth configuration present for id: `auth_config_id`"));
    }

    public void validateUniquenessOfRoleName(Validator v) throws Exception {
        PluginRoleConfig role = new PluginRoleConfig("admin", "auth_config_id");
        SecurityConfig securityConfig = new SecurityConfig();
        ValidationContext validationContext = ValidationContextMother.validationContext(securityConfig);

        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("auth_config_id", "plugin_id"));
        securityConfig.getRoles().add(new RoleConfig(new CaseInsensitiveString("admin")));
        securityConfig.getRoles().add(role);

        v.validate(role, validationContext);

        assertThat(role.errors().size(), is(1));
        assertThat(role.errors().get("name").get(0), is("Role names should be unique. Role with the same name exists."));
    }

    interface Validator {
        void validate(PluginRoleConfig pluginRoleConfig, ValidationContext context);
    }
}