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

package com.thoughtworks.go.api.spring;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.RoleConfig;
import com.thoughtworks.go.config.Users;
import com.thoughtworks.go.config.policy.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import spark.HaltException;

import java.util.Arrays;
import java.util.Map;

import static com.thoughtworks.go.domain.ArtifactPlan.GSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class ApiAuthenticationHelperTest {
    @Mock
    private SecurityService securityService;
    @Mock
    private GoConfigService goConfigService;

    private ApiAuthenticationHelper helper;

    private final Username ADMIN = new Username("Admin");
    private final Username BOB = new Username("Bob");

    @BeforeEach
    void setUp() {
        initMocks(this);

        when(securityService.isUserAdmin(ADMIN)).thenReturn(true);
        when(securityService.isUserAdmin(BOB)).thenReturn(false);
    }

    @Nested
    class GranularAuth {
        @BeforeEach
        void setUp() {
            helper = new ApiAuthenticationHelper(securityService, goConfigService);
        }

        @Test
        void shouldAllowAllAdministratorsToViewAllEnvironments() {
            assertDoesNotThrow(() -> helper.checkUserHasPermissions(ADMIN, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, null));
        }

        @Test
        void shouldAllowAllAdministratorsToViewSingleEnvironments() {
            assertDoesNotThrow(() -> helper.checkUserHasPermissions(ADMIN, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, "env_1"));
        }

        @Test
        void shouldNotAllNormalUsersToViewAllEnvironments() {
            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, null));
        }

        @Test
        void shouldNotAllNormalUsersToViewSingleEnvironments() {
            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, "env_1"));
        }

        @Test
        /*
        <role name="allow-all-environments">
            <policy>
                <allow action="view" type="environment">*</allow>
            </policy>
            <users>
                <user>Bob</user>
            </users>
        </role>
        */
        void shouldAllowNormalUserWithAllowedEnvironmentPolicyToViewEnvironments() {
            Policy directives = new Policy();
            directives.add(new Allow("view", "environment", "*"));
            RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("read-only-environments"), new Users(), directives);

            when(goConfigService.rolesForUser(BOB.getUsername())).thenReturn(Arrays.asList(roleConfig));

            assertDoesNotThrow(() -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, null));
            assertDoesNotThrow(() -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, "foo"));
        }

        @Test
        /*
        <role name="allow-all-environments">
            <policy>
                <allow action="edit" type="environment">*</allow>
            </policy>
            <users>
                <user>Bob</user>
            </users>
        </role>
        */
        void shouldAllowNormalUserWithEditEnvironmentPolicyToViewEnvironments() {
            Policy directives = new Policy();
            directives.add(new Allow("edit", "environment", "*"));
            RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("read-only-environments"), new Users(), directives);

            when(goConfigService.rolesForUser(BOB.getUsername())).thenReturn(Arrays.asList(roleConfig));

            assertDoesNotThrow(() -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, null));
            assertDoesNotThrow(() -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, "foo"));
        }


        @Test
        /*
        <role name="allow-all-environments">
            <policy>
                <allow action="edit" type="environment">*</allow>
            </policy>
            <users>
                <user>Bob</user>
            </users>
        </role>
        */
        void shouldAllowNormalUserWithEditEnvironmentPolicyToEditEnvironments() {
            Policy directives = new Policy();
            directives.add(new Allow("edit", "environment", "*"));
            RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("read-only-environments"), new Users(), directives);

            when(goConfigService.rolesForUser(BOB.getUsername())).thenReturn(Arrays.asList(roleConfig));

            assertDoesNotThrow(() -> helper.checkUserHasPermissions(BOB, SupportedAction.EDIT, SupportedEntity.ENVIRONMENT, null));
            assertDoesNotThrow(() -> helper.checkUserHasPermissions(BOB, SupportedAction.EDIT, SupportedEntity.ENVIRONMENT, "foo"));
        }

        @Test
        /*
        <role name="allow-all-environments">
            <policy>
                <allow action="edit" type="environment">*</allow>
            </policy>
            <users>
                <user>Bob</user>
            </users>
        </role>
        */
        void shouldNotAllowNormalUserWithEditEnvironmentPolicyToAdministerEnvironments() {
            Policy directives = new Policy();
            directives.add(new Allow("edit", "environment", "*"));
            RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("read-only-environments"), new Users(), directives);

            when(goConfigService.rolesForUser(BOB.getUsername())).thenReturn(Arrays.asList(roleConfig));

            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.ADMINISTER, SupportedEntity.ENVIRONMENT, null));
            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.ADMINISTER, SupportedEntity.ENVIRONMENT, "foo"));
        }

        @Test
        /*
        <role name="allow-one-environments">
            <policy>
                <allow action="view" type="environment">env_1</allow>
            </policy>
            <users>
                <user>Bob</user>
            </users>
        </role>
        */
        void shouldAllowNormalUserWithAllowedEnvironmentPolicyToViewSpecificEnvironment() {
            Policy directives = new Policy();
            directives.add(new Allow("view", "environment", "env_1"));
            RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("read-only-environments"), new Users(), directives);

            when(goConfigService.rolesForUser(BOB.getUsername())).thenReturn(Arrays.asList(roleConfig));

            assertDoesNotThrow(() -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, "env_1"));

            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, null));
            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, "env_2"));
        }

        @Test
        /*
        <role name="disallowed-directive-first-environment">
            <policy>
                <deny action="view" type="environment">*</deny>
                <allow action="view" type="environment">env_1</allow>
            </policy>
            <users>
                <user>Bob</user>
            </users>
        </role>
        */
        void shouldNotAllowNormalUserWithDisallowPolicyFirstToViewEnvironment() {
            Policy directives = new Policy();
            directives.add(new Deny("view", "environment", "*"));
            directives.add(new Allow("view", "environment", "env_1"));
            RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("read-only-environments"), new Users(), directives);

            when(goConfigService.rolesForUser(BOB.getUsername())).thenReturn(Arrays.asList(roleConfig));

            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, null));
            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, "env_1"));
        }

        @Test
        /*
        <role name="disallowed-directive-first-environment">
            <policy>
                <allow action="view" type="environment">env_1</allow>
                <deny action="view" type="environment">*</deny>
            </policy>
            <users>
                <user>Bob</user>
            </users>
        </role>
        */
        void shouldAllowNormalUserWithAllowedPolicyFirstToViewEnvironment() {
            Policy directives = new Policy();
            directives.add(new Allow("view", "environment", "env_1"));
            directives.add(new Deny("view", "environment", "*"));
            RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("read-only-environments"), new Users(), directives);

            when(goConfigService.rolesForUser(BOB.getUsername())).thenReturn(Arrays.asList(roleConfig));

            assertDoesNotThrow(() -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, "env_1"));
            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, null));
        }

        @Test
        /*
        <role name="allow-all-environments">
            <policy>
                <allow action="administer" type="environment">*</allow>
            </policy>
            <users>
                <user>Bob</user>
            </users>
        </role>
        */
        void shouldAllowNormalUserWithAllowedEnvironmentPolicyToAdministerEnvironments() {
            Policy directives = new Policy();
            directives.add(new Allow("administer", "environment", "*"));
            RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("read-only-environments"), new Users(), directives);

            when(goConfigService.rolesForUser(BOB.getUsername())).thenReturn(Arrays.asList(roleConfig));

            assertDoesNotThrow(() -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, null));
            assertDoesNotThrow(() -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, "foo"));

            assertDoesNotThrow(() -> helper.checkUserHasPermissions(BOB, SupportedAction.ADMINISTER, SupportedEntity.ENVIRONMENT, null));
            assertDoesNotThrow(() -> helper.checkUserHasPermissions(BOB, SupportedAction.ADMINISTER, SupportedEntity.ENVIRONMENT, "foo"));
        }

        @Test
        /*
        <role name="disallowed-directive-first-environment">
            <policy>
                <deny action="administer" type="environment">*</deny>
                <allow action="administer" type="environment">env_1</allow>
            </policy>
            <users>
                <user>Bob</user>
            </users>
        </role>
        */
        void shouldNotAllowNormalUserWithDisallowPolicyFirstToAdministerEnvironment() {
            Policy directives = new Policy();
            directives.add(new Deny("administer", "environment", "*"));
            directives.add(new Allow("administer", "environment", "env_1"));
            RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("read-only-environments"), new Users(), directives);

            when(goConfigService.rolesForUser(BOB.getUsername())).thenReturn(Arrays.asList(roleConfig));

            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, null));
            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, "env_1"));

            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.ADMINISTER, SupportedEntity.ENVIRONMENT, null));
            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.ADMINISTER, SupportedEntity.ENVIRONMENT, "env_1"));
        }

        @Test
        /*
        <role name="deny-permissions">
            <policy>
                <deny action="administer" type="environment">*</deny>
            </policy>
            <users>
                <user>Bob</user>
            </users>
        </role>
        <role name="allow-permissions">
            <policy>
                <allow action="administer" type="environment">*</allow>
            </policy>
            <users>
                <user>Bob</user>
            </users>
        </role>
        */
        void shouldNotAllowNormalUserAccessToEnvironmentWhenOnOfTheRoleHasAnExplicitDeny_WithFirstRoleAsDeny() {
            Policy directives = new Policy();
            directives.add(new Deny("administer", "environment", "*"));
            RoleConfig denyRoleConfig = new RoleConfig(new CaseInsensitiveString("deny-permissions"), new Users(), directives);

            Policy directives2 = new Policy();
            directives2.add(new Allow("administer", "environment", "*"));
            RoleConfig allowRoleConfig = new RoleConfig(new CaseInsensitiveString("allow-permissions"), new Users(), directives2);

            when(goConfigService.rolesForUser(BOB.getUsername())).thenReturn(Arrays.asList(denyRoleConfig, allowRoleConfig));

            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, null));
            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, "env_1"));

            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.ADMINISTER, SupportedEntity.ENVIRONMENT, null));
            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.ADMINISTER, SupportedEntity.ENVIRONMENT, "env_1"));
        }

        @Test
        /*
        <role name="allow-permissions">
            <policy>
                <allow action="administer" type="environment">*</allow>
            </policy>
            <users>
                <user>Bob</user>
            </users>
        </role>
        <role name="deny-permissions">
            <policy>
                <deny action="administer" type="environment">*</deny>
            </policy>
            <users>
                <user>Bob</user>
            </users>
        </role>
        */
        void shouldNotAllowNormalUserAccessToEnvironmentWhenOnOfTheRoleHasAnExplicitDeny_WithFirstRoleAsAllow() {
            Policy directives = new Policy();
            directives.add(new Deny("administer", "environment", "*"));
            RoleConfig denyRoleConfig = new RoleConfig(new CaseInsensitiveString("deny-permissions"), new Users(), directives);

            Policy directives2 = new Policy();
            directives2.add(new Allow("administer", "environment", "*"));
            RoleConfig allowRoleConfig = new RoleConfig(new CaseInsensitiveString("allow-permissions"), new Users(), directives2);

            when(goConfigService.rolesForUser(BOB.getUsername())).thenReturn(Arrays.asList(allowRoleConfig, denyRoleConfig));

            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, null));
            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, "env_1"));

            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.ADMINISTER, SupportedEntity.ENVIRONMENT, null));
            assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.ADMINISTER, SupportedEntity.ENVIRONMENT, "env_1"));
        }

        @Test
        void shouldRenderAppropriateForbiddenErrorMessageWhenUserDoesNotHaveViewPermissions() {
            HaltException thrown = assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.VIEW, SupportedEntity.ENVIRONMENT, "env_1"));
            String expectedMessage = "User 'Bob' does not have permissions to view 'env_1' environment(s).";
            assertThat(GSON.fromJson(thrown.body(), Map.class).get("message")).isEqualTo(expectedMessage);
        }

        @Test
        void shouldRenderAppropriateForbiddenErrorMessageWhenUserDoesNotHaveAdministerPermissions() {
            HaltException thrown = assertThrows(HaltException.class, () -> helper.checkUserHasPermissions(BOB, SupportedAction.ADMINISTER, SupportedEntity.ENVIRONMENT, "env_1"));
            String expectedMessage = "User 'Bob' does not have permissions to administer 'env_1' environment(s).";
            assertThat(GSON.fromJson(thrown.body(), Map.class).get("message")).isEqualTo(expectedMessage);
        }
    }
}
