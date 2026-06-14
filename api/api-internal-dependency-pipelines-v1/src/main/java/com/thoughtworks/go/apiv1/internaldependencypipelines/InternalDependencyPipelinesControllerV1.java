/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.api.spring.ApiAuthorizationHelper;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.server.presentation.FetchArtifactViewHelper;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.spark.GlobalExceptionMapper;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static spark.Spark.*;

@Component
public class InternalDependencyPipelinesControllerV1 extends ApiController implements SparkSpringController {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private final ApiAuthorizationHelper apiAuthorizationHelper;
    private final GoConfigService goConfigService;

    @Autowired
    public InternalDependencyPipelinesControllerV1(ApiAuthorizationHelper apiAuthorizationHelper,
                                                   GoConfigService goConfigService) {
        super(ApiVersion.v1);
        this.apiAuthorizationHelper = apiAuthorizationHelper;
        this.goConfigService = goConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.InternalDependencyPipelines.BASE;
    }

    @Override
    public void setupRoutes(GlobalExceptionMapper exceptionMapper) {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);

            before("", mimeType, this::checkPipelineOrTemplateViewPermissionsAnd403);

            get("", mimeType, this::index);
        });
    }

    void checkPipelineOrTemplateViewPermissionsAnd403(Request request, Response response) {
        if (isTemplateRequest(request)) {
            apiAuthorizationHelper.checkViewAccessToTemplateAnd403(request, response, r -> r.params("pipeline_name"));
        } else {
            apiAuthorizationHelper.checkPipelineViewPermissionsAnd403(request, response);
        }
    }

    public String index(Request request, Response response) {
        CruiseConfig config = goConfigService.getMergedConfigForEditing();
        FetchArtifactViewHelper helper = new FetchArtifactViewHelper(config,
            cis(request.params("pipeline_name")),
            cis(request.params("stage_name")),
            isTemplateRequest(request),
            name -> apiAuthorizationHelper.hasViewPermissionForPipeline(name.toString()));

        response.type("application/json");
        return GSON.toJson(helper.autosuggestMap());
    }

    private static boolean isTemplateRequest(Request request) {
        return isNotBlank(request.queryParams("template"));
    }

}
