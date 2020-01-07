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

import com.thoughtworks.go.config.policy.*;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.*;

import static com.thoughtworks.go.config.CaseInsensitiveString.isBlank;
import static com.thoughtworks.go.config.policy.SupportedAction.ADMINISTER;
import static com.thoughtworks.go.config.policy.SupportedAction.VIEW;
import static com.thoughtworks.go.config.policy.SupportedEntity.*;

@ConfigInterface
public interface Role extends Validatable, PolicyAware {
    List<String> allowedActions = SupportedAction.unmodifiableListOf(VIEW, ADMINISTER);
    List<String> allowedTypes = SupportedEntity.unmodifiableListOf(ENVIRONMENT, CONFIG_REPO, ELASTIC_AGENT_PROFILE, CLUSTER_PROFILE);

    CaseInsensitiveString getName();

    void setName(CaseInsensitiveString name);

    List<RoleUser> getUsers();

    void addUser(RoleUser user);

    void removeUser(RoleUser roleUser);

    default boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        getPolicy().validateTree(new DelegatingValidationContext(validationContext) {
            @Override
            public PolicyValidationContext getPolicyValidationContext() {
                return new PolicyValidationContext(allowedActions(), allowedTypes());
            }
        });
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

    void setPolicy(Policy policy);

    @Override
    default List<String> allowedActions() {
        return allowedActions;
    }

    @Override
    default List<String> allowedTypes() {
        return allowedTypes;
    }
}
