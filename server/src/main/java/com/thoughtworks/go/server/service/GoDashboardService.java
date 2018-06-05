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
import com.thoughtworks.go.server.dashboard.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/* Understands how to interact with the GoDashboardCache cache. */
@Service
public class GoDashboardService {
    private final GoDashboardCache cache;
    private final GoDashboardCurrentStateLoader dashboardCurrentStateLoader;
    private final GoConfigService goConfigService;
    private SecurityService securityService;

    @Autowired
    public GoDashboardService(GoDashboardCache cache, GoDashboardCurrentStateLoader dashboardCurrentStateLoader, GoConfigService goConfigService, SecurityService securityService) {
        this.cache = cache;
        this.dashboardCurrentStateLoader = dashboardCurrentStateLoader;
        this.goConfigService = goConfigService;
        this.securityService = securityService;
    }

    public List<GoDashboardPipelineGroup> allPipelineGroupsForDashboard(PipelineSelections pipelineSelections, Username user) {
        GoDashboardPipelines allPipelines = cache.allEntries();
        List<GoDashboardPipelineGroup> pipelineGroups = new ArrayList<>();

        goConfigService.groups().accept(group -> {
            GoDashboardPipelineGroup dashboardPipelineGroup = dashboardPipelineGroupFor(group, pipelineSelections, user, allPipelines);
            if (dashboardPipelineGroup.hasPipelines()) {
                pipelineGroups.add(dashboardPipelineGroup);
            }
        });

        return pipelineGroups;
    }

    public void updateCacheForPipeline(CaseInsensitiveString pipelineName) {
        updateCacheForPipeline(goConfigService.pipelineConfigNamed(pipelineName));
    }

    public void updateCacheForPipeline(PipelineConfig pipelineConfig) {
        final EnvironmentConfig env = goConfigService.getEnvironments().findEnvironmentForPipeline(pipelineConfig.name());
        final PipelineConfigs group = goConfigService.findGroupByPipeline(pipelineConfig.name());
        updateCache(group, pipelineConfig, env);
    }

    public void updateCacheForAllPipelinesIn(CruiseConfig config) {
        cache.replaceAllEntriesInCacheWith(dashboardCurrentStateLoader.allPipelines(config));
    }

    public boolean hasEverLoadedCurrentState() {
        return dashboardCurrentStateLoader.hasEverLoadedCurrentState();
    }

    public boolean isSuperAdmin(Username username) {
        return securityService.isUserAdmin(username);
    }

    private GoDashboardPipelineGroup dashboardPipelineGroupFor(PipelineConfigs pipelineGroup, PipelineSelections pipelineSelections, Username user, GoDashboardPipelines allPipelines) {
        GoDashboardPipelineGroup goDashboardPipelineGroup = new GoDashboardPipelineGroup(pipelineGroup.getGroup(), resolvePermissionsForPipelineGroup(pipelineGroup, allPipelines));

        if (goDashboardPipelineGroup.hasPermissions() && goDashboardPipelineGroup.canBeViewedBy(user.getUsername().toString())) {
            pipelineGroup.accept(pipelineConfig -> {
                if (pipelineSelections.includesPipeline(pipelineConfig)) {
                    goDashboardPipelineGroup.addPipeline(allPipelines.find(pipelineConfig.getName()));
                }
            });
        }
        return goDashboardPipelineGroup;
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

    private void updateCache(PipelineConfigs group, PipelineConfig pipelineConfig, EnvironmentConfig env) {
        if (group == null) {
            cache.remove(pipelineConfig.name());
            dashboardCurrentStateLoader.clearEntryFor(pipelineConfig.name());
            return;
        }

        cache.put(dashboardCurrentStateLoader.pipelineFor(pipelineConfig, group, env));
    }
}
