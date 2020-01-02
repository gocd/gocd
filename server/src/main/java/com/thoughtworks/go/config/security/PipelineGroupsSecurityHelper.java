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
package com.thoughtworks.go.config.security;

import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityConfig;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.thoughtworks.go.config.security.util.SecurityConfigUtils.*;

/**
 * Helper factory to build {@link GroupSecurity} helpers that abstract the details of calculating
 * effective pipeline group viewers, operators, and admins.
 * <p>
 * NOTE: Instances are intended to have short lifetimes (e.g., within the life of a request).
 * Don't hang onto references as results are cached and will not update on changes.
 * <p>
 * Extracted from {@link GoConfigPipelinePermissionsAuthority}
 */
class PipelineGroupsSecurityHelper {
    private final SecurityConfig security;
    private final Map<String, Collection<String>> rolesToUsers;
    private final Set<String> superAdminUsers;
    private final Set<PluginRoleConfig> superAdminPluginRoles;
    private final boolean hasNoAdminsDefinedAtRootLevel;

    PipelineGroupsSecurityHelper(SecurityConfig security) {
        this.security = security;

        rolesToUsers = rolesToUsers(security);
        superAdminUsers = namesOf(security.adminsConfig(), rolesToUsers);
        superAdminPluginRoles = pluginRolesFor(security, security.adminsConfig().getRoles());
        hasNoAdminsDefinedAtRootLevel = noSuperAdminsDefined(security);
    }

    boolean hasNoSuperAdmins() {
        return hasNoAdminsDefinedAtRootLevel;
    }

    GroupSecurity forGroup(PipelineConfigs group) {
        return new GroupSecurity(group, security, rolesToUsers, superAdminUsers, superAdminPluginRoles);
    }
}
