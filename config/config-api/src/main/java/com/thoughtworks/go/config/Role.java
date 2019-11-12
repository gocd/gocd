/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.policy.Policy;
import com.thoughtworks.go.config.policy.PolicyAware;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.*;

import static com.thoughtworks.go.config.CaseInsensitiveString.isBlank;
import static com.thoughtworks.go.config.rules.SupportedEntity.ENVIRONMENT;
import static com.thoughtworks.go.config.rules.SupportedEntity.unmodifiableListOf;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

@ConfigInterface
public interface Role extends Validatable, PolicyAware {
    List<String> allowedActions = unmodifiableList(asList("view"));
    List<String> allowedTypes = unmodifiableListOf(ENVIRONMENT);

    CaseInsensitiveString getName();

    void setName(CaseInsensitiveString name);

    List<RoleUser> getUsers();

    void addUser(RoleUser user);

    void removeUser(RoleUser roleUser);

    default boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return !hasErrors();
    }

    @Override
    default void validate(ValidationContext validationContext) {
        if (isBlank(getName()) || !new NameTypeValidator().isNameValid(getName())) {
            addError("name", NameTypeValidator.errorMessage("role name", getName()));
        }

        RolesConfig roles = validationContext.getServerSecurityConfig().getRoles();

        if (!isBlank(getName()) && !roles.isUniqueRoleName(getName())) {
            addError("name", "Role names should be unique. Role with the same name exists.");
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

    boolean hasErrors();

    void encryptSecureProperties(CruiseConfig preprocessedConfig);

    class ErrorMarkingDuplicateHandler {

        private final Role role;

        public ErrorMarkingDuplicateHandler(Role role) {
            this.role = role;
        }

        public void invoke(final RoleUser roleUser) {
            roleUser.addDuplicateError(CaseInsensitiveString.str(role.getName()));
        }
    }

    Policy getPolicy();

    @Override
    default List<String> allowedActions() {
        return allowedActions;
    }

    @Override
    default List<String> allowedTypes() {
        return allowedTypes;
    }
}
