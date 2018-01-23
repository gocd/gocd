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

package com.thoughtworks.go.apiv2.dashboard.spring;

import com.thoughtworks.go.apiv2.dashboard.DashboardControllerDelegate;
import com.thoughtworks.go.server.service.GoDashboardService;
import com.thoughtworks.go.server.service.PipelineSelectionsService;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DashboardController implements SparkSpringController {

    private final DashboardControllerDelegate delegate;

    @Autowired
    public DashboardController(PipelineSelectionsService pipelineSelectionsService, GoDashboardService goDashboardService) {
        delegate = new DashboardControllerDelegate(pipelineSelectionsService, goDashboardService);
    }

    @Override
    public void setupRoutes() {
        delegate.setupRoutes();
    }
}
