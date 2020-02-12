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
package com.thoughtworks.go.apiv2.configrepos;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv2.configrepos.representers.ConfigRepoConfigRepresenterV2;
import com.thoughtworks.go.apiv2.configrepos.representers.ConfigReposConfigRepresenterV2;
import com.thoughtworks.go.spark.DeprecatedAPI;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.server.service.ConfigRepoService;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.util.function.Consumer;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEntityAlreadyExists;
import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEtagDoesNotMatch;
import static com.thoughtworks.go.config.policy.SupportedEntity.CONFIG_REPO;
import static com.thoughtworks.go.spark.Routes.ConfigRepos;
import static java.util.stream.Collectors.toCollection;
import static spark.Spark.*;

@Component
@DeprecatedAPI(deprecatedApiVersion = ApiVersion.v2, successorApiVersion = ApiVersion.v3, deprecatedIn = "20.2.0", removalIn = "20.5.0", entityName = "Config Repo")
public class ConfigReposControllerV2 extends ApiController implements SparkSpringController, CrudController<ConfigRepoConfig> {
    private final ApiAuthenticationHelper authHelper;
    private final ConfigRepoService service;
    private final EntityHashingService entityHashingService;

    @Autowired
    public ConfigReposControllerV2(ApiAuthenticationHelper authHelper, ConfigRepoService service, EntityHashingService entityHashingService) {
        super(ApiVersion.v2);
        this.authHelper = authHelper;
        this.service = service;
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
            before("", mimeType, this::verifyContentType);

            before("", mimeType, (request, response) -> {
                String resourceToOperateOn = "*";
                if (request.requestMethod().equalsIgnoreCase("GET")) {
                    authHelper.checkUserAnd403(request, response);
                    return;
                }

                if (request.requestMethod().equalsIgnoreCase("POST")) {
                    resourceToOperateOn = GsonTransformer.getInstance().jsonReaderFrom(request.body()).getString("id");
                }

                authHelper.checkUserHasPermissions(currentUsername(), getAction(request), CONFIG_REPO, resourceToOperateOn);
            });

            before("/*", mimeType, this::setContentType);
            before("/*", mimeType, this::verifyContentType);

            before(ConfigRepos.REPO_PATH, mimeType, (request, response) -> {
                authHelper.checkUserHasPermissions(currentUsername(), getAction(request), CONFIG_REPO, request.params(":id"));
            });

            get(ConfigRepos.INDEX_PATH, mimeType, this::index);
            get(ConfigRepos.REPO_PATH, mimeType, this::showRepo);
            post(ConfigRepos.CREATE_PATH, mimeType, this::createRepo);
            put(ConfigRepos.UPDATE_PATH, mimeType, this::updateRepo);
            delete(ConfigRepos.DELETE_PATH, mimeType, this::deleteRepo);
        });
    }

    String index(Request req, Response res) {
        ConfigReposConfig repos = allRepos();

        ConfigReposConfig userSpecificRepos = repos.stream()
                .filter(configRepoConfig -> authHelper.doesUserHasPermissions(currentUsername(), getAction(req), CONFIG_REPO, configRepoConfig.getId()))
                .collect(toCollection(ConfigReposConfig::new));

        String etag = userSpecificRepos.etag();

        setEtagHeader(res, etag);

        if (fresh(req, etag)) {
            return notModified(res);
        }

        return jsonizeAsTopLevelObject(req, w -> ConfigReposConfigRepresenterV2.toJSON(w, userSpecificRepos));
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
    public EntityType getEntityType() {
        return EntityType.ConfigRepo;
    }

    @Override
    public ConfigRepoConfig doFetchEntityFromConfig(String id) {
        return service.getConfigRepo(id);
    }

    @Override
    public ConfigRepoConfig buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return ConfigRepoConfigRepresenterV2.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(ConfigRepoConfig repo) {
        return w -> ConfigRepoConfigRepresenterV2.toJSON(w, repo);
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
