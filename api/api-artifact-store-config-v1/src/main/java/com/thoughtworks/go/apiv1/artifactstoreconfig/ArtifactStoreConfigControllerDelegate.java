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

package com.thoughtworks.go.apiv1.artifactstoreconfig;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.artifactstoreconfig.representers.ArtifactStoreRepresenter;
import com.thoughtworks.go.apiv1.artifactstoreconfig.representers.ArtifactStoresRepresenter;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.ArtifactStoreService;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static spark.Spark.*;

public class ArtifactStoreConfigControllerDelegate extends ApiController implements CrudController<ArtifactStore> {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final ArtifactStoreService artifactStoreService;
    private final EntityHashingService entityHashingService;
    private final Localizer localizer;

    public ArtifactStoreConfigControllerDelegate(ApiAuthenticationHelper apiAuthenticationHelper, ArtifactStoreService artifactStoreService,
                                                 EntityHashingService entityHashingService, Localizer localizer) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.artifactStoreService = artifactStoreService;
        this.entityHashingService = entityHashingService;
        this.localizer = localizer;
    }

    @Override
    public String etagFor(ArtifactStore entityFromServer) {
        return entityHashingService.md5ForEntity(entityFromServer);
    }

    @Override
    public Localizer getLocalizer() {
        return localizer;
    }

    @Override
    public ArtifactStore doGetEntityFromConfig(String name) {
        return artifactStoreService.findArtifactStore(name);
    }

    @Override
    public ArtifactStore getEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return ArtifactStoreRepresenter.fromJSON(jsonReader);
    }

    @Override
    public String jsonize(Request req, ArtifactStore artifactStore) {
        return jsonizeAsTopLevelObject(req, writer -> ArtifactStoreRepresenter.toJSON(writer, artifactStore));
    }

    @Override
    public JsonNode jsonNode(Request req, ArtifactStore artifactStore) throws IOException {
        String jsonize = jsonize(req, artifactStore);
        return new ObjectMapper().readTree(jsonize);
    }

    @Override
    public String controllerBasePath() {
        return Routes.ArtifactStoreConfig.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", this::setContentType);
            before("/*", this::setContentType);

            before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd401);
            before("/*", mimeType, apiAuthenticationHelper::checkAdminUserAnd401);

            get("", this::index);
            post("", this::create);
            put(Routes.ArtifactStoreConfig.NAME, this::update);
            delete(Routes.ArtifactStoreConfig.NAME, this::destroy);

            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    public String index(Request request, Response response) throws IOException {
        ArtifactStores artifactStores = artifactStoreService.getPluginProfiles();
        return writerForTopLevelObject(request, response,
                outputWriter -> ArtifactStoresRepresenter.toJSON(outputWriter, artifactStores));
    }

    public String create(Request request, Response response) throws IOException {
        ArtifactStore artifactStore = getEntityFromRequestBody(request);

        haltIfEntityWithSameIdExists(request, artifactStore);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        artifactStoreService.create(currentUsername(), artifactStore, result);

        return handleCreateOrUpdateResponse(request, response, artifactStore, result);
    }

    public String update(Request req, Response res) throws IOException {
        ArtifactStore artifactStoreFromServer = getEntityFromConfig(req.params("store_id"));
        ArtifactStore artifactStoreFromRequest = getEntityFromRequestBody(req);

        if (isRenameAttempt(artifactStoreFromServer, artifactStoreFromRequest)) {
            throw haltBecauseRenameOfEntityIsNotSupported("artifactStore");
        }

        if (!isPutRequestFresh(req, artifactStoreFromServer)) {
            throw haltBecauseEtagDoesNotMatch("artifactStore", artifactStoreFromServer.getId());
        }

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        artifactStoreService.update(currentUsername(), etagFor(artifactStoreFromServer), artifactStoreFromRequest, result);
        return handleCreateOrUpdateResponse(req, res, artifactStoreFromRequest, result);
    }

    public String destroy(Request request, Response response) throws IOException {
        ArtifactStore artifactStore = getEntityFromConfig(request.params("store_id"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        artifactStoreService.delete(currentUsername(), artifactStore, result);

        return renderHTTPOperationResult(result, request, response, localizer);
    }

    private boolean isRenameAttempt(ArtifactStore fromServer, ArtifactStore fromRequest) {
        return !fromServer.getId().equalsIgnoreCase(fromRequest.getId());
    }

    private void haltIfEntityWithSameIdExists(Request req, ArtifactStore artifactStore) throws IOException {
        if (artifactStoreService.findArtifactStore(artifactStore.getId()) == null) {
            return;
        }
        artifactStore.addError("id", "Artifact store ids should be unique. Artifact store with the same id exists.");
        throw haltBecauseEntityAlreadyExists(jsonNode(req, artifactStore), "artifactStore", artifactStore.getId());
    }
}
