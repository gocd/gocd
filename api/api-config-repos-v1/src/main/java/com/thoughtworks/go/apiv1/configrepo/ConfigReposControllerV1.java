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

package com.thoughtworks.go.apiv1.configrepo;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv1.configrepo.representers.PartialConfigParseResultRepresenter;
import com.thoughtworks.go.config.GoRepoConfigDataSource;
import com.thoughtworks.go.config.PartialConfigParseResult;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.service.ConfigRepoService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.util.Objects;

import static spark.Spark.*;

@Component
public class ConfigReposControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper authenticationHelper;
    private final GoRepoConfigDataSource goRepoConfigDataSource;
    private final ConfigRepoService configRepoService;
    private final MaterialUpdateService mus;
    private final MaterialConfigConverter converter;

    @Autowired
    public ConfigReposControllerV1(ApiAuthenticationHelper authenticationHelper, GoRepoConfigDataSource goRepoConfigDataSource, ConfigRepoService configRepoService, MaterialUpdateService mus, MaterialConfigConverter converter) {
        super(ApiVersion.v1);
        this.authenticationHelper = authenticationHelper;
        this.goRepoConfigDataSource = goRepoConfigDataSource;
        this.configRepoService = configRepoService;
        this.mus = mus;
        this.converter = converter;
    }

    @Override
    public String controllerBasePath() {
        return Routes.ConfigRepos.INTERNAL_BASE;
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

            get(Routes.ConfigRepos.LAST_PARSED_RESULT_PATH, mimeType, this::getLastParseResult);
            post(Routes.ConfigRepos.TRIGGER_UPDATE_PATH, mimeType, this::triggerUpdate);
        });
    }

    String getLastParseResult(Request req, Response res) {
        ConfigRepoConfig repo = Objects.requireNonNull(getRepoFromRequest(req));

        final PartialConfigParseResult result = goRepoConfigDataSource.getLastParseResult(repo.getMaterialConfig());

        return PartialConfigParseResultRepresenter.toJSON(result);
    }

    private ConfigRepoConfig getRepoFromRequest(Request req) {
        ConfigRepoConfig repo = configRepoService.getConfigRepo(req.params(":id"));

        if (null == repo) {
            HaltApiResponses.haltBecauseNotFound();
        }

        return repo;
    }

    String triggerUpdate(Request req, Response res) {
        MaterialConfig materialConfig = getRepoFromRequest(req).getMaterialConfig();
        if (mus.updateMaterial(converter.toMaterial(materialConfig))) {
            return MessageJson.create("OK");
        } else {
            return MessageJson.create("Update already in progress");
        }
    }
}
