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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.policy.Allow;
import com.thoughtworks.go.config.policy.Policy;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoleConfigCommandTest {
    private Username currentUser;
    private GoConfigService goConfigService;
    private BasicCruiseConfig cruiseConfig;

    private AuthorizationExtension extension;

    @BeforeEach
    public void setUp() {
        currentUser = new Username("bob");
        goConfigService = mock(GoConfigService.class);
        extension = mock(AuthorizationExtension.class);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() {
        PluginRoleConfig role = new PluginRoleConfig("blackbird", "ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RoleConfigCommand command = new StubCommand(goConfigService, role, extension, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("foo")).isNull();

        assertThat(command.canContinue(cruiseConfig)).isFalse();
        assertThat(result.message()).isEqualTo(EntityType.Role.forbiddenToEdit(role.getName(), currentUser.getUsername()));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAuthorized() {
        PluginRoleConfig role = new PluginRoleConfig("blackbird", "ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RoleConfigCommand command = new StubCommand(goConfigService, role, extension, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("ldap")).isNull();

        assertThat(command.canContinue(cruiseConfig)).isTrue();
        assertThat(result.httpCode()).isEqualTo(200);
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsGroupAdmin() {
        PluginRoleConfig role = new PluginRoleConfig("blackbird", "ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RoleConfigCommand command = new StubCommand(goConfigService, role, extension, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("foo")).isNull();

        assertThat(command.canContinue(cruiseConfig)).isFalse();
        assertThat(result.message()).isEqualTo(EntityType.Role.forbiddenToEdit(role.getName(), currentUser.getUsername()));
    }

    @Test
    public void isValid_shouldValidateTheUpdatedRoleConfig() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PluginRoleConfig pluginRoleConfig = new PluginRoleConfig(null, "ldap");
        cruiseConfig.server().security().addRole(pluginRoleConfig);

        RoleConfigCommand command = new StubCommand(goConfigService, pluginRoleConfig, extension, currentUser, result);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(pluginRoleConfig.errors().size()).isEqualTo(2);
        assertThat(pluginRoleConfig.errors().get("name").getFirst()).isEqualTo("Invalid role name name 'null'. This must be " +
                "alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
        assertThat(pluginRoleConfig.errors().get("authConfigId").getFirst()).isEqualTo("No such security auth configuration present for id: `ldap`");
    }

    @Test
    public void isValid_shouldValidationRolesWithNonUniqueNamesAcrossPluginType() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PluginRoleConfig pluginRoleConfig = new PluginRoleConfig("test", "ldap");
        cruiseConfig.server().security().addRole(pluginRoleConfig);
        cruiseConfig.server().security().addRole(new RoleConfig(new CaseInsensitiveString("test")));

        RoleConfigCommand command = new StubCommand(goConfigService, pluginRoleConfig, extension, currentUser, result);

        assertFalse(command.isValid(cruiseConfig));
        assertThat(pluginRoleConfig.errors().size()).isEqualTo(2);
        assertThat(pluginRoleConfig.errors().get("name").getFirst()).isEqualTo("Role names should be unique. Role with the same name exists.");
    }

    @Test
    public void shouldPassValidationForValidRole() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PluginRoleConfig pluginRoleConfig = new PluginRoleConfig("foo", "ldap");
        cruiseConfig.server().security().addRole(pluginRoleConfig);
        cruiseConfig.server().security().securityAuthConfigs().add(new SecurityAuthConfig("ldap", "cd.go.ldap"));
        when(extension.validateRoleConfiguration(eq("cd.go.ldap"), anyMap())).thenReturn(new ValidationResult());

        RoleConfigCommand command = new StubCommand(goConfigService, pluginRoleConfig, extension, currentUser, result);
        boolean isValid = command.isValid(cruiseConfig);
        assertTrue(isValid);
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAdmin() {
        PluginRoleConfig role = new PluginRoleConfig("blackbird", "ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RoleConfigCommand command = new StubCommand(goConfigService, role, extension, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("ldap")).isNull();

        assertThat(command.canContinue(cruiseConfig)).isTrue();
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsGroupAdmin() {
        PluginRoleConfig role = new PluginRoleConfig("blackbird", "ldap");

        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RoleConfigCommand command = new StubCommand(goConfigService, role, extension, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("ldap")).isNull();

        assertThat(command.canContinue(cruiseConfig)).isFalse();
    }

    @Test
    public void shouldEncryptRoleConfig() throws CryptoException {
        setAuthorizationPluginInfo();
        cruiseConfig.server().security().securityAuthConfigs().add(new SecurityAuthConfig("ldap", "cd.go.github"));
        PluginRoleConfig role = new PluginRoleConfig("blackbird", "ldap");
        role.addConfigurations(List.of(
                new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("pub_v1")),
                new ConfigurationProperty(new ConfigurationKey("k2"), new ConfigurationValue("pub_v2")),
                new ConfigurationProperty(new ConfigurationKey("k3"), new ConfigurationValue("pub_v3"))));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        RoleConfigCommand command = new StubCommand(goConfigService, role, extension, currentUser, result);

        assertThat(role.getProperty("k1").getEncryptedValue()).isNull();
        assertThat(role.getProperty("k1").getConfigValue()).isEqualTo("pub_v1");
        assertThat(role.getProperty("k1").getValue()).isEqualTo("pub_v1");
        assertThat(role.getProperty("k2").getEncryptedValue()).isNull();
        assertThat(role.getProperty("k2").getConfigValue()).isEqualTo("pub_v2");
        assertThat(role.getProperty("k2").getValue()).isEqualTo("pub_v2");
        assertThat(role.getProperty("k3").getEncryptedValue()).isNull();
        assertThat(role.getProperty("k3").getConfigValue()).isEqualTo("pub_v3");
        assertThat(role.getProperty("k3").getValue()).isEqualTo("pub_v3");

        command.encrypt(cruiseConfig);

        GoCipher goCipher = new GoCipher();
        assertThat(role.getProperty("k1").getEncryptedValue()).isEqualTo(goCipher.encrypt("pub_v1"));
        assertThat(role.getProperty("k1").getConfigValue()).isNull();
        assertThat(role.getProperty("k1").getValue()).isEqualTo("pub_v1");
        assertThat(role.getProperty("k2").getEncryptedValue()).isNull();
        assertThat(role.getProperty("k2").getConfigValue()).isEqualTo("pub_v2");
        assertThat(role.getProperty("k2").getValue()).isEqualTo("pub_v2");
        assertThat(role.getProperty("k3").getEncryptedValue()).isEqualTo(goCipher.encrypt("pub_v3"));
        assertThat(role.getProperty("k3").getConfigValue()).isNull();
        assertThat(role.getProperty("k3").getValue()).isEqualTo("pub_v3");
    }

    @Test
    public void shouldNotPassValidationIfPolicyHasAnError() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Policy policy = new Policy();
        policy.add(new Allow("*", "*", "*"));
        RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("foo"), new Users(), policy);
        cruiseConfig.server().security().addRole(roleConfig);

        RoleConfigCommand command = new StubCommand(goConfigService, roleConfig, extension, currentUser, result);
        boolean isValid = command.isValid(cruiseConfig);
        assertFalse(isValid);
    }

    private void setAuthorizationPluginInfo() {
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);

        PluginConfiguration k1 = new PluginConfiguration("k1", new Metadata(false, true));
        PluginConfiguration k2 = new PluginConfiguration("k2", new Metadata(false, false));
        PluginConfiguration k3 = new PluginConfiguration("k3", new Metadata(false, true));

        PluggableInstanceSettings authConfigSettins = new PluggableInstanceSettings(List.of(k1, k2, k3));
        PluggableInstanceSettings roleConfigSettings = new PluggableInstanceSettings(List.of(k1, k2, k3));

        com.thoughtworks.go.plugin.domain.authorization.Capabilities capabilities = new com.thoughtworks.go.plugin.domain.authorization.Capabilities(SupportedAuthType.Web, true, true, true);
        AuthorizationPluginInfo artifactPluginInfo = new AuthorizationPluginInfo(pluginDescriptor, authConfigSettins, roleConfigSettings, null, capabilities);
        when(pluginDescriptor.id()).thenReturn("cd.go.github");
        AuthorizationMetadataStore.instance().setPluginInfo(artifactPluginInfo);
    }

    private static class StubCommand extends RoleConfigCommand {

        public StubCommand(GoConfigService goConfigService, Role role, AuthorizationExtension extension, Username currentUser, LocalizedOperationResult result) {
            super(goConfigService, role, currentUser, result);
        }

        @Override
        public void update(CruiseConfig preprocessedConfig) {

        }
    }
}
