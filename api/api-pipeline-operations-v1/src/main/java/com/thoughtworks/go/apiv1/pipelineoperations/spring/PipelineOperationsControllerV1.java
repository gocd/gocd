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

package com.thoughtworks.go.apiv1.pipelineoperations.spring;

import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.pipelineoperations.PipelineOperationsControllerV1Delegate;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PipelineHistoryService;
import com.thoughtworks.go.server.service.PipelinePauseService;
import com.thoughtworks.go.server.service.PipelineUnlockApiService;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PipelineOperationsControllerV1 implements SparkSpringController {
    private PipelineOperationsControllerV1Delegate delegate;

    @Autowired
    public PipelineOperationsControllerV1(PipelinePauseService pipelinePauseService,
                                          ApiAuthenticationHelper apiAuthenticationHelper,
                                          Localizer localizer,
                                          PipelineUnlockApiService pipelineUnlockApiService,
                                          GoConfigService goConfigService,
                                          PipelineHistoryService pipelineHistoryService) {

        this.delegate = new PipelineOperationsControllerV1Delegate(
                pipelinePauseService,
                pipelineUnlockApiService,
                apiAuthenticationHelper,
                localizer,
                goConfigService,
                pipelineHistoryService
        );
    }

    @Override
    public void setupRoutes() {
        delegate.setupRoutes();
    }
}
