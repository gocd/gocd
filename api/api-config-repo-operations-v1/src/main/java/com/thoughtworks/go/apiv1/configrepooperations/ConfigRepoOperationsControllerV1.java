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

package com.thoughtworks.go.apiv1.configrepooperations;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.spark.Routes.ConfigRepos;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static spark.Spark.before;
import static spark.Spark.path;

@Component
public class ConfigRepoOperationsControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper authenticationHelper;

    @Autowired
    public ConfigRepoOperationsControllerV1(ApiAuthenticationHelper authenticationHelper) {
        super(ApiVersion.v1);
        this.authenticationHelper = authenticationHelper;
    }

    @Override
    public String controllerBasePath() {
        return ConfigRepos.OPERATIONS_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("", mimeType, authenticationHelper::checkAdminUserAnd403);
            before("", this::verifyContentType);

            before("/*", mimeType, this::setContentType);
            before("/*", mimeType, authenticationHelper::checkAdminUserAnd403);
            before("/*", this::verifyContentType);
        });
    }
}
