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
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

@Component
public class MaterialNotifyControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final MaterialUpdateService materialUpdateService;

    @Autowired
    public MaterialNotifyControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, MaterialUpdateService materialUpdateService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.materialUpdateService = materialUpdateService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.MaterialNotify.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);
            before("", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            post(Routes.MaterialNotify.SVN, mimeType, this::svnNotify);
            post(Routes.MaterialNotify.GIT, mimeType, this::gitNotify);
            post(Routes.MaterialNotify.HG, mimeType, this::hgNotify);
            post(Routes.MaterialNotify.SCM, mimeType, this::scmNotify);
        });
    }

    public String svnNotify(Request req, Response res) throws IOException {
        return notifyWithRepoUrl(req, res, "svn");
    }

    public String gitNotify(Request req, Response res) throws IOException {
        return notifyWithRepoUrl(req, res, "git");
    }

    public String hgNotify(Request req, Response res) throws IOException {
        return notifyWithRepoUrl(req, res, "hg");
    }

    public String scmNotify(Request req, Response res) throws IOException {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        String repoUrl = jsonReader.getString("scm_name");
        return notify(req, res, "scm", "scm_name", repoUrl);
    }

    private String notifyWithRepoUrl(Request req, Response res, String materialType) throws IOException {
        String repoUrl = getRepoUrl(req);
        return notify(req, res, materialType, "repository_url", repoUrl);
    }

    private String notify(Request req, Response res, String materialType, String paramName, String paramValue) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put(MaterialUpdateService.TYPE, materialType);
        params.put(paramName, paramValue);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        materialUpdateService.notifyMaterialsForUpdate(currentUsername(), params, result);

        return renderHTTPOperationResult(result, req, res);
    }

    private String getRepoUrl(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return jsonReader.getString("repository_url");
    }
}
