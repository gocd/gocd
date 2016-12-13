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

import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractRole extends Configuration implements Role {
    protected final ConfigErrors configErrors = new ConfigErrors();

    @ConfigAttribute(value = "name", optional = false)
    protected CaseInsensitiveString name;

    protected AbstractRole() {
    }


    protected AbstractRole(final CaseInsensitiveString name, RoleUser... users) {
        this(name, Users.users(users));
    }

    protected AbstractRole(final CaseInsensitiveString name, Users users) {
        this.name = name;
        doSetUsers(new Users());
        for (RoleUser user : users) {
            addUser(user);
        }
    }


    public void validate(ValidationContext validationContext) {
        if (this.name.isBlank() || !new NameTypeValidator().isNameValid(name)) {
            configErrors.add("name", NameTypeValidator.errorMessage("role name", name));
        }

        checkForDuplicateUsers(new ErrorMarkingDuplicateHandler(this));
    }

    private void checkForDuplicateUsers(ErrorMarkingDuplicateHandler dupHandler) {
        Set<RoleUser> roleUsers = new HashSet<>();
        for (RoleUser user : doGetUsers()) {
            if (roleUsers.contains(user)) {
                dupHandler.invoke(user);
            } else {
                roleUsers.add(user);
            }
        }
    }


    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    @Override
    public CaseInsensitiveString getName() {
        return name;
    }

    @Override
    public void setName(CaseInsensitiveString name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AbstractRole that = (AbstractRole) o;

        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    private static class ErrorMarkingDuplicateHandler {

        private final Role role;

        public ErrorMarkingDuplicateHandler(Role role) {
            this.role = role;
        }

        public void invoke(final RoleUser roleUser) {
            roleUser.addDuplicateError(CaseInsensitiveString.str(role.getName()));
        }
    }

}
