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

package com.thoughtworks.go.apiv1.configrepos;

import com.fasterxml.jackson.databind.JsonNode;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.configrepos.representers.ConfigRepoConfigRepresenterV1;
import com.thoughtworks.go.apiv1.configrepos.representers.ConfigReposConfigRepresenterV1;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.server.service.ConfigRepoService;
import com.thoughtworks.go.spark.Routes.ConfigRepos;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static com.thoughtworks.go.util.CachedDigestUtils.sha256Hex;
import static spark.Spark.*;

@Component
public class ConfigReposControllerV1 extends ApiController implements SparkSpringController, CrudController<ConfigRepoConfig> {
    private final ConfigRepoService service;
    private final ApiAuthenticationHelper authHelper;


    @Autowired
    public ConfigReposControllerV1(ConfigRepoService service, ApiAuthenticationHelper authHelper) {
        super(ApiVersion.v1);
        this.service = service;
        this.authHelper = authHelper;
    }

    @Override
    public String controllerBasePath() {
        return ConfigRepos.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("", mimeType, authHelper::checkAdminUserAnd403);
            before("", this::verifyContentType);

            before("/*", mimeType, this::setContentType);
            before("/*", mimeType, authHelper::checkAdminUserAnd403);
            before("/*", this::verifyContentType);

            get(ConfigRepos.INDEX_PATH, mimeType, this::index);
            get(ConfigRepos.REPO_PATH, mimeType, this::showRepo);
        });
    }

    String index(Request req, Response res) {
        return jsonizeAsTopLevelObject(req, w -> ConfigReposConfigRepresenterV1.toJSON(w, allRepos()));
    }

    String showRepo(Request req, Response res) {
        ConfigRepoConfig repoConfig = getEntityFromConfig(req.params(":id"));
        return jsonize(req, repoConfig);
    }

    private ConfigReposConfig allRepos() {
        return service.getConfigRepos();
    }

    @Override
    public String etagFor(ConfigRepoConfig entityFromServer) {
        return sha256Hex(Integer.toString(entityFromServer.hashCode()));
    }

    @Override
    public ConfigRepoConfig doGetEntityFromConfig(String id) {
        return service.getConfigRepo(id);
    }

    @Override
    public ConfigRepoConfig getEntityFromRequestBody(Request req) {
        return null;
    }

    @Override
    public String jsonize(Request req, ConfigRepoConfig repo) {
        return jsonizeAsTopLevelObject(req, w -> ConfigRepoConfigRepresenterV1.toJSON(w, repo));
    }

    @Override
    public JsonNode jsonNode(Request req, ConfigRepoConfig configRepoConfig) throws IOException {
        return null;
    }
}
