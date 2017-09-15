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

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Admin;

import java.util.ArrayList;
import java.util.List;

@ConfigTag("security")
public class SecurityConfig implements Validatable {
    @ConfigSubtag(optional = true)
    private SecurityAuthConfigs securityAuthConfigs = new SecurityAuthConfigs();
    @ConfigSubtag(optional = true)
    private RolesConfig rolesConfig = new RolesConfig();
    @ConfigSubtag(optional = true)
    private AdminsConfig adminsConfig = new AdminsConfig();
    @ConfigAttribute(value = "allowOnlyKnownUsersToLogin")
    private boolean allowOnlyKnownUsersToLogin = false;
    private ConfigErrors errors = new ConfigErrors();

    public SecurityConfig() {
    }

    /*Dont chain constructors*/
    public SecurityConfig(boolean allowOnlyKnownUsersToLogin) {
        this.allowOnlyKnownUsersToLogin = allowOnlyKnownUsersToLogin;
    }

    public SecurityConfig(AdminsConfig admins) {
        this.adminsConfig = admins;
    }

    public SecurityConfig(AdminsConfig adminsConfig, boolean allowOnlyKnownUsersToLogin) {
        this.adminsConfig = adminsConfig;
        this.allowOnlyKnownUsersToLogin = allowOnlyKnownUsersToLogin;
    }

    public boolean isSecurityEnabled() {
        return securityAuthConfigs != null && !securityAuthConfigs.isEmpty();
    }

    public AdminsConfig adminsConfig() {
        return adminsConfig;
    }

    public RolesConfig getRoles() {
        return rolesConfig;
    }

    public Role roleNamed(String roleName) {
        return rolesConfig.findByName(new CaseInsensitiveString(roleName));
    }

    public void addRole(Role role) {
        rolesConfig.add(role);
    }

    public boolean deleteRole(Role role) {
        return rolesConfig.remove(role);
    }

    public boolean deleteRole(String roleName) {
        return rolesConfig.remove(roleNamed(roleName));
    }

    public boolean isRoleExist(final CaseInsensitiveString role) {
        return rolesConfig.isRoleExist(role);
    }

    public boolean isAdmin(Admin admin) {
        return !isSecurityEnabled() || noAdminsRequired() || adminsConfig.isAdmin(admin, rolesConfig.memberRoles(admin));
    }

    private boolean noAdminsRequired() {
        return adminsConfig == null || adminsConfig.isEmpty();
    }

    public boolean isUserMemberOfRole(final CaseInsensitiveString userName, final CaseInsensitiveString roleName) {
        return rolesConfig.isUserMemberOfRole(userName, roleName);
    }

    public List<Role> memberRoleFor(final CaseInsensitiveString userName) {
        List<Role> roles = new ArrayList<>();
        for (Role role : rolesConfig) {
            if (isUserMemberOfRole(userName, role.getName())) {
                roles.add(role);
            }
        }
        return roles;
    }

    public void setAllowOnlyKnownUsersToLogin(boolean value) {
        this.allowOnlyKnownUsersToLogin = value;
    }

    public boolean isAllowOnlyKnownUsersToLogin() {
        return this.allowOnlyKnownUsersToLogin;
    }

    public void modifyAllowOnlyKnownUsers(boolean shouldAllow) {
        this.allowOnlyKnownUsersToLogin = shouldAllow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SecurityConfig that = (SecurityConfig) o;

        if (allowOnlyKnownUsersToLogin != that.allowOnlyKnownUsersToLogin) return false;
        if (securityAuthConfigs != null ? !securityAuthConfigs.equals(that.securityAuthConfigs) : that.securityAuthConfigs != null)
            return false;
        if (rolesConfig != null ? !rolesConfig.equals(that.rolesConfig) : that.rolesConfig != null) return false;
        if (adminsConfig != null ? !adminsConfig.equals(that.adminsConfig) : that.adminsConfig != null) return false;
        return errors != null ? errors.equals(that.errors) : that.errors == null;
    }

    @Override
    public int hashCode() {
        int result = securityAuthConfigs != null ? securityAuthConfigs.hashCode() : 0;
        result = 31 * result + (rolesConfig != null ? rolesConfig.hashCode() : 0);
        result = 31 * result + (adminsConfig != null ? adminsConfig.hashCode() : 0);
        result = 31 * result + (allowOnlyKnownUsersToLogin ? 1 : 0);
        result = 31 * result + (errors != null ? errors.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("SecurityConfig{securityAuthConfigs=%s, rolesConfig=%s, adminsConfig=%s, allowOnlyKnownUsersToLogin=%s}", securityAuthConfigs, rolesConfig, adminsConfig, allowOnlyKnownUsersToLogin);
    }

    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return errors;
    }

    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    public SecurityAuthConfigs securityAuthConfigs() {
        return this.securityAuthConfigs;
    }

    public List<PluginRoleConfig> getPluginRoles(String pluginId) {
        List<PluginRoleConfig> result = new ArrayList<>();

        List<SecurityAuthConfig> authConfigs = securityAuthConfigs.findByPluginId(pluginId);
        List<PluginRoleConfig> pluginRoles = rolesConfig.getPluginRoleConfigs();

        for (SecurityAuthConfig authConfig : authConfigs) {
            for (PluginRoleConfig pluginRole : pluginRoles) {
                if (pluginRole.getAuthConfigId().equals(authConfig.getId())) {
                    result.add(pluginRole);
                }
            }
        }
        return result;
    }
}
