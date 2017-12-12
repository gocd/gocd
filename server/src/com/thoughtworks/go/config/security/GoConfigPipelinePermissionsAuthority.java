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
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/* Understands which users can view, operate and administer which pipelines and pipeline groups. */
@Service
public class GoConfigPipelinePermissionsAuthority {
    private GoConfigService goConfigService;

    @Autowired
    public GoConfigPipelinePermissionsAuthority(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public Map<CaseInsensitiveString, Permissions> pipelinesAndTheirPermissions() {
        return pipelinesInGroupsAndTheirPermissions(goConfigService.groups());
    }

    public Permissions permissionsForPipeline(CaseInsensitiveString pipelineName) {
        PipelineConfigs group = goConfigService.findGroupByPipeline(pipelineName);
        return pipelinesInGroupsAndTheirPermissions(new PipelineGroups(group)).get(pipelineName);
    }

    private Map<CaseInsensitiveString, Permissions> pipelinesInGroupsAndTheirPermissions(PipelineGroups groups) {
        final Map<CaseInsensitiveString, Permissions> pipelinesAndTheirPermissions = new HashMap<>();

        final SecurityConfig security = goConfigService.security();
        final Map<String, Collection<String>> rolesToUsers = rolesToUsers(security);
        final Set<String> superAdminUsers = namesOf(security.adminsConfig(), rolesToUsers);
        final Set<PluginRoleConfig> superAdminPluginRoles = pluginRolesFor(security.adminsConfig().getRoles());
        final boolean hasNoAdminsDefinedAtRootLevel = noSuperAdminsDefined();

        groups.accept(new PipelineGroupVisitor() {
            @Override
            public void visit(PipelineConfigs group) {
                Set<String> viewers = new HashSet<>();
                Set<String> operators = new HashSet<>();
                Set<String> admins = new HashSet<>();

                Set<String> pipelineGroupViewers = namesOf(group.getAuthorization().getViewConfig(), rolesToUsers);
                Set<String> pipelineGroupOperators = namesOf(group.getAuthorization().getOperationConfig(), rolesToUsers);
                Set<String> pipelineGroupAdmins = namesOf(group.getAuthorization().getAdminsConfig(), rolesToUsers);

                Set<PluginRoleConfig> pipelineGroupViewerRoles = pluginRolesFor(group.getAuthorization().getViewConfig().getRoles());
                Set<PluginRoleConfig> pipelineGroupOperatorRoles = pluginRolesFor(group.getAuthorization().getOperationConfig().getRoles());
                Set<PluginRoleConfig> pipelineGroupAdminRoles = pluginRolesFor(group.getAuthorization().getAdminsConfig().getRoles());

                pipelineGroupAdminRoles.addAll(superAdminPluginRoles);
                pipelineGroupOperatorRoles.addAll(pipelineGroupAdminRoles);
                pipelineGroupViewerRoles.addAll(pipelineGroupAdminRoles);

                admins.addAll(superAdminUsers);
                admins.addAll(pipelineGroupAdmins);

                operators.addAll(admins);
                operators.addAll(pipelineGroupOperators);

                viewers.addAll(admins);
                viewers.addAll(pipelineGroupViewers);

                boolean hasNoAuthDefinedAtGroupLevel = !group.hasAuthorizationDefined();

                for (PipelineConfig pipeline : group) {
                    if (hasNoAdminsDefinedAtRootLevel) {
                        pipelinesAndTheirPermissions.put(pipeline.name(), new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE));
                    } else if (hasNoAuthDefinedAtGroupLevel) {
                        AllowedUsers adminUsers = new AllowedUsers(admins, pipelineGroupAdminRoles);
                        pipelinesAndTheirPermissions.put(pipeline.name(), new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, adminUsers, adminUsers));
                    }else {
                        AllowedUsers pipelineOperators = pipelineOperators(pipeline, admins, new AllowedUsers(operators, pipelineGroupOperatorRoles), rolesToUsers);
                        Permissions permissions = new Permissions(new AllowedUsers(viewers, pipelineGroupViewerRoles), new AllowedUsers(operators, pipelineGroupOperatorRoles), new AllowedUsers(admins, pipelineGroupAdminRoles), pipelineOperators);
                        pipelinesAndTheirPermissions.put(pipeline.name(), permissions);
                    }
                }
            }
        });

        return pipelinesAndTheirPermissions;
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

    private AllowedUsers pipelineOperators(PipelineConfig pipeline, Set<String> admins, AllowedUsers groupLevelOperators, Map<String, Collection<String>> rolesToUsers) {
        if (!pipeline.first().hasOperatePermissionDefined()) {
            return groupLevelOperators;
        }

        Set<String> stageLevelApproversOfFirstStage = namesOf(pipeline.first().getApproval().getAuthConfig(), rolesToUsers);
        Set<PluginRoleConfig> stageLevelPluginRoleApproversOfFirstStage = pluginRolesFor(pipeline.first().getApproval().getAuthConfig().getRoles());

        Set<String> pipelineOperators = new HashSet<>();
        pipelineOperators.addAll(admins);
        pipelineOperators.addAll(stageLevelApproversOfFirstStage);

        return new AllowedUsers(pipelineOperators, stageLevelPluginRoleApproversOfFirstStage);
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
