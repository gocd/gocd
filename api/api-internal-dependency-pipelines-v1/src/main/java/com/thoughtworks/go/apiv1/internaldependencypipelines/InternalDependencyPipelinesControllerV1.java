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

package com.thoughtworks.go.apiv1.internaldependencypipelines;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.server.presentation.FetchArtifactViewHelper;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import static spark.Spark.*;

@Component
public class InternalDependencyPipelinesControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private SystemEnvironment systemEnvironment;
    private GoConfigService goConfigService;
    private Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    @Autowired
    public InternalDependencyPipelinesControllerV1(ApiAuthenticationHelper apiAuthenticationHelper,
                                                   SystemEnvironment systemEnvironment,
                                                   GoConfigService goConfigService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.systemEnvironment = systemEnvironment;
        this.goConfigService = goConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.InternalDependencyPipelines.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);

            before("", mimeType, apiAuthenticationHelper::checkUserAnd403);

            get("", mimeType, this::index);
        });
    }

    public String index(Request request, Response response) {
        String pipelineName = request.params("pipeline_name");
        String stageName = request.params("stage_name");

        CruiseConfig config = goConfigService.getMergedConfigForEditing();
        FetchArtifactViewHelper helper = new FetchArtifactViewHelper(systemEnvironment, config, new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName), false);

        response.type("application/json");
        return gson.toJson(helper.autosuggestMap());
    }

}
