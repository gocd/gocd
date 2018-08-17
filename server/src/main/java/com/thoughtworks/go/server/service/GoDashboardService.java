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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.server.dashboard.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.DashboardFilter;
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

    @Autowired
    public GoDashboardService(GoDashboardCache cache, GoDashboardCurrentStateLoader dashboardCurrentStateLoader, GoConfigService goConfigService) {
        this.cache = cache;
        this.dashboardCurrentStateLoader = dashboardCurrentStateLoader;
        this.goConfigService = goConfigService;
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
