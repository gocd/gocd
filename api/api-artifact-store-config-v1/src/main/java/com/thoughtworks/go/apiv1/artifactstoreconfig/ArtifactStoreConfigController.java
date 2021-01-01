/*
 * Copyright 2021 ThoughtWorks, Inc.
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


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.artifactstoreconfig.representers.ArtifactStoreRepresenter;
import com.thoughtworks.go.apiv1.artifactstoreconfig.representers.ArtifactStoresRepresenter;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.server.service.ArtifactStoreService;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.function.Consumer;

import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static spark.Spark.*;

@Component
public class ArtifactStoreConfigController extends ApiController implements SparkSpringController, CrudController<ArtifactStore> {
    private static final String ID_PARAM = "id";
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final ArtifactStoreService artifactStoreService;
    private final EntityHashingService entityHashingService;

    @Autowired
    public ArtifactStoreConfigController(ApiAuthenticationHelper apiAuthenticationHelper, ArtifactStoreService artifactStoreService,
                                         EntityHashingService entityHashingService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.artifactStoreService = artifactStoreService;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public String etagFor(ArtifactStore entityFromServer) {
        return entityHashingService.hashForEntity(entityFromServer);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ArtifactStore;
    }

    @Override
    public ArtifactStore doFetchEntityFromConfig(String name) {
        return artifactStoreService.findArtifactStore(name);
    }

    @Override
    public ArtifactStore buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return ArtifactStoreRepresenter.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(ArtifactStore artifactStore) {
        return writer -> ArtifactStoreRepresenter.toJSON(writer, artifactStore);
    }

    @Override
    public String controllerBasePath() {
        return Routes.ArtifactStoreConfig.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);

            get("", mimeType, this::index);
            get(Routes.ArtifactStoreConfig.ID, mimeType, this::show);
            post("", mimeType, this::create);
            put(Routes.ArtifactStoreConfig.ID, mimeType, this::update);
            delete(Routes.ArtifactStoreConfig.ID, mimeType, this::destroy);
        });
    }

    public String index(Request request, Response response) throws IOException {
        ArtifactStores artifactStores = artifactStoreService.getPluginProfiles();
        return writerForTopLevelObject(request, response,
                outputWriter -> ArtifactStoresRepresenter.toJSON(outputWriter, artifactStores));
    }

    public String show(Request request, Response response) throws IOException {
        ArtifactStore artifactStore = fetchEntityFromConfig(request.params(ID_PARAM));

        if (isGetOrHeadRequestFresh(request, artifactStore)) {
            return notModified(response);
        } else {
            setEtagHeader(artifactStore, response);
            return writerForTopLevelObject(request, response, writer -> ArtifactStoreRepresenter.toJSON(writer, artifactStore));
        }
    }

    public String create(Request request, Response response) {
        ArtifactStore artifactStore = buildEntityFromRequestBody(request);

        haltIfEntityWithSameIdExists(artifactStore);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        artifactStoreService.create(currentUsername(), artifactStore, result);

        return handleCreateOrUpdateResponse(request, response, artifactStore, result);
    }

    public String update(Request req, Response res) {
        ArtifactStore artifactStoreFromServer = fetchEntityFromConfig(req.params(ID_PARAM));
        ArtifactStore artifactStoreFromRequest = buildEntityFromRequestBody(req);

        if (isRenameAttempt(artifactStoreFromServer, artifactStoreFromRequest)) {
            throw haltBecauseRenameOfEntityIsNotSupported("artifactStore");
        }

        if (isPutRequestStale(req, artifactStoreFromServer)) {
            throw haltBecauseEtagDoesNotMatch("artifactStore", artifactStoreFromServer.getId());
        }

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        artifactStoreService.update(currentUsername(), etagFor(artifactStoreFromServer), artifactStoreFromRequest, result);
        return handleCreateOrUpdateResponse(req, res, artifactStoreFromRequest, result);
    }

    public String destroy(Request request, Response response) throws IOException {
        ArtifactStore artifactStore = fetchEntityFromConfig(request.params(ID_PARAM));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        artifactStoreService.delete(currentUsername(), artifactStore, result);

        return renderHTTPOperationResult(result, request, response);
    }

    private boolean isRenameAttempt(ArtifactStore fromServer, ArtifactStore fromRequest) {
        return !fromServer.getId().equalsIgnoreCase(fromRequest.getId());
    }

    private void haltIfEntityWithSameIdExists(ArtifactStore artifactStore) {
        if (artifactStoreService.findArtifactStore(artifactStore.getId()) == null) {
            return;
        }
        artifactStore.addError("id", "Artifact store ids should be unique. Artifact store with the same id exists.");
        throw haltBecauseEntityAlreadyExists(jsonWriter(artifactStore), "artifactStore", artifactStore.getId());
    }
}
