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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.security.GoConfigPipelinePermissionsAuthority;
import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Everyone;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.server.dashboard.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.DashboardFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.thoughtworks.go.config.security.util.SecurityConfigUtils.*;

/* Understands how to interact with the GoDashboardCache cache. */
@Service
public class GoDashboardService {
    private final GoDashboardCache cache;
    private final GoDashboardCurrentStateLoader dashboardCurrentStateLoader;
    private final GoConfigService goConfigService;
    private GoConfigPipelinePermissionsAuthority permissionsAuthority;

    @Autowired
    public GoDashboardService(GoDashboardCache cache, GoDashboardCurrentStateLoader dashboardCurrentStateLoader, GoConfigPipelinePermissionsAuthority permissionsAuthority, GoConfigService goConfigService) {
        this.cache = cache;
        this.dashboardCurrentStateLoader = dashboardCurrentStateLoader;
        this.permissionsAuthority = permissionsAuthority;
        this.goConfigService = goConfigService;
    }

    public List<GoDashboardEnvironment> allEnvironmentsForDashboard(DashboardFilter filter, Username user) {
        GoDashboardPipelines allPipelines = cache.allEntries();
        List<GoDashboardEnvironment> environments = new ArrayList<>();

        final Users admins = superAdmins();

        goConfigService.getEnvironments().forEach(environment -> {
            GoDashboardEnvironment env = dashboardEnvironmentFor(environment, filter, user, admins, allPipelines);

            if (env.hasPipelines()) {
                environments.add(env);
            }
        });

        return environments;
    }

    public List<GoDashboardPipelineGroup> allPipelineGroupsForDashboard(DashboardFilter filter, Username user) {
        return allPipelineGroupsForDashboard(filter, user, false);
    }

    public List<GoDashboardPipelineGroup> allPipelineGroupsForDashboard(DashboardFilter filter, Username user, final boolean allowEmpty) {
        GoDashboardPipelines allPipelines = cache.allEntries();
        List<GoDashboardPipelineGroup> pipelineGroups = new ArrayList<>();

        goConfigService.groups().accept(group -> {
            GoDashboardPipelineGroup dashboardPipelineGroup = dashboardPipelineGroupFor(group, filter, user, allPipelines);
            if (forceIncludeEmptyGroup(allowEmpty, dashboardPipelineGroup, user) || dashboardPipelineGroup.hasPipelines()) {
                pipelineGroups.add(dashboardPipelineGroup);
            }
        });

        return pipelineGroups;
    }

    public void updateCacheForPipeline(CaseInsensitiveString pipelineName) {
        PipelineConfigs group = goConfigService.findGroupByPipeline(pipelineName);
        if (group == null) {
            removePipelineFromCache(pipelineName);
            return;
        }

        PipelineConfig pipelineConfig = group.findBy(pipelineName);
        updateCache(group, pipelineConfig);
    }

    public void updateCacheForPipeline(PipelineConfig pipelineConfig) {
        updateCache(goConfigService.findGroupByPipeline(pipelineConfig.name()), pipelineConfig);
    }

    public void updateCacheForAllPipelinesIn(CruiseConfig config) {
        cache.replaceAllEntriesInCacheWith(dashboardCurrentStateLoader.allPipelines(config));
    }

    public boolean hasEverLoadedCurrentState() {
        return dashboardCurrentStateLoader.hasEverLoadedCurrentState();
    }

    private GoDashboardEnvironment dashboardEnvironmentFor(EnvironmentConfig environment, DashboardFilter filter, Username user, Users allowedUsers, GoDashboardPipelines allPipelines) {
        List<CaseInsensitiveString> pipelinesInEnv = environment.getPipelineNames();
        GoDashboardEnvironment env = new GoDashboardEnvironment(environment.name().toString(), allowedUsers, !pipelinesInEnv.isEmpty());

        pipelinesInEnv.forEach(pipelineName -> {
            GoDashboardPipeline pipeline = allPipelines.find(pipelineName);

            if (null != pipeline && pipeline.canBeViewedBy(user.getUsername().toString()) && filter.isPipelineVisible(pipelineName)) {
                env.addPipeline(pipeline);
            }
        });

        return env;
    }

    private GoDashboardPipelineGroup dashboardPipelineGroupFor(PipelineConfigs pipelineGroup, DashboardFilter filter, Username user, GoDashboardPipelines allPipelines) {
        Permissions groupPermissions = resolvePermissionsForPipelineGroup(pipelineGroup, allPipelines);
        GoDashboardPipelineGroup goDashboardPipelineGroup = new GoDashboardPipelineGroup(pipelineGroup.getGroup(), groupPermissions, !pipelineGroup.isEmpty());

        if (goDashboardPipelineGroup.hasPermissions() && goDashboardPipelineGroup.canBeViewedBy(user)) {
            pipelineGroup.accept(pipelineConfig -> {
                CaseInsensitiveString pipelineName = pipelineConfig.name();
                GoDashboardPipeline pipeline = allPipelines.find(pipelineName);
                if (pipeline != null && filter.isPipelineVisible(pipelineName)) {
                    goDashboardPipelineGroup.addPipeline(pipeline);
                }
            });
        }
        return goDashboardPipelineGroup;
    }

    private Users superAdmins() {
        final SecurityConfig security = goConfigService.security();
        final Map<String, Collection<String>> rolesToUsersMap = rolesToUsers(security);
        final Set<String> superAdminUsers = namesOf(security.adminsConfig(), rolesToUsersMap);
        final Set<PluginRoleConfig> superAdminPluginRoles = pluginRolesFor(security, security.adminsConfig().getRoles());

        if (!goConfigService.isSecurityEnabled() || noSuperAdminsDefined(security)) {
            return Everyone.INSTANCE;
        }

        return new AllowedUsers(superAdminUsers, superAdminPluginRoles);
    }

    private Permissions resolvePermissionsForPipelineGroup(PipelineConfigs pipelineGroup, GoDashboardPipelines allPipelines) {
        for (PipelineConfig pipelineConfig : pipelineGroup) {
            GoDashboardPipeline goDashboardPipeline = allPipelines.find(pipelineConfig.getName());
            if (goDashboardPipeline != null) {
                return goDashboardPipeline.permissions();
            }
        }

        return permissionsAuthority.permissionsForEmptyGroup(pipelineGroup);
    }

    /**
     * Determines whether or not to include a (potentially) empty pipeline group on the dashboard. This consults the
     * feature toggle, view permissions, and whether or the not the pipeline group has any defined pipeline regardless
     * of permissions and personalization filters.
     *
     * @param allowEmpty - flag from feature toggle
     * @param dashboardGroup - the {@link GoDashboardPipelineGroup} instance
     * @param user - the current user
     * @return true if it should be included, false otherwise
     */
    private boolean forceIncludeEmptyGroup(final boolean allowEmpty, GoDashboardPipelineGroup dashboardGroup, Username user) {
        return allowEmpty && !dashboardGroup.hasDefinedPipelines() && dashboardGroup.canBeViewedBy(user);
    }

    private void updateCache(PipelineConfigs group, PipelineConfig pipelineConfig) {
        if (group == null) {
            removePipelineFromCache(pipelineConfig.name());
            return;
        }

        cache.put(dashboardCurrentStateLoader.pipelineFor(pipelineConfig, group));
    }

    private void removePipelineFromCache(CaseInsensitiveString pipelineName) {
        cache.remove(pipelineName);
        dashboardCurrentStateLoader.clearEntryFor(pipelineName);
    }
}
