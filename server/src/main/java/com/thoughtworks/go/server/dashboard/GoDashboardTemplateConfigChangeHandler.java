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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.GoDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/* Understands what needs to be done to keep the dashboard cache updated, when a template changes. */
@Component
public class GoDashboardTemplateConfigChangeHandler {
    private GoDashboardService cacheUpdateService;
    private GoConfigService goConfigService;

    @Autowired
    public GoDashboardTemplateConfigChangeHandler(GoDashboardService cacheUpdateService, GoConfigService goConfigService) {
        this.cacheUpdateService = cacheUpdateService;
        this.goConfigService = goConfigService;
    }

    public void call(PipelineTemplateConfig pipelineTemplateConfig) {
        List<CaseInsensitiveString> pipelines = goConfigService.currentCruiseConfig().pipelinesAssociatedWithTemplate(pipelineTemplateConfig.name());
        for (CaseInsensitiveString pipeline : pipelines) {
            cacheUpdateService.updateCacheForPipeline(pipeline);
        }
    }
}
