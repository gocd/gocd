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

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.config.security.users.AllowedUsers;

import java.util.*;

import static com.thoughtworks.go.config.security.util.SecurityConfigUtils.namesOf;
import static com.thoughtworks.go.config.security.util.SecurityConfigUtils.pluginRolesFor;

/**
 * Helper class to calculate the effective users that have view, operate, and admin permissions on
 * a specific group, upon which one can build {@link Permissions}
 * <p>
 * NOTE: Instances are intended to have short lifetimes (e.g., within the life of a request).
 * Don't hang onto references as results are cached and will not update on changes.
 * <p>
 * Extracted from {@link GoConfigPipelinePermissionsAuthority}
 */
class GroupSecurity {
    private final PipelineConfigs group;

    private final SecurityConfig security;
    private final Map<String, Collection<String>> rolesToUsers;
    private final Set<String> configuredAdmins;
    private final Set<PluginRoleConfig> definedAdminRoles;

    private AllowedUsers viewers;
    private AllowedUsers operators;
    private AllowedUsers admins;

    GroupSecurity(PipelineConfigs group, SecurityConfig security, Map<String, Collection<String>> rolesToUsers, Set<String> superAdminUsers, Set<PluginRoleConfig> superAdminPluginRoles) {
        this.group = group;
        this.security = security;
        this.rolesToUsers = rolesToUsers;

        HashSet<String> admins = new HashSet<>(superAdminUsers);
        admins.addAll(namesOf(group.getAuthorization().getAdminsConfig(), rolesToUsers));

        this.configuredAdmins = Collections.unmodifiableSet(admins);

        Set<PluginRoleConfig> adminRoles = pluginRolesFor(security, group.getAuthorization().getAdminsConfig().getRoles());
        adminRoles.addAll(superAdminPluginRoles);

        this.definedAdminRoles = Collections.unmodifiableSet(adminRoles);
    }

    Set<String> configuredAdmins() {
        return configuredAdmins;
    }

    AllowedUsers effectiveAdmins() {
        if (null == this.admins) {
            this.admins = new AllowedUsers(configuredAdmins(), definedAdminRoles);
        }
        return this.admins;
    }

    AllowedUsers effectiveOperators() {
        if (null == this.operators) {
            Set<String> operators = new HashSet<>();
            operators.addAll(configuredAdmins());
            operators.addAll(namesOf(group.getAuthorization().getOperationConfig(), rolesToUsers));

            Set<PluginRoleConfig> roles = pluginRolesFor(security, group.getAuthorization().getOperationConfig().getRoles());
            roles.addAll(definedAdminRoles);
            this.operators = new AllowedUsers(operators, roles);
        }
        return this.operators;
    }

    AllowedUsers effectiveViewers() {
        if (null == this.viewers) {
            Set<String> viewers = new HashSet<>();
            viewers.addAll(configuredAdmins());
            viewers.addAll(namesOf(group.getAuthorization().getViewConfig(), rolesToUsers));

            Set<PluginRoleConfig> roles = pluginRolesFor(security, group.getAuthorization().getViewConfig().getRoles());
            roles.addAll(definedAdminRoles);
            this.viewers = new AllowedUsers(viewers, roles);
        }
        return this.viewers;
    }

    AllowedUsers operatorsForPipeline(PipelineConfig pipeline) {
        if (!pipeline.first().hasOperatePermissionDefined()) {
            return effectiveOperators();
        }

        Set<String> approversOfFirstStage = namesOf(pipeline.first().getApproval().getAuthConfig(), rolesToUsers);
        Set<PluginRoleConfig> roleApproverOfFirstStage = pluginRolesFor(security, pipeline.first().getApproval().getAuthConfig().getRoles());

        Set<String> pipelineOperators = new HashSet<>();
        pipelineOperators.addAll(configuredAdmins());
        pipelineOperators.addAll(approversOfFirstStage);

        return new AllowedUsers(pipelineOperators, roleApproverOfFirstStage);
    }
}
