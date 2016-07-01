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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.server.service.GoDashboardCacheUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/* Understands what needs to be done to keep the dashboard cache updated, when a stage status changes. */
@Component
public class GoDashboardStageStatusChangeHandler {
    private GoDashboardCacheUpdateService cacheUpdateService;

    @Autowired
    public GoDashboardStageStatusChangeHandler(GoDashboardCacheUpdateService cacheUpdateService) {
        this.cacheUpdateService = cacheUpdateService;
    }

    public void call(Stage stage) {
        cacheUpdateService.updateForPipeline(new CaseInsensitiveString(stage.getIdentifier().getPipelineName()));
    }
}
