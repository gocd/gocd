/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv2.environments;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv2.environments.representers.EnvironmentsRepresenter;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.server.service.EnvironmentConfigService;
import com.thoughtworks.go.server.ui.EnvironmentViewModel;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;

import static spark.Spark.*;

@Component
public class EnvironmentsControllerV2 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EnvironmentConfigService environmentConfigService;

    @Autowired
    public EnvironmentsControllerV2(ApiAuthenticationHelper apiAuthenticationHelper, EnvironmentConfigService environmentConfigService) {
        super(ApiVersion.v2);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.environmentConfigService = environmentConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Environments.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", this::setContentType);
            before("/*", this::setContentType);

            before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            get("", mimeType, this::index);
        });
    }

    public String index(Request request, Response response) throws IOException {
        List<EnvironmentConfig> environmentViewModelList = environmentConfigService.getAllMergedEnvironments();
        return writerForTopLevelObject(request, response,
                outputWriter -> EnvironmentsRepresenter.toJSON(outputWriter, environmentViewModelList));
    }
}
