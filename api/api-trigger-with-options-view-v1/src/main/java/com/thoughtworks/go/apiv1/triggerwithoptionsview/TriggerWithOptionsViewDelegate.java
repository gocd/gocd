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

package com.thoughtworks.go.apiv1.triggerwithoptionsview;


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.triggerwithoptionsview.representers.TriggerOptions;
import com.thoughtworks.go.apiv1.triggerwithoptionsview.representers.TriggerWithOptionsViewRepresenter;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PipelineHistoryService;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;
import spark.Request;
import spark.Response;

import java.util.Map;

import static spark.Spark.*;

public class TriggerWithOptionsViewDelegate extends ApiController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final GoConfigService goConfigService;
    private final PipelineHistoryService pipelineHistoryService;

    public TriggerWithOptionsViewDelegate(ApiAuthenticationHelper apiAuthenticationHelper, GoConfigService goConfigService, PipelineHistoryService pipelineHistoryService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.goConfigService = goConfigService;
        this.pipelineHistoryService = pipelineHistoryService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.TriggerWithOptionsView.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, apiAuthenticationHelper::checkPipelineGroupOperateUserAnd401);
            before("/*", mimeType, apiAuthenticationHelper::checkPipelineGroupOperateUserAnd401);

            get("/:pipeline_name", this::index, GsonTransformer.getInstance());

            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    public Map<String, Object> index(Request request, Response response) {
        String pipelineName = request.params("pipeline_name");

        EnvironmentVariablesConfig variables = goConfigService.variablesFor(pipelineName);
        PipelineInstanceModel pipelineInstanceModel = pipelineHistoryService.latest(pipelineName, currentUsername());

        TriggerOptions triggerOptions = new TriggerOptions(variables, pipelineInstanceModel);
        return TriggerWithOptionsViewRepresenter.toJSON(triggerOptions, RequestContext.requestContext(request));
    }
}
