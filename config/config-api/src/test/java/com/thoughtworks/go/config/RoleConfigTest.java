/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.helper.ValidationContextMother;
import com.thoughtworks.go.config.policy.Allow;
import com.thoughtworks.go.config.policy.Policy;
import com.thoughtworks.go.config.policy.SupportedAction;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.config.policy.SupportedEntity.ENVIRONMENT;
import static org.assertj.core.api.Assertions.assertThat;

public class RoleConfigTest {

    @Test
    public void validate_presenceOfRoleName() {
        validatePresenceOfRoleName(new Validator() {
            @Override
            public void validate(RoleConfig roleConfig, ValidationContext context) {
                roleConfig.validate(context);
            }
        });
    }

    @Test
    public void validate_nullNameInRole() {
        validateNullRoleName(new Validator() {
            @Override
            public void validate(RoleConfig roleConfig, ValidationContext context) {
                roleConfig.validate(context);
            }
        });
    }

    @Test
    public void validate_uniquenessOfRoleName() throws Exception {
        validateUniquenessOfRoleName(new Validator() {
            @Override
            public void validate(RoleConfig roleConfig, ValidationContext context) {
                roleConfig.validate(context);
            }
        });
    }

    @Test
    public void validateTree_presenceOfRoleName() {
        validatePresenceOfRoleName(new Validator() {
            @Override
            public void validate(RoleConfig roleConfig, ValidationContext context) {
                assertThat(roleConfig.validateTree(context)).isFalse();
            }
        });
    }

    @Test
    public void validateTree_nullNameInRole() {
        validateNullRoleName(new Validator() {
            @Override
            public void validate(RoleConfig roleConfig, ValidationContext context) {
                roleConfig.validateTree(context);
            }
        });
    }

    @Test
    public void validateTree_uniquenessOfRoleName() throws Exception {
        validateUniquenessOfRoleName(new Validator() {
            @Override
            public void validate(RoleConfig roleConfig, ValidationContext context) {
                assertThat(roleConfig.validateTree(context)).isFalse();
            }
        });
    }

    @Test
    public void shouldAnswerWhetherItHasPermissionsForGivenEntityOfTypeAndName() {
        final Policy directives = new Policy();
        directives.add(new Allow("view", ENVIRONMENT.getType(), "env_1"));
        RoleConfig role = new RoleConfig(new CaseInsensitiveString(""), new Users(), directives);

        assertThat(role.hasPermissionsFor(SupportedAction.VIEW, EnvironmentConfig.class, "env_1")).isTrue();
        assertThat(role.hasPermissionsFor(SupportedAction.VIEW, EnvironmentConfig.class, "env_2")).isFalse();
        assertThat(role.hasPermissionsFor(SupportedAction.VIEW, PipelineConfig.class, "*")).isFalse();
    }

    @Test
    void shouldValidatePolicy() {
        validatePolicyIsInvalid(new Validator() {
            @Override
            public void validate(RoleConfig roleConfig, ValidationContext context) {
                roleConfig.validateTree(context);
            }
        });
    }

    private void validatePresenceOfRoleName(Validator v) {
        RoleConfig role = new RoleConfig(new CaseInsensitiveString(""));

        v.validate(role, ValidationContextMother.validationContext(new SecurityConfig()));

        assertThat(role.errors().size()).isEqualTo(1);
        assertThat(role.errors().get("name").get(0)).isEqualTo("Invalid role name name ''. This must be alphanumeric and can" +
                " contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }

    private void validateNullRoleName(Validator v) {
        RoleConfig role = new RoleConfig();

        v.validate(role, ValidationContextMother.validationContext(new SecurityConfig()));

        assertThat(role.errors().size()).isEqualTo(1);
        assertThat(role.errors().get("name").get(0)).isEqualTo("Invalid role name name 'null'. This must be alphanumeric and can" +
                " contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }

    public void validateUniquenessOfRoleName(Validator v) throws Exception {
        RoleConfig role = new RoleConfig(new CaseInsensitiveString("admin"));
        SecurityConfig securityConfig = new SecurityConfig();
        ValidationContext validationContext = ValidationContextMother.validationContext(securityConfig);

        securityConfig.getRoles().add(new RoleConfig(new CaseInsensitiveString("admin")));
        securityConfig.getRoles().add(role);

        v.validate(role, validationContext);

        assertThat(role.errors().size()).isEqualTo(1);
        assertThat(role.errors().get("name").get(0)).isEqualTo("Role names should be unique. Role with the same name exists.");
    }

    private void validatePolicyIsInvalid(Validator validator) {
        SecurityConfig securityConfig = new SecurityConfig();
        ValidationContext validationContext = ValidationContextMother.validationContext(securityConfig);

        Policy policy = new Policy();
        policy.add(new Allow("*", ENVIRONMENT.getType(), "env_1"));
        RoleConfig role = new RoleConfig(new CaseInsensitiveString("role"), new Users(), policy);
        securityConfig.getRoles().add(role);

        validator.validate(role, validationContext);

        assertThat(role.getPolicy().hasErrors()).isTrue();
        assertThat(role.getPolicy().get(0).errors().on("action")).isEqualTo("Invalid action, must be one of [view, administer].");
    }

    interface Validator {
        void validate(RoleConfig roleConfig, ValidationContext context);
    }
}
