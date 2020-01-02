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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Admin;

import static java.util.Arrays.asList;

@ConfigTag("admins")
@ConfigCollection(Admin.class)
public class AdminsConfig extends BaseCollection<Admin> implements Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    public AdminsConfig() {
    }

    public AdminsConfig(Admin... admins) {
        addAll(asList(admins));
    }

    public AdminsConfig(Set<Admin> admins) {
        addAll(admins);
    }

    public boolean isAdmin(Admin username, List<Role> memberRoles) {
        for (Admin admin : this) {
            if (admin.isSameAs(username, memberRoles)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasUser(final CaseInsensitiveString username, UserRoleMatcher userRoleMatcher) {
        for (Admin admin : this) {
            if (admin instanceof AdminUser) {
                if (admin.getName().equals(username)) {
                    return true;
                }
            } else {
                if (userRoleMatcher.match(username, admin.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean has(Admin admin, List<Role> rolesConfig) {
        for (Admin configured : this) {
            if (configured.isSameAs(admin, rolesConfig)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasErrors() {
        return !configErrors.isEmpty();
    }

    @Override
    public void validate(ValidationContext validationContext) {
        return;
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public boolean isAdminRole(List<Role> rolesUserHas) {
        return has(null, rolesUserHas);
    }

    public List<AdminUser> getUsers() {
        List<AdminUser> users = new ArrayList<>();
        for (Admin admin : this) {
            if (admin instanceof AdminUser) {
                users.add((AdminUser) admin);
            }
        }
        return users;
    }

    public List<AdminRole> getRoles() {
        List<AdminRole> roles = new ArrayList<>();
        for (Admin admin : this) {
            if (admin instanceof AdminRole) {
                roles.add((AdminRole) admin);
            }
        }
        return roles;
    }

    public void removeRole(Role role) {
        this.remove(new AdminRole(role));
    }

    public void addRole(AdminRole adminRole) {
        add(adminRole);
    }

    public boolean validateTree(ValidationContext validationContextWithSecurityConfig) {
        this.stream().forEach(admin -> {
            admin.validate(validationContextWithSecurityConfig);
            configErrors.addAll(admin.errors());
        });

        configErrors.remove("name");

        return !hasErrors();
    }
}
