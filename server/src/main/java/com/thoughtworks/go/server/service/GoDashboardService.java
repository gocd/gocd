/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Everyone;
import com.thoughtworks.go.config.security.users.NoOne;
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

    @Autowired
    public GoDashboardService(GoDashboardCache cache, GoDashboardCurrentStateLoader dashboardCurrentStateLoader, GoConfigService goConfigService) {
        this.cache = cache;
        this.dashboardCurrentStateLoader = dashboardCurrentStateLoader;
        this.goConfigService = goConfigService;
    }

    public List<GoDashboardEnvironment> allEnvironmentsForDashboard(DashboardFilter filter, Username user) {
        GoDashboardPipelines allPipelines = cache.allEntries();
        List<GoDashboardEnvironment> environments = new ArrayList<>();

        final Permissions permissions = environmentPermissions();

        goConfigService.getEnvironments().forEach(environment -> {
            GoDashboardEnvironment env = dashboardEnvironmentFor(environment, filter, user, permissions, allPipelines);

            if (env.hasPipelines()) {
                environments.add(env);
            }
        });

        return environments;
    }

    public List<GoDashboardPipelineGroup> allPipelineGroupsForDashboard(DashboardFilter filter, Username user) {
        GoDashboardPipelines allPipelines = cache.allEntries();
        List<GoDashboardPipelineGroup> pipelineGroups = new ArrayList<>();

        goConfigService.groups().accept(group -> {
            GoDashboardPipelineGroup dashboardPipelineGroup = dashboardPipelineGroupFor(group, filter, user, allPipelines);
            if (dashboardPipelineGroup.hasPipelines()) {
                pipelineGroups.add(dashboardPipelineGroup);
            }
        });

        return pipelineGroups;
    }

    public void updateCacheForPipeline(CaseInsensitiveString pipelineName) {
        PipelineConfigs group = goConfigService.findGroupByPipeline(pipelineName);
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

    private GoDashboardEnvironment dashboardEnvironmentFor(EnvironmentConfig environment, DashboardFilter filter, Username user, Permissions permissions, GoDashboardPipelines allPipelines) {
        GoDashboardEnvironment env = new GoDashboardEnvironment(environment.name().toString(), permissions);

        environment.getPipelineNames().forEach(pipelineName -> {
            GoDashboardPipeline pipeline = allPipelines.find(pipelineName);

            if (null != pipeline && pipeline.canBeViewedBy(user.getUsername().toString())) {
                if (filter.isPipelineVisible(pipelineName, pipeline.getLatestStage())) {
                    env.addPipeline(pipeline);
                }
            }
        });

        return env;
    }

    private GoDashboardPipelineGroup dashboardPipelineGroupFor(PipelineConfigs pipelineGroup, DashboardFilter filter, Username user, GoDashboardPipelines allPipelines) {
        GoDashboardPipelineGroup goDashboardPipelineGroup = new GoDashboardPipelineGroup(pipelineGroup.getGroup(), resolvePermissionsForPipelineGroup(pipelineGroup, allPipelines));

        if (goDashboardPipelineGroup.hasPermissions() && goDashboardPipelineGroup.canBeViewedBy(user.getUsername().toString())) {
            pipelineGroup.accept(pipelineConfig -> {
                GoDashboardPipeline pipeline = allPipelines.find(pipelineConfig.getName());
                if (pipeline != null) {
                    if (filter.isPipelineVisible(pipelineConfig.name(), pipeline.getLatestStage())) {
                        goDashboardPipelineGroup.addPipeline(pipeline);
                    }
                }
            });
        }
        return goDashboardPipelineGroup;
    }

    private Permissions environmentPermissions() {
        final SecurityConfig security = goConfigService.security();
        final Map<String, Collection<String>> rolesToUsersMap = rolesToUsers(security);
        final Set<String> superAdminUsers = namesOf(security.adminsConfig(), rolesToUsersMap);
        final Set<PluginRoleConfig> superAdminPluginRoles = pluginRolesFor(security, security.adminsConfig().getRoles());

        if (!goConfigService.isSecurityEnabled() || noSuperAdminsDefined(security)) {
            return new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE);
        }

        AllowedUsers superUsers = new AllowedUsers(superAdminUsers, superAdminPluginRoles);

        return new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, superUsers, NoOne.INSTANCE); // only care about the "admins" parameter
    }

    private Permissions resolvePermissionsForPipelineGroup(PipelineConfigs pipelineGroup, GoDashboardPipelines allPipelines) {
        for (PipelineConfig pipelineConfig : pipelineGroup) {
            GoDashboardPipeline goDashboardPipeline = allPipelines.find(pipelineConfig.getName());
            if (goDashboardPipeline != null) {
                return goDashboardPipeline.permissions();
            }
        }

        return null;
    }

    private void updateCache(PipelineConfigs group, PipelineConfig pipelineConfig) {
        if (group == null) {
            cache.remove(pipelineConfig.name());
            dashboardCurrentStateLoader.clearEntryFor(pipelineConfig.name());
            return;
        }

        cache.put(dashboardCurrentStateLoader.pipelineFor(pipelineConfig, group));
    }
}
