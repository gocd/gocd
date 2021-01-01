/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv1.dependencymaterialautocomplete;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.dependencymaterialautocomplete.representers.SuggestionsRepresenter;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;

@Component
public class DependencyMaterialAutocompleteControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private GoConfigService configService;

    @Autowired
    public DependencyMaterialAutocompleteControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, GoConfigService configService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.configService = configService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.DependencyMaterialAutocomplete.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            // change the line below to enable appropriate security
            before("", this.mimeType, this.apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403);
            get("", mimeType, this::suggest);
            head("", mimeType, this::suggest);
        });
    }

    String suggest(Request req, Response res) throws IOException {
        return writerForTopLevelArray(req, res, w -> SuggestionsRepresenter.toJSON(w, configService.getAllLocalPipelineConfigs()));
    }
}
