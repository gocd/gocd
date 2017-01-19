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

import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.*;

@ConfigInterface
public interface Role extends Validatable {
    CaseInsensitiveString getName();

    void setName(CaseInsensitiveString name);

    List<RoleUser> getUsers();

    void addUser(RoleUser user);

    void removeUser(RoleUser roleUser);

    default void validate(ValidationContext validationContext) {
        if (getName().isBlank() || !new NameTypeValidator().isNameValid(getName())) {
            addError("name", NameTypeValidator.errorMessage("role name", getName()));
        }

        Set<RoleUser> roleUsers = new HashSet<>();
        for (RoleUser user : getUsers()) {
            if (roleUsers.contains(user)) {
                new ErrorMarkingDuplicateHandler(this).invoke(user);
            } else {
                roleUsers.add(user);
            }
        }
    }

    default boolean hasMember(CaseInsensitiveString user) {
        if (user == null) {
            return false;
        }
        for (RoleUser roleUser : getUsers()) {
            if (roleUser.getName().equals(user)) {
                return true;
            }
        }
        return false;
    }

    default Collection<String> usersOfRole() {
        List<String> users = new ArrayList<>();
        for (RoleUser roleUser : getUsers()) {
            users.add(CaseInsensitiveString.str(roleUser.getName()));
        }
        return users;
    }

    default List<ConfigErrors> getAllErrors() {
        return ErrorCollector.getAllErrors(this);
    }

    class ErrorMarkingDuplicateHandler {

        private final Role role;

        public ErrorMarkingDuplicateHandler(Role role) {
            this.role = role;
        }

        public void invoke(final RoleUser roleUser) {
            roleUser.addDuplicateError(CaseInsensitiveString.str(role.getName()));
        }
    }
}
