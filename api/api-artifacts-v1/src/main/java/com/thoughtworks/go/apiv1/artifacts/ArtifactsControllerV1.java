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

package com.thoughtworks.go.apiv1.artifacts;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.config.ArtifactConfig;
import com.thoughtworks.go.config.PurgeSettings;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.server.service.ServerConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;

@Component
public class ArtifactsControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private ArtifactsService artifactsService;

    @Autowired
    public ArtifactsControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, ArtifactsService artifactsService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.artifactsService = artifactsService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Artifacts.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> { ;
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);
            before(Routes.Artifacts.PURGE_PATH, mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            post(Routes.Artifacts.PURGE_PATH, mimeType, this::purgeOldArtifacts);
        });
    }

    public String purgeOldArtifacts(Request req, Response res) throws IOException {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        Integer purgeThreshold = jsonReader.getInt("purge_threshold");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        artifactsService.purgeOldArtifacts(purgeThreshold, result);
        return renderHTTPOperationResult(result, req, res);
    }


}
