/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import java.util.List;
import java.util.ArrayList;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ObjectUtil;

@ConfigTag("security")
public class SecurityConfig implements Validatable {
    @ConfigSubtag(optional = true) private LdapConfig ldapConfig = new LdapConfig(new GoCipher());
    @ConfigSubtag(optional = true) private PasswordFileConfig passwordFileConfig = new PasswordFileConfig();
    @ConfigSubtag(optional = true) private RolesConfig rolesConfig = new RolesConfig();
    @ConfigSubtag(optional = true) private AdminsConfig adminsConfig = new AdminsConfig();
    @ConfigAttribute(value = "anonymous") private boolean anonymous = true;
    @ConfigAttribute(value = "allowOnlyKnownUsersToLogin") private boolean allowOnlyKnownUsersToLogin = false;
    private ConfigErrors errors = new ConfigErrors();

    public SecurityConfig() {
    }

    /*Dont chain constructors*/
    public SecurityConfig(LdapConfig ldapConfig, PasswordFileConfig passwordFileConfig, boolean allowOnlyKnownUsersToLogin) {
        this.ldapConfig = ldapConfig;
        this.passwordFileConfig = passwordFileConfig;
        this.allowOnlyKnownUsersToLogin = allowOnlyKnownUsersToLogin;
    }

    public SecurityConfig(LdapConfig ldap, PasswordFileConfig pwordFile, boolean anonymous, AdminsConfig admins) {
        this.ldapConfig = ldap;
        this.passwordFileConfig = pwordFile;
        this.anonymous = anonymous;
        this.adminsConfig = admins;
    }

    public SecurityConfig(LdapConfig ldapConfig, PasswordFileConfig passwordFileConfig, boolean anonymous, AdminsConfig adminsConfig, boolean allowOnlyKnownUsersToLogin) {
        this.ldapConfig = ldapConfig;
        this.anonymous = anonymous;
        this.adminsConfig = adminsConfig;
        this.passwordFileConfig = passwordFileConfig;
        this.allowOnlyKnownUsersToLogin = allowOnlyKnownUsersToLogin;
    }

    public LdapConfig ldapConfig() {
        return ldapConfig;
    }

    public boolean isSecurityEnabled() {
        boolean ldapEnabled = ldapConfig != null && ldapConfig.isEnabled();
        boolean passwordFileEnabled = passwordFileConfig != null && passwordFileConfig.isEnabled();
        return ldapEnabled || passwordFileEnabled;
    }

    public boolean anonymousAccess() {
        return anonymous;
    }

    public AdminsConfig adminsConfig() {
        return adminsConfig;
    }

    public PasswordFileConfig passwordFileConfig() {
        return passwordFileConfig;
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

    public boolean hasSecurityMethodChanged(SecurityConfig newSecurity) {
        if (newSecurity == null) {
            return true;
        }
        boolean ldapChanged = !ObjectUtil.equal(ldapConfig, newSecurity.ldapConfig());
        boolean passwordFileChanged = !ObjectUtil.equal(passwordFileConfig, newSecurity.passwordFileConfig());
        return ldapChanged || passwordFileChanged;
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

    public void modifyLdap(LdapConfig ldapConfig) {
        this.ldapConfig.updateWithNew(ldapConfig);
    }

    public void modifyPasswordFile(PasswordFileConfig passwordConfig) {
        this.passwordFileConfig = passwordConfig;
    }

    public void modifyAllowOnlyKnownUsers(boolean shouldAllow) {
        this.allowOnlyKnownUsersToLogin = shouldAllow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecurityConfig)) {
            return false;
        }

        SecurityConfig that = (SecurityConfig) o;

        if (allowOnlyKnownUsersToLogin != that.allowOnlyKnownUsersToLogin) {
            return false;
        }
        if (anonymous != that.anonymous) {
            return false;
        }
        if (adminsConfig != null ? !adminsConfig.equals(that.adminsConfig) : that.adminsConfig != null) {
            return false;
        }
        if (ldapConfig != null ? !ldapConfig.equals(that.ldapConfig) : that.ldapConfig != null) {
            return false;
        }
        if (passwordFileConfig != null ? !passwordFileConfig.equals(that.passwordFileConfig) : that.passwordFileConfig != null) {
            return false;
        }
        if (rolesConfig != null ? !rolesConfig.equals(that.rolesConfig) : that.rolesConfig != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = ldapConfig != null ? ldapConfig.hashCode() : 0;
        result = 31 * result + (passwordFileConfig != null ? passwordFileConfig.hashCode() : 0);
        result = 31 * result + (rolesConfig != null ? rolesConfig.hashCode() : 0);
        result = 31 * result + (adminsConfig != null ? adminsConfig.hashCode() : 0);
        result = 31 * result + (anonymous ? 1 : 0);
        result = 31 * result + (allowOnlyKnownUsersToLogin ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("SecurityConfig{ldapConfig=%s, passwordFileConfig=%s, rolesConfig=%s, adminsConfig=%s, anonymous=%s, allowOnlyKnownUsersToLogin=%s}", ldapConfig, passwordFileConfig,
                rolesConfig, adminsConfig, anonymous, allowOnlyKnownUsersToLogin);
    }

    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return errors;
    }

    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }
}
