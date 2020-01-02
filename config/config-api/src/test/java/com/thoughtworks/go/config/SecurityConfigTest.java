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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.config.Admin;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

public class SecurityConfigTest {

    public static final Role ROLE1 = new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(new CaseInsensitiveString("chris")), new RoleUser(new CaseInsensitiveString("jez")));
    public static final Role ROLE2 = new RoleConfig(new CaseInsensitiveString("role2"), new RoleUser(new CaseInsensitiveString("chris")));
    public static final Role[] DEFAULT_ROLES = new Role[]{ROLE1, ROLE2};

    @Test
    public void shouldNotSaySecurityEnabledIfSecurityHasNoAuthenticatorsDefined() {
        ServerConfig serverConfig = new ServerConfig();
        assertFalse("Security should not be enabled by default", serverConfig.isSecurityEnabled());
    }

    @Test
    public void twoEmptySecurityConfigsShouldBeTheSame() throws Exception {
        SecurityConfig one = new SecurityConfig();
        SecurityConfig two = new SecurityConfig();
        assertThat(one, is(two));
    }

    @Test
    public void shouldSaySecurityEnabledIfPasswordFileSecurityEnabled() {
        ServerConfig serverConfig = server(passwordFileAuthConfig(), admins());
        assertTrue("Security should be enabled when password file config present", serverConfig.isSecurityEnabled());
    }

    @Test
    public void shouldKnowIfUserIsAdmin() throws Exception {
        SecurityConfig security = security(null, admins(user("chris")));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("chris"))), is(true));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("evilHacker"))), is(true));

        security = security(passwordFileAuthConfig(), admins(user("chris")));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("chris"))), is(true));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("evilHacker"))), is(false));
    }

    @Test
    public void shouldKnowIfRoleIsAdmin() throws Exception {
        SecurityConfig security = security(passwordFileAuthConfig(), admins(role("role1")));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("chris"))), is(true));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("jez"))), is(true));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("evilHacker"))), is(false));
    }

    @Test
    public void shouldNotCareIfValidUserInRoleOrUser() throws Exception {
        SecurityConfig security = security(passwordFileAuthConfig(), admins(role("role2")));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("chris"))), is(true));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("jez"))), is(false));

        security = security(passwordFileAuthConfig(), admins(role("role2"), user("jez")));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("chris"))), is(true));
        assertThat(security.isAdmin(new AdminUser(new CaseInsensitiveString("jez"))), is(true));
    }

    @Test
    public void shouldValidateRoleAsAdmin() throws Exception {
        SecurityConfig security = security(passwordFileAuthConfig(), admins(role("role2")));
        assertThat(security.isAdmin(new AdminRole(new CaseInsensitiveString("role2"))), is(true));
    }

    @Test
    public void shouldReturnTheMemberRoles() throws Exception {
        SecurityConfig securityConfig = security(passwordFileAuthConfig(), admins());
        assertUserRoles(securityConfig, "chris", DEFAULT_ROLES);
        assertUserRoles(securityConfig, "jez", DEFAULT_ROLES[0]);
        assertUserRoles(securityConfig, "loser");
    }

    @Test
    public void shouldReturnTrueIfDeletingARoleGoesThroughSuccessfully() throws Exception {
        SecurityConfig securityConfig = security(passwordFileAuthConfig(), admins());
        securityConfig.deleteRole(ROLE1);

        assertUserRoles(securityConfig, "chris", ROLE2);
        assertUserRoles(securityConfig, "jez");
    }

    @Test
    public void shouldBombIfDeletingARoleWhichDoesNotExist() throws Exception {
        try {
            SecurityConfig securityConfig = security(passwordFileAuthConfig(), admins());
            securityConfig.deleteRole(new RoleConfig(new CaseInsensitiveString("role99")));
            fail("Should have blown up with an exception on the previous line as deleting role99 should blow up");
        } catch (RuntimeException e) {
            assertTrue(Pattern.compile("does not exist").matcher(e.getMessage()).find());
        }
    }

    private void assertUserRoles(SecurityConfig securityConfig, String username, Role... roles) {
        assertThat(securityConfig.memberRoleFor(new CaseInsensitiveString(username)), is(Arrays.asList(roles)));
    }

    private ServerConfig server(SecurityAuthConfig passwordFile, AdminsConfig admins) {
        return new ServerConfig("", security(passwordFile, admins));
    }

    public static SecurityConfig security(SecurityAuthConfig securityAuthConfig, AdminsConfig admins) {
        final SecurityConfig security = new SecurityConfig(admins);

        if (securityAuthConfig != null) {
            security.securityAuthConfigs().add(securityAuthConfig);
        }

        for (Role role : DEFAULT_ROLES) {
            security.addRole(role);
        }
        return security;
    }

    public static AdminsConfig admins(Admin... admins) {
        return new AdminsConfig(admins);
    }

    public static AdminUser user(String name) {
        return new AdminUser(new CaseInsensitiveString(name));
    }

    public static AdminRole role(String name) {
        return new AdminRole(new CaseInsensitiveString(name));
    }

    public static SecurityAuthConfig passwordFileAuthConfig() {
        return new SecurityAuthConfig("file", "cd.go.authentication.passwordfile");
    }

    @Test
    public void shouldGetPluginRolesWhichBelogsToSpecifiedPlugin() throws Exception {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.addRole(new PluginRoleConfig("foo", "ldap"));
        securityConfig.addRole(new PluginRoleConfig("bar", "github"));
        securityConfig.addRole(new RoleConfig(new CaseInsensitiveString("xyz")));

        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", "cd.go.ldap"));
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.github"));


        List<PluginRoleConfig> pluginRolesConfig = securityConfig.getPluginRoles("cd.go.ldap");

        assertThat(pluginRolesConfig, hasSize(1));
        assertThat(pluginRolesConfig, contains(new PluginRoleConfig("foo", "ldap")));
    }

    @Test
    public void getPluginRolesConfig_shouldReturnNothingWhenBadPluginIdSpecified() throws Exception {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.addRole(new PluginRoleConfig("foo", "ldap"));
        securityConfig.addRole(new PluginRoleConfig("bar", "github"));
        securityConfig.addRole(new RoleConfig(new CaseInsensitiveString("xyz")));
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", "cd.go.ldap"));
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.github"));

        List<PluginRoleConfig> pluginRolesConfig = securityConfig.getPluginRoles("non-existant-plugin");

        assertThat(pluginRolesConfig, hasSize(0));
    }

    @Test
    public void getPluginRole_shouldReturnPluginRoleMatchingTheGivenName() throws Exception {
        PluginRoleConfig role = new PluginRoleConfig("foo", "ldap");
        SecurityConfig securityConfig = new SecurityConfig();

        securityConfig.addRole(role);

        assertThat(securityConfig.getPluginRole(new CaseInsensitiveString("FOO")), is(role));
    }

    @Test
    public void getPluginRole_shouldReturnNullInAbsenceOfPluginRoleForTheGivenName() throws Exception {
        SecurityConfig securityConfig = new SecurityConfig();

        assertNull(securityConfig.getPluginRole(new CaseInsensitiveString("foo")));
    }
}
