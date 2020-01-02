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
package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.server.service.GoDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/* Understands what needs to be done to keep the dashboard cache updated, when the config changes. */
@Component
public class GoDashboardConfigChangeHandler {
    private GoDashboardService cacheUpdateService;

    @Autowired
    public GoDashboardConfigChangeHandler(GoDashboardService cacheUpdateService) {
        this.cacheUpdateService = cacheUpdateService;
    }

    public void call(PipelineConfig pipelineConfig) {
        cacheUpdateService.updateCacheForPipeline(pipelineConfig);
    }

    public void call(CruiseConfig config) {
        cacheUpdateService.updateCacheForAllPipelinesIn(config);
    }
}
