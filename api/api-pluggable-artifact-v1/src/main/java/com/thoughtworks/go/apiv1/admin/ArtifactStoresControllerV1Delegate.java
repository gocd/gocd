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

package com.thoughtworks.go.apiv1.admin;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.spring.AuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.config.InvalidPluginTypeException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.ArtifactStoreService;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.util.UserHelper;
import gen.com.thoughtworks.go.apiv1.admin.representers.ArtifactStoreMapper;
import gen.com.thoughtworks.go.apiv1.admin.representers.ArtifactStoresMapper;
import org.springframework.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.util.Collections;
import java.util.Map;

import static com.thoughtworks.go.api.util.HaltResponses.*;
import static spark.Spark.*;

public class ArtifactStoresControllerV1Delegate extends ApiController implements CrudController<ArtifactStore> {
    private final ArtifactStoreService artifactStoreService;
    private final EntityHashingService entityHashingService;
    private final AuthenticationHelper authenticationHelper;
    private final Localizer localizer;

    public ArtifactStoresControllerV1Delegate(ArtifactStoreService artifactStoreService, EntityHashingService entityHashingService, AuthenticationHelper authenticationHelper, Localizer localizer) {
        super(ApiVersion.v1);
        this.artifactStoreService = artifactStoreService;
        this.entityHashingService = entityHashingService;
        this.authenticationHelper = authenticationHelper;
        this.localizer = localizer;
    }

    @Override
    public String controllerBasePath() {
        return "/api/admin/artifact_stores";
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", this::verifyContentType);
            before("/*", this::verifyContentType);
            before("", mimeType, authenticationHelper::checkAdminUserAnd401);
            before("/*", mimeType, authenticationHelper::checkAdminUserAnd401);

            get("", mimeType, this::index, GsonTransformer.getInstance());
            post("", mimeType, this::create, GsonTransformer.getInstance());

            get("/:storeId", mimeType, this::show, GsonTransformer.getInstance());
            put("/:storeId", mimeType, this::update, GsonTransformer.getInstance());
            delete("/:storeId", mimeType, this::destroy, GsonTransformer.getInstance());

            exception(InvalidPluginTypeException.class, (ex, req, res) -> {
                res.body(this.messageJson(ex));
                res.status(HttpStatus.BAD_REQUEST.value());
            });

            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    public Object index(Request req, Response res) throws InvalidPluginTypeException {
        final ArtifactStores artifactStores = artifactStoreService.getPluginProfiles();
        final String etag = entityHashingService.md5ForEntity(artifactStores);
        if (fresh(req, etag)) {
            return notModified(res);
        } else {
            setEtagHeader(res, etag);
            return ArtifactStoresMapper.toJSON(artifactStores, requestContext(req));
        }
    }

    public Object show(Request req, Response res) {
        ArtifactStore artifactStore = getEntityFromConfig(req.params("storeId"));

        if (isGetOrHeadRequestFresh(req, artifactStore)) {
            return notModified(res);
        } else {
            setEtagHeader(artifactStore, res);
            return jsonize(req, artifactStore);
        }
    }

    public Map create(Request req, Response res) {
        ArtifactStore artifactStore = getEntityFromRequestBody(req);

        haltIfEntityBySameNameInRequestExists(req, artifactStore);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        artifactStoreService.create(UserHelper.getUserName(), artifactStore, result);

        return handleCreateOrUpdateResponse(req, res, artifactStore, result);
    }

    public Map update(Request req, Response res) {
        ArtifactStore artifactStoreFromServer = artifactStoreService.findArtifactStore(req.params("storeId"));
        ArtifactStore artifactStoreFromRequest = getEntityFromRequestBody(req);

        if (isRenameAttempt(artifactStoreFromServer, artifactStoreFromRequest)) {
            throw haltBecauseRenameOfEntityIsNotSupported("artifactStore");
        }

        if (!isPutRequestFresh(req, artifactStoreFromServer)) {
            throw haltBecauseEtagDoesNotMatch("artifactStore", artifactStoreFromServer.getId());
        }

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        artifactStoreService.update(UserHelper.getUserName(), etagFor(artifactStoreFromServer), artifactStoreFromRequest, result);
        return handleCreateOrUpdateResponse(req, res, artifactStoreFromRequest, result);
    }

    public Map destroy(Request req, Response res) {
        ArtifactStore artifactStore = getEntityFromConfig(req.params("storeId"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        artifactStoreService.delete(UserHelper.getUserName(), artifactStore, result);

        res.status(result.httpCode());
        return Collections.singletonMap("message", result.message(localizer));
    }

    @Override
    public String etagFor(ArtifactStore entityFromServer) {
        return entityHashingService.md5ForEntity(entityFromServer);
    }

    @Override
    public boolean isRenameAttempt(ArtifactStore fromServer, ArtifactStore fromRequest) {
        return !fromServer.getId().equalsIgnoreCase(fromRequest.getId());
    }

    @Override
    public Localizer getLocalizer() {
        return localizer;
    }

    @Override
    public ArtifactStore doGetEntityFromConfig(String storeId) {
        return artifactStoreService.findArtifactStore(storeId);
    }

    @Override
    public ArtifactStore getEntityFromRequestBody(Request req) {
        return ArtifactStoreMapper.fromJSON(GsonTransformer.getInstance().fromJson(req.body(), Map.class), requestContext(req));
    }

    @Override
    public Map jsonize(Request req, ArtifactStore artifactStore) {
        return ArtifactStoreMapper.toJSON(artifactStore, requestContext(req));
    }

    private void haltIfEntityBySameNameInRequestExists(Request req, ArtifactStore artifactStore) {
        if (artifactStoreService.findArtifactStore(artifactStore.getId()) == null) {
            return;
        }
        artifactStore.addError("id", "Artifact store ids should be unique. Artifact store with the same id exists.");
        throw haltBecauseEntityAlreadyExists(jsonize(req, artifactStore), "artifactStore", artifactStore.getId());
    }
}
