/*
 * Copyright Thoughtworks, Inc.
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

import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PluginRoleUsersStore {
    private final ConcurrentMap<PluginRoleConfig, Set<RoleUser>> roleToUsersMappings = new ConcurrentHashMap<>();

    private PluginRoleUsersStore() {

    }

    public static PluginRoleUsersStore instance() {
        return PluginRoleUsersStoreHolder.PLUGIN_ROLE_USERS_STORE;
    }

    public void assignRole(String user, PluginRoleConfig pluginRoleConfig) {
        roleToUsersMappings
            .computeIfAbsent(pluginRoleConfig, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
            .add(new RoleUser(user));
    }

    public List<RoleUser> usersInRole(PluginRoleConfig pluginRoleConfig) {
        return pluginRoleConfig == null ? Collections.emptyList() : new ArrayList<>(roleToUsersMappings.getOrDefault(pluginRoleConfig, Collections.emptySet()));
    }

    public void removePluginRolesNotIn(List<PluginRoleConfig> pluginRoles) {
        for (PluginRoleConfig pluginRole : pluginRoles()) {
            if (!pluginRoles.contains(pluginRole)) {
                remove(pluginRole);
            }
        }
    }

    public void remove(PluginRoleConfig pluginRole) {
        roleToUsersMappings.remove(pluginRole);
    }

    public void remove(Collection<PluginRoleConfig> pluginRoles) {
        for (PluginRoleConfig role : pluginRoles) {
            remove(role);
        }
    }

    public void revokeAllRolesFor(String username) {
        final RoleUser roleUser = new RoleUser(username);
        for (Map.Entry<PluginRoleConfig, Set<RoleUser>> entry : roleToUsersMappings.entrySet()) {
            if (entry.getValue().remove(roleUser)) {
                roleToUsersMappings.computeIfPresent(entry.getKey(), (c, users) -> users.isEmpty() ? null : users);
            }
        }
    }

    protected Set<PluginRoleConfig> pluginRoles() {
        return new HashSet<>(roleToUsersMappings.keySet());
    }

    @TestOnly
    public void clearAll() {
        roleToUsersMappings.clear();
    }

    private static class PluginRoleUsersStoreHolder {
        static final PluginRoleUsersStore PLUGIN_ROLE_USERS_STORE = new PluginRoleUsersStore();
    }
}
