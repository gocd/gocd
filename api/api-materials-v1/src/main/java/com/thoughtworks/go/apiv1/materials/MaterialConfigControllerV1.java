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
package com.thoughtworks.go.apiv1.materials;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.materials.representers.MaterialConfigsRepresenter;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.server.service.MaterialConfigService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;

@Component
public class MaterialConfigControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final MaterialConfigService materialConfigService;

    @Autowired
    public MaterialConfigControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, MaterialConfigService materialConfigService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.materialConfigService = materialConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.MaterialConfig.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);
            before("", this.mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            before("/*", this.mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            get("", mimeType, this::index);
        });
    }

    public String index(Request req, Response res) throws IOException {
        MaterialConfigs materialConfigs = materialConfigService.getMaterialConfigs(currentUsernameString());
        return writerForTopLevelObject(req, res, writer -> MaterialConfigsRepresenter.toJSON(writer, materialConfigs));
    }
}
