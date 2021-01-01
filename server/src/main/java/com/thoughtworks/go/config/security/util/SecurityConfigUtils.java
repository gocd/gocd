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
package com.thoughtworks.go.config.security.util;

import com.thoughtworks.go.config.*;

import java.util.*;

/**
 * Utility class for extracting and manipulating settings in {@link SecurityConfig} objects.
 */
public class SecurityConfigUtils {
    private SecurityConfigUtils() {
    }

    /**
     * Compiles a list of users from an {@link AdminsConfig}, denormalizing roles to the underlying
     * members.
     *
     * @param adminsConfig the config fragment
     * @param rolesToUsers a {@link Map} of member users to their respective roles
     * @return a {@link Set} of user names from the config
     */
    public static Set<String> namesOf(AdminsConfig adminsConfig, Map<String, Collection<String>> rolesToUsers) {
        List<AdminUser> admins = adminsConfig.getUsers();
        Set<String> adminNames = new HashSet<>();

        for (AdminUser admin : admins) {
            adminNames.add(admin.getName().toLower());
        }

        for (AdminRole adminRole : adminsConfig.getRoles()) {
            adminNames.addAll(emptyIfNull(rolesToUsers.get(adminRole.getName().toLower())));
        }

        return adminNames;
    }

    public static Map<String, Collection<String>> rolesToUsers(SecurityConfig securityConfig) {
        Map<String, Collection<String>> rolesToUsers = new HashMap<>();
        for (Role role : securityConfig.getRoles()) {
            if (role instanceof RoleConfig) {
                rolesToUsers.put(role.getName().toLower(), role.usersOfRole());
            }
        }
        return rolesToUsers;
    }

    public static Set<PluginRoleConfig> pluginRolesFor(SecurityConfig securityConfig, List<AdminRole> roles) {
        Set<PluginRoleConfig> pluginRoleConfigs = new HashSet<>();

        for (AdminRole role : roles) {
            PluginRoleConfig pluginRole = securityConfig.getPluginRole(role.getName());
            if (pluginRole != null) {
                pluginRoleConfigs.add(pluginRole);
            }
        }

        return pluginRoleConfigs;
    }

    public static boolean noSuperAdminsDefined(SecurityConfig securityConfig) {
        AdminsConfig adminsConfig = securityConfig.adminsConfig();
        return adminsConfig.getRoles().isEmpty() && adminsConfig.getUsers().isEmpty();
    }


    private static Collection<String> emptyIfNull(Collection<String> collection) {
        return collection == null ? Collections.emptyList() : collection;
    }

}
