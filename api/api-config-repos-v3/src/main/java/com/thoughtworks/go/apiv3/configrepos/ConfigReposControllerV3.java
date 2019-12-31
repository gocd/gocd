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

package com.thoughtworks.go.apiv3.configrepos;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv3.configrepos.representers.ConfigRepoConfigRepresenterV3;
import com.thoughtworks.go.apiv3.configrepos.representers.ConfigReposConfigRepresenterV3;
import com.thoughtworks.go.apiv3.configrepos.representers.PartialConfigRepresenter;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.service.ConfigRepoService;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.util.function.Consumer;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEntityAlreadyExists;
import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEtagDoesNotMatch;
import static com.thoughtworks.go.config.policy.SupportedEntity.CONFIG_REPO;

import static com.thoughtworks.go.util.CachedDigestUtils.sha256Hex;
import static java.util.stream.Collectors.toCollection;
import static spark.Spark.*;

@Component
public class ConfigReposControllerV3 extends ApiController implements SparkSpringController, CrudController<ConfigRepoConfig> {
    private final ApiAuthenticationHelper authHelper;
    private final ConfigRepoService service;
    private final EntityHashingService entityHashingService;
    private final MaterialUpdateService materialUpdateService;
    private final MaterialConfigConverter converter;

    @Autowired
    public ConfigReposControllerV3(ApiAuthenticationHelper authHelper, ConfigRepoService service, EntityHashingService entityHashingService, MaterialUpdateService materialUpdateService, MaterialConfigConverter converter) {
        super(ApiVersion.v3);
        this.authHelper = authHelper;
        this.service = service;
        this.entityHashingService = entityHashingService;
        this.materialUpdateService = materialUpdateService;
        this.converter = converter;
    }

    @Override
    public String controllerBasePath() {
        return Routes.ConfigRepos.BASE;
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

            before(Routes.ConfigRepos.REPO_PATH, mimeType, this::authorize);
            before(Routes.ConfigRepos.STATUS_PATH, mimeType, this::authorize);
            before(Routes.ConfigRepos.TRIGGER_UPDATE_PATH, mimeType, this::authorize);
            before(Routes.ConfigRepos.DEFINITIONS_PATH, mimeType, this::authorize);

            get(Routes.ConfigRepos.INDEX_PATH, mimeType, this::index);
            get(Routes.ConfigRepos.REPO_PATH, mimeType, this::showRepo);
            post(Routes.ConfigRepos.CREATE_PATH, mimeType, this::createRepo);
            put(Routes.ConfigRepos.UPDATE_PATH, mimeType, this::updateRepo);
            delete(Routes.ConfigRepos.DELETE_PATH, mimeType, this::deleteRepo);
            get(Routes.ConfigRepos.STATUS_PATH, mimeType, this::inProgress);
            post(Routes.ConfigRepos.TRIGGER_UPDATE_PATH, mimeType, this::triggerUpdate);
            get(Routes.ConfigRepos.DEFINITIONS_PATH, mimeType, this::definedConfigs);
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

        return jsonizeAsTopLevelObject(req, w -> ConfigReposConfigRepresenterV3.toJSON(w, userSpecificRepos));
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

    String inProgress(Request req, Response res) {
        MaterialConfig materialConfig = repoFromRequest(req).getMaterialConfig();
        final boolean state = materialUpdateService.isInProgress(converter.toMaterial(materialConfig));
        return String.format("{\"in_progress\":%b}", state);
    }

    String triggerUpdate(Request req, Response res) {
        MaterialConfig materialConfig = repoFromRequest(req).getMaterialConfig();
        if (materialUpdateService.updateMaterial(converter.toMaterial(materialConfig))) {
            res.status(HttpStatus.CREATED.value());
            return MessageJson.create("OK");
        } else {
            res.status(HttpStatus.CONFLICT.value());
            return MessageJson.create("Update already in progress.");
        }
    }

    String definedConfigs(Request req, Response res) {
        ConfigRepoConfig repo = repoFromRequest(req);
        PartialConfig def = service.partialConfigDefinedBy(repo);

        final String etag = etagFor(def);

        setEtagHeader(res, etag);

        if (fresh(req, etag)) {
            return notModified(res);
        }

        return jsonizeAsTopLevelObject(req, (w) -> PartialConfigRepresenter.toJSON(w, def));
    }

    private ConfigRepoConfig repoFromRequest(Request req) {
        String repoId = req.params(":id");
        ConfigRepoConfig repo = service.getConfigRepo(repoId);

        if (null == repo) {
            throw new RecordNotFoundException(EntityType.ConfigRepo, repoId);
        }

        return repo;
    }

    @Override
    public String etagFor(ConfigRepoConfig repo) {
        return entityHashingService.md5ForEntity(repo);
    }

    private String etagFor(PartialConfig entity) {
        return sha256Hex(Integer.toString(entity.hashCode()));
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
        return ConfigRepoConfigRepresenterV3.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(ConfigRepoConfig repo) {
        return w -> ConfigRepoConfigRepresenterV3.toJSON(w, repo);
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

    private void authorize(Request request, Response response) {
        authHelper.checkUserHasPermissions(currentUsername(), getAction(request), CONFIG_REPO, request.params(":id"));
    }
}
