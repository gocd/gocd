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
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.configrepos.representers.ConfigRepoConfigRepresenterV1;
import com.thoughtworks.go.apiv1.configrepos.representers.ConfigReposConfigRepresenterV1;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.server.service.ConfigRepoService;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes.ConfigRepos;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.util.function.Consumer;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEntityAlreadyExists;
import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEtagDoesNotMatch;
import static spark.Spark.*;

@Component
public class ConfigReposControllerV1 extends ApiController implements SparkSpringController, CrudController<ConfigRepoConfig> {
    private final ApiAuthenticationHelper authHelper;
    private final ConfigRepoService service;
    private final EntityHashingService entityHashingService;

    @Autowired
    public ConfigReposControllerV1(ApiAuthenticationHelper authHelper, ConfigRepoService service, EntityHashingService entityHashingService) {
        super(ApiVersion.v1);
        this.service = service;
        this.authHelper = authHelper;
        this.entityHashingService = entityHashingService;
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
            before("", mimeType, this::verifyContentType);

            before("/*", mimeType, this::setContentType);
            before("/*", mimeType, authHelper::checkAdminUserAnd403);
            before("/*", mimeType, this::verifyContentType);

            get(ConfigRepos.INDEX_PATH, mimeType, this::index);
            get(ConfigRepos.REPO_PATH, mimeType, this::showRepo);
            post(ConfigRepos.CREATE_PATH, mimeType, this::createRepo);
            put(ConfigRepos.UPDATE_PATH, mimeType, this::updateRepo);
            delete(ConfigRepos.DELETE_PATH, mimeType, this::deleteRepo);

            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    String index(Request req, Response res) {
        ConfigReposConfig repos = allRepos();
        String etag = repos.etag();

        setEtagHeader(res, etag);

        if (fresh(req, etag)) {
            return notModified(res);
        }

        return jsonizeAsTopLevelObject(req, w -> ConfigReposConfigRepresenterV1.toJSON(w, repos));
    }

    String showRepo(Request req, Response res) {
        ConfigRepoConfig repo = fetchEntityFromConfig(req.params(":id"));
        String etag = etagFor(repo);

        setEtagHeader(res, etag);

        if (fresh(req, etag)) {
            return notModified(res);
        }

        return jsonize(req, repo);
    }

    String createRepo(Request req, Response res) {
        ConfigRepoConfig repo = buildEntityFromRequestBody(req);
        haltIfEntityWithSameIdExists(repo);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.createConfigRepo(repo, currentUsername(), result);

        return handleCreateOrUpdateResponse(req, res, repo, result);
    }

    String updateRepo(Request req, Response res) {
        String id = req.params(":id");
        ConfigRepoConfig repoFromConfig = fetchEntityFromConfig(id);
        ConfigRepoConfig repoFromRequest = buildEntityFromRequestBody(req);

        if (isPutRequestStale(req, repoFromConfig)) {
            throw haltBecauseEtagDoesNotMatch();
        }

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.updateConfigRepo(id, repoFromRequest, etagFor(repoFromConfig), currentUsername(), result);

        return handleCreateOrUpdateResponse(req, res, repoFromRequest, result);
    }

    String deleteRepo(Request req, Response res) {
        ConfigRepoConfig repo = fetchEntityFromConfig(req.params(":id"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        service.deleteConfigRepo(repo.getId(), currentUsername(), result);

        return handleSimpleMessageResponse(res, result);
    }

    @Override
    public String etagFor(ConfigRepoConfig repo) {
        return entityHashingService.md5ForEntity(repo);
    }

    @Override
    public ConfigRepoConfig doFetchEntityFromConfig(String id) {
        return service.getConfigRepo(id);
    }

    @Override
    public ConfigRepoConfig buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return ConfigRepoConfigRepresenterV1.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(ConfigRepoConfig repo) {
        return w -> ConfigRepoConfigRepresenterV1.toJSON(w, repo);
    }

    private ConfigReposConfig allRepos() {
        return service.getConfigRepos();
    }

    private void haltIfEntityWithSameIdExists(ConfigRepoConfig repo) {
        String id = repo.getId();
        if (doFetchEntityFromConfig(id) == null) {
            return;
        }

        repo.addError("id", "ConfigRepo ids should be unique. A ConfigRepo with the same id already exists.");
        throw haltBecauseEntityAlreadyExists(jsonWriter(repo), "config-repo", id);
    }
}
