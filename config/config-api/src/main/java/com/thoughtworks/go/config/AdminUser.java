/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import java.util.List;

@ConfigTag("user")
public class AdminUser implements Admin {
    @ConfigValue
    private CaseInsensitiveString name;

    private final ConfigErrors configErrors = new ConfigErrors();

    public static final String ADMIN = "users";

    public AdminUser() {
    }

    public AdminUser(final CaseInsensitiveString name) {
        this.name = name;
    }

    public AdminUser(String name) {
        this.name = new CaseInsensitiveString(name);
    }

    public void validate(ValidationContext validationContext) {
        if (name == null || name.isBlank())
            addError("User cannot be blank.");
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public CaseInsensitiveString getName() {
        return name;
    }

    public String describe() {
        return "User";
    }

    public boolean belongsTo(Role role) {
        return role.hasMember(name);
    }

    public void addError(String message) {
        errors().add(NAME, message); // Do not remove this - The old view for editing group authorization, template authorization makes use of it.
        errors().add(ADMIN, message);
    }

    public boolean isSameAs(Admin admin, List<Role> memberRoles) {
        return admin instanceof AdminUser && name.equals(((AdminUser) admin).name);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AdminUser adminUser = (AdminUser) o;

        return name.equals(adminUser.name);
    }

    public int hashCode() {
        return 31 * describe().hashCode() + (null != name ? name.hashCode() : 0);
    }

    public String toString() {
        return CaseInsensitiveString.str(name);
    }
}
