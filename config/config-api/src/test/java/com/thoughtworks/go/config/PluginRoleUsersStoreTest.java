/*
 * Copyright 2024 Thoughtworks, Inc.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginRoleUsersStoreTest {

    private PluginRoleUsersStore pluginRoleUsersStore;

    @BeforeEach
    public void setUp() throws Exception {
        pluginRoleUsersStore = PluginRoleUsersStore.instance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        pluginRoleUsersStore.clearAll();
    }

    @Test
    public void assignRole_ShouldAssignPluginRoleToAnUser() {
        assertThat(pluginRoleUsersStore.pluginRoles()).isEmpty();

        PluginRoleConfig pluginRoleConfig = new PluginRoleConfig("spacetiger", "ldap");
        pluginRoleUsersStore.assignRole("wing-commander", pluginRoleConfig);

        assertThat(pluginRoleUsersStore.pluginRoles()).hasSize(1);
        assertThat(pluginRoleUsersStore.usersInRole(pluginRoleConfig)).contains(new RoleUser("wing-commander"));
    }

    @Test
    public void removePluginRole_ShouldRemovePluginRoleFromStore() {
        PluginRoleConfig pluginRoleConfig = new PluginRoleConfig("spacetiger", "ldap");
        pluginRoleUsersStore.assignRole("wing-commander", pluginRoleConfig);

        assertThat(pluginRoleUsersStore.pluginRoles()).hasSize(1);
        assertThat(pluginRoleUsersStore.usersInRole(pluginRoleConfig)).contains(new RoleUser("wing-commander"));

        pluginRoleUsersStore.remove(pluginRoleConfig);

        assertThat(pluginRoleUsersStore.pluginRoles()).isEmpty();
    }

    @Test
    public void revokeAllRolesFor_ShouldRevokeAllRolesForAGivenUser() {
        PluginRoleConfig pluginRoleSpaceTiger = new PluginRoleConfig("spacetiger", "ldap");
        PluginRoleConfig pluginRoleBlackBird = new PluginRoleConfig("blackbird", "ldap");
        pluginRoleUsersStore.assignRole("wing-commander", pluginRoleSpaceTiger);
        pluginRoleUsersStore.assignRole("wing-commander", pluginRoleBlackBird);
        pluginRoleUsersStore.assignRole("bob", pluginRoleBlackBird);

        assertThat(pluginRoleUsersStore.pluginRoles()).hasSize(2);
        assertThat(pluginRoleUsersStore.usersInRole(pluginRoleSpaceTiger)).hasSize(1);
        assertThat(pluginRoleUsersStore.usersInRole(pluginRoleBlackBird)).hasSize(2);
        assertThat(pluginRoleUsersStore.usersInRole(pluginRoleSpaceTiger)).contains(new RoleUser("wing-commander"));
        assertThat(pluginRoleUsersStore.usersInRole(pluginRoleBlackBird)).containsExactlyInAnyOrder(new RoleUser("wing-commander"), new RoleUser("bob"));

        pluginRoleUsersStore.revokeAllRolesFor("wing-commander");

        assertThat(pluginRoleUsersStore.pluginRoles()).hasSize(1);
        assertThat(pluginRoleUsersStore.usersInRole(pluginRoleSpaceTiger)).hasSize(0);
        assertThat(pluginRoleUsersStore.usersInRole(pluginRoleBlackBird)).hasSize(1);
    }
}
