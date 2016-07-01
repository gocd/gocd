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

package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.server.service.GoDashboardCurrentStateLoader;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/* Understands what needs to be done to keep the dashboard cache updated, when the config changes. */
@Component
public class GoDashboardConfigChangeHandler {
    private final GoDashboardCache cache;
    private final GoDashboardCurrentStateLoader dashboardCurrentStateLoader;
    private GoConfigService goConfigService;

    @Autowired
    public GoDashboardConfigChangeHandler(GoDashboardCache cache, GoDashboardCurrentStateLoader dashboardCurrentStateLoader, GoConfigService goConfigService) {
        this.cache = cache;
        this.dashboardCurrentStateLoader = dashboardCurrentStateLoader;
        this.goConfigService = goConfigService;
    }

    public void call(PipelineConfig pipelineConfig) {
        PipelineConfigs group = goConfigService.findGroupByPipeline(pipelineConfig.name());
        cache.put(dashboardCurrentStateLoader.pipelineFor(pipelineConfig, group));
    }

    public void call(CruiseConfig config) {
        cache.replaceAllEntriesInCacheWith(dashboardCurrentStateLoader.allPipelines(config));
    }
}
