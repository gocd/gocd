/*
 * Copyright 2017 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Admin;

import java.util.List;

@ConfigTag("role")
public class AdminRole implements Admin {
    @ConfigValue
    private CaseInsensitiveString name;
    private ConfigErrors configErrors = new ConfigErrors();
    public static final String ADMIN = "roles";

    public AdminRole() {
    }

    public AdminRole(final CaseInsensitiveString name) {
        this.name = name;
    }

    public AdminRole(final Role role) {
        this(role.getName());
    }

    public AdminRole(final String name) {
        this.name = new CaseInsensitiveString(name);
    }

    public boolean isSameAs(Admin admin, List<Role> memberRoles) {
        if (this.name == null || memberRoles == null) {
            return false;
        }
        for (Role memberRole : memberRoles) {
            if (name.equals(memberRole.getName())) {
                return true;
            }
        }
        return false;
    }

    public CaseInsensitiveString getName() {
        return name;
    }

    public String describe() {
        return "Role";
    }

    public boolean belongsTo(Role role) {
        return role.getName().equals(name);
    }

    public void addError(String message) {
        errors().add(NAME, message); // Do not remove this - The old view for editing group authorization, template authorization makes use of it.
        errors().add(ADMIN, message);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AdminRole adminRole = (AdminRole) o;

        return name.equals(adminRole.name);
    }

    public int hashCode() {
        return 31 * describe().hashCode() + (null != name ? name.hashCode() : 0);
    }

    public String toString() {
        return CaseInsensitiveString.str(name);
    }

    public void validate(ValidationContext validationContext) {
        if (validationContext.shouldNotCheckRole()) {
            return;
        }
        SecurityConfig securityConfig = validationContext.getServerSecurityConfig();
        if (!securityConfig.isRoleExist(this.name)) {
            // This is needed for the old UI while validating roles. Errors will be added on the name field.
            configErrors.add(NAME, String.format("Role \"%s\" does not exist.", this.name));
            configErrors.add(ADMIN, String.format("Role \"%s\" does not exist.", this.name));
        }
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

}
