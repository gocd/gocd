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
package com.thoughtworks.go.config;

import com.thoughtworks.go.helper.SecurityConfigMother;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class SecurityConfigTest {

    @Test
    public void shouldNotSaySecurityEnabledIfSecurityHasNoAuthenticatorsDefined() {
        ServerConfig serverConfig = new ServerConfig();
        assertFalse(serverConfig.isSecurityEnabled(), "Security should not be enabled by default");
    }

    @Test
    public void twoEmptySecurityConfigsShouldBeTheSame() {
        SecurityConfig one = new SecurityConfig();
        SecurityConfig two = new SecurityConfig();
        assertThat(one).isEqualTo(two);
    }

    @Test
    public void shouldSaySecurityEnabledIfPasswordFileSecurityEnabled() {
        ServerConfig serverConfig = server(SecurityConfigMother.passwordFileAuthConfig(), SecurityConfigMother.admins());
        assertTrue(serverConfig.isSecurityEnabled(), "Security should be enabled when password file config present");
    }

    @Test
    public void shouldKnowIfUserIsAdmin() {
        SecurityConfig security = SecurityConfigMother.security(null, SecurityConfigMother.admins(SecurityConfigMother.user("chris")));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("chris")))).isTrue();
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("evilHacker")))).isTrue();

        security = SecurityConfigMother.security(SecurityConfigMother.passwordFileAuthConfig(), SecurityConfigMother.admins(SecurityConfigMother.user("chris")));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("chris")))).isTrue();
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("evilHacker")))).isFalse();
    }

    @Test
    public void shouldKnowIfRoleIsAdmin() {
        SecurityConfig security = SecurityConfigMother.security(SecurityConfigMother.passwordFileAuthConfig(), SecurityConfigMother.admins(SecurityConfigMother.role("role1")));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("chris")))).isTrue();
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("jez")))).isTrue();
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("evilHacker")))).isFalse();
    }

    @Test
    public void shouldNotCareIfValidUserInRoleOrUser() {
        SecurityConfig security = SecurityConfigMother.security(SecurityConfigMother.passwordFileAuthConfig(), SecurityConfigMother.admins(SecurityConfigMother.role("role2")));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("chris")))).isTrue();
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("jez")))).isFalse();

        security = SecurityConfigMother.security(SecurityConfigMother.passwordFileAuthConfig(), SecurityConfigMother.admins(SecurityConfigMother.role("role2"), SecurityConfigMother.user("jez")));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("chris")))).isTrue();
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("jez")))).isTrue();
    }

    @Test
    public void shouldValidateRoleAsAdmin() {
        SecurityConfig security = SecurityConfigMother.security(SecurityConfigMother.passwordFileAuthConfig(), SecurityConfigMother.admins(SecurityConfigMother.role("role2")));
        assertThat(security.isAdmin(new AdminRole(new CaseInsensitiveString("role2")))).isTrue();
    }

    @Test
    public void shouldReturnTheMemberRoles() {
        SecurityConfig securityConfig = SecurityConfigMother.security(SecurityConfigMother.passwordFileAuthConfig(), SecurityConfigMother.admins());
        assertUserRoles(securityConfig, "chris", SecurityConfigMother.DEFAULT_ROLES);
        assertUserRoles(securityConfig, "jez", SecurityConfigMother.DEFAULT_ROLES[0]);
        assertUserRoles(securityConfig, "loser");
    }

    private void assertUserRoles(SecurityConfig securityConfig, String username, Role... roles) {
        assertThat(securityConfig.memberRoleFor(new CaseInsensitiveString(username))).isEqualTo(Arrays.asList(roles));
    }

    private ServerConfig server(SecurityAuthConfig passwordFile, AdminsConfig admins) {
        return new ServerConfig("", SecurityConfigMother.security(passwordFile, admins));
    }

    @Test
    public void shouldGetPluginRolesWhichBelongsToSpecifiedPlugin() {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.addRole(new PluginRoleConfig("foo", "ldap"));
        securityConfig.addRole(new PluginRoleConfig("bar", "github"));
        securityConfig.addRole(new RoleConfig(new CaseInsensitiveString("xyz")));

        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", "cd.go.ldap"));
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.github"));


        List<PluginRoleConfig> pluginRolesConfig = securityConfig.getPluginRoles("cd.go.ldap");

        assertThat(pluginRolesConfig).hasSize(1);
        assertThat(pluginRolesConfig).contains(new PluginRoleConfig("foo", "ldap"));
    }

    @Test
    public void getPluginRolesConfig_shouldReturnNothingWhenBadPluginIdSpecified() {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.addRole(new PluginRoleConfig("foo", "ldap"));
        securityConfig.addRole(new PluginRoleConfig("bar", "github"));
        securityConfig.addRole(new RoleConfig(new CaseInsensitiveString("xyz")));
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", "cd.go.ldap"));
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.github"));

        List<PluginRoleConfig> pluginRolesConfig = securityConfig.getPluginRoles("non-existant-plugin");

        assertThat(pluginRolesConfig).hasSize(0);
    }

    @Test
    public void getPluginRole_shouldReturnPluginRoleMatchingTheGivenName() {
        PluginRoleConfig role = new PluginRoleConfig("foo", "ldap");
        SecurityConfig securityConfig = new SecurityConfig();

        securityConfig.addRole(role);

        assertThat(securityConfig.getPluginRole(new CaseInsensitiveString("FOO"))).isEqualTo(role);
    }

    @Test
    public void getPluginRole_shouldReturnNullInAbsenceOfPluginRoleForTheGivenName() {
        SecurityConfig securityConfig = new SecurityConfig();

        assertNull(securityConfig.getPluginRole(new CaseInsensitiveString("foo")));
    }
}
