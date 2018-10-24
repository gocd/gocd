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

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.apiv1.configrepos.representers.ConfigRepoRepresenterV2;
import com.thoughtworks.go.apiv1.configrepos.representers.ConfigReposRepresenterV2;
import com.thoughtworks.go.config.GoRepoConfigDataSource;
import com.thoughtworks.go.config.PartialConfigParseResult;
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

import static spark.Spark.*;

@Component
public class ConfigReposInternalControllerV1 extends ApiController implements SparkSpringController {

    private final ConfigRepoService service;
    private final GoRepoConfigDataSource dataSource;
    private final ApiAuthenticationHelper authHelper;

    @Autowired
    public ConfigReposInternalControllerV1(ApiAuthenticationHelper authHelper, ConfigRepoService service, GoRepoConfigDataSource dataSource) {
        super(ApiVersion.v1);
        this.service = service;
        this.dataSource = dataSource;
        this.authHelper = authHelper;
    }

    @Override
    public String controllerBasePath() {
        return ConfigRepos.INTERNAL_BASE;
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

            get(ConfigRepos.INDEX_PATH, mimeType, this::listRepos);
            get(ConfigRepos.REPO_PATH, mimeType, this::showRepo);
        });
    }

    String listRepos(Request req, Response res) throws IOException {
        ConfigReposConfig repos = service.getConfigRepos();
        return writerForTopLevelObject(req, res, w -> ConfigReposRepresenterV2.toJSON(w, repos, r -> dataSource.getLastParseResult(r.getMaterialConfig())));
    }

    String showRepo(Request req, Response res) throws IOException {
        ConfigRepoConfig repo = repoFromRequest(req);
        PartialConfigParseResult result = dataSource.getLastParseResult(repo.getMaterialConfig());
        return writerForTopLevelObject(req, res, w -> ConfigRepoRepresenterV2.toJSON(w, repo, result));
    }

    private ConfigRepoConfig repoFromRequest(Request req) {
        ConfigRepoConfig repo = service.getConfigRepo(req.params(":id"));

        if (null == repo) {
            throw HaltApiResponses.haltBecauseNotFound();
        }

        return repo;
    }

}
