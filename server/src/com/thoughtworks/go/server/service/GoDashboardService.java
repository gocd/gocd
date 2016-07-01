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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.server.dashboard.GoDashboardCache;
import com.thoughtworks.go.server.dashboard.GoDashboardCurrentStateLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/* Understands how to update the GoDashboardCache cache, given some pipeline related information. */
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

    public void updateForPipeline(CaseInsensitiveString pipelineName) {
        PipelineConfigs group = goConfigService.findGroupByPipeline(pipelineName);
        PipelineConfig pipelineConfig = group.findBy(pipelineName);

        updateCache(group, pipelineConfig);
    }

    public void updateForPipeline(PipelineConfig pipelineConfig) {
        updateCache(goConfigService.findGroupByPipeline(pipelineConfig.name()), pipelineConfig);
    }

    public void updateForAllPipelinesIn(CruiseConfig config) {
        cache.replaceAllEntriesInCacheWith(dashboardCurrentStateLoader.allPipelines(config));
    }

    private void updateCache(PipelineConfigs group, PipelineConfig pipelineConfig) {
        cache.put(dashboardCurrentStateLoader.pipelineFor(pipelineConfig, group));
    }
}
