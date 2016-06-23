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

package com.thoughtworks.go.config.security;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.PipelineGroupVisitor;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Everyone;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/* Understands which users can view which pipelines and pipeline groups. */
@Service
public class GoConfigPipelinePermissionsAuthority {
    private GoConfigService goConfigService;

    @Autowired
    public GoConfigPipelinePermissionsAuthority(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public Map<String, Users> groupsAndTheirViewers() {
        final Map<String, Users> pipelinesAndViewers = new HashMap<>();

        SecurityConfig security = goConfigService.security();
        final Map<String, Collection<String>> rolesToUsers = rolesToUsers(security);
        final Set<String> superAdminUsers = namesOf(security.adminsConfig(), rolesToUsers);
        final Set<PluginRoleConfig> superAdminPluginRoles = pluginRolesFor(security.adminsConfig().getRoles());
        final boolean noSuperAdminsAreDefined = noSuperAdminsDefined();

        goConfigService.groups().accept(new PipelineGroupVisitor() {
            @Override
            public void visit(PipelineConfigs pipelineConfigs) {
                if (noSuperAdminsAreDefined || !pipelineConfigs.hasAuthorizationDefined()) {
                    pipelinesAndViewers.put(pipelineConfigs.getGroup(), Everyone.INSTANCE);
                    return;
                }

                Set<String> pipelineGroupAdmins = namesOf(pipelineConfigs.getAuthorization().getAdminsConfig(), rolesToUsers);
                Set<String> pipelineGroupViewers = namesOf(pipelineConfigs.getAuthorization().getViewConfig(), rolesToUsers);

                Set<String> viewers = new HashSet<>();
                viewers.addAll(superAdminUsers);
                viewers.addAll(pipelineGroupAdmins);
                viewers.addAll(pipelineGroupViewers);

                Set<PluginRoleConfig> roles = new HashSet<>();
                roles.addAll(superAdminPluginRoles);
                roles.addAll(pluginRolesFor(pipelineConfigs.getAuthorization().getAdminsConfig().getRoles()));
                roles.addAll(pluginRolesFor(pipelineConfigs.getAuthorization().getViewConfig().getRoles()));

                pipelinesAndViewers.put(pipelineConfigs.getGroup(), new AllowedUsers(viewers, roles));
            }
        });

        return pipelinesAndViewers;
    }

    private Set<PluginRoleConfig> pluginRolesFor(List<AdminRole> roles) {
        Set<PluginRoleConfig> pluginRoleConfigs = new HashSet<>();

        for (AdminRole role : roles) {
            PluginRoleConfig pluginRole = goConfigService.security().getPluginRole(role.getName());
            if (pluginRole != null) {
                pluginRoleConfigs.add(pluginRole);
            }
        }

        return pluginRoleConfigs;
    }

    private boolean noSuperAdminsDefined() {
        AdminsConfig adminsConfig = goConfigService.security().adminsConfig();
        return adminsConfig.getRoles().isEmpty() && adminsConfig.getUsers().isEmpty();
    }

    private Set<String> namesOf(AdminsConfig adminsConfig, Map<String, Collection<String>> rolesToUsers) {
        List<AdminUser> superAdmins = adminsConfig.getUsers();
        Set<String> superAdminNames = new HashSet<>();

        for (AdminUser superAdminUser : superAdmins) {
            superAdminNames.add(superAdminUser.getName().toLower());
        }

        for (AdminRole superAdminRole : adminsConfig.getRoles()) {
            superAdminNames.addAll(emptyIfNull(rolesToUsers.get(superAdminRole.getName().toLower())));
        }

        return superAdminNames;
    }

    private Map<String, Collection<String>> rolesToUsers(SecurityConfig securityConfig) {
        Map<String, Collection<String>> rolesToUsers = new HashMap<>();
        for (Role role : securityConfig.getRoles()) {
            if (role instanceof RoleConfig) {
                rolesToUsers.put(role.getName().toLower(), role.usersOfRole());
            }
        }
        return rolesToUsers;
    }

    private Collection<String> emptyIfNull(Collection<String> collection) {
        return collection == null ? Collections.emptyList() : collection;
    }
}
