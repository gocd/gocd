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
package com.thoughtworks.go.apiv2.clusterprofiles;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv2.clusterprofiles.representers.ClusterProfileRepresenter;
import com.thoughtworks.go.apiv2.clusterprofiles.representers.ClusterProfilesRepresenter;
import com.thoughtworks.go.config.PluginProfiles;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.policy.SupportedAction;
import com.thoughtworks.go.config.policy.SupportedEntity;
import com.thoughtworks.go.server.service.ClusterProfilesService;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static com.thoughtworks.go.config.policy.SupportedEntity.CLUSTER_PROFILE;
import static java.lang.String.format;
import static spark.Spark.*;

@Component
public class ClusterProfilesControllerV2 extends ApiController implements SparkSpringController, CrudController<ClusterProfile> {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final ClusterProfilesService clusterProfilesService;
    private EntityHashingService entityHashingService;

    @Autowired
    public ClusterProfilesControllerV2(ApiAuthenticationHelper apiAuthenticationHelper, ClusterProfilesService clusterProfilesService, EntityHashingService entityHashingService) {
        super(ApiVersion.v2);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.clusterProfilesService = clusterProfilesService;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.ClusterProfilesAPI.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, (request, response) -> {
                String resourceToOperateOn = "*";
                if (request.requestMethod().equalsIgnoreCase("GET")) {
                    apiAuthenticationHelper.checkUserAnd403(request, response);
                    return;
                }


                if (request.requestMethod().equalsIgnoreCase("POST")) {
                    resourceToOperateOn = GsonTransformer.getInstance().jsonReaderFrom(request.body()).getString("id");
                }

                apiAuthenticationHelper.checkUserHasPermissions(currentUsername(), getAction(request), SupportedEntity.CLUSTER_PROFILE, resourceToOperateOn);
            });

            before(Routes.ClusterProfilesAPI.ID, mimeType, (request, response) -> {
                apiAuthenticationHelper.checkUserHasPermissions(currentUsername(), getAction(request), SupportedEntity.CLUSTER_PROFILE, request.params("cluster_id"));
            });

            get("", mimeType, this::index);
            post("", mimeType, this::create);

            get(Routes.ClusterProfilesAPI.ID, mimeType, this::getClusterProfile);
            delete(Routes.ClusterProfilesAPI.ID, mimeType, this::deleteClusterProfile);
            put(Routes.ClusterProfilesAPI.ID, mimeType, this::update);
        });
    }

    public String index(Request request, Response response) throws IOException {
        final PluginProfiles<ClusterProfile> userSpecificClusterProfiles = new ClusterProfiles();
        for (ClusterProfile clusterProfile : clusterProfilesService.getPluginProfiles()) {
            if (apiAuthenticationHelper.doesUserHasPermissions(currentUsername(), getAction(request), SupportedEntity.CLUSTER_PROFILE, clusterProfile.getId())) {
                userSpecificClusterProfiles.add(clusterProfile);
            }
        }

        Function<String, Boolean> canAdministerClusterProfile = repoId -> apiAuthenticationHelper.doesUserHasPermissions(currentUsername(), SupportedAction.ADMINISTER, CLUSTER_PROFILE, repoId);
        return writerForTopLevelObject(request, response, outputWriter -> ClusterProfilesRepresenter.toJSON(outputWriter, userSpecificClusterProfiles, canAdministerClusterProfile));
    }

    public String getClusterProfile(Request request, Response response) throws IOException {
        final ClusterProfile clusterProfile = fetchEntityFromConfig(request.params("cluster_id"));
        String etag = etagFor(clusterProfile);

        if (fresh(request, etag)) {
            return notModified(response);
        }

        setEtagHeader(response, etag);
        return writerForTopLevelObject(request, response, jsonWriter(clusterProfile));
    }

    public String create(Request request, Response response) throws IOException {
        ClusterProfile clusterProfile = buildEntityFromRequestBody(request);
        haltIfEntityWithSameIdExists(clusterProfile);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        clusterProfilesService.create(clusterProfile, currentUsername(), result);

        return handleCreateOrUpdateResponse(request, response, clusterProfile, result);
    }

    String deleteClusterProfile(Request req, Response res) {
        ClusterProfile clusterProfile = fetchEntityFromConfig(req.params("cluster_id"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        clusterProfilesService.delete(clusterProfile, currentUsername(), result);

        return handleSimpleMessageResponse(res, result);
    }

    public String update(Request request, Response response) throws IOException {
        final String profileId = request.params("cluster_id");
        final ClusterProfile existingClusterProfile = fetchEntityFromConfig(profileId);
        final ClusterProfile newClusterProfile = buildEntityFromRequestBody(request);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        if (isRenameAttempt(profileId, newClusterProfile.getId())) {
            throw haltBecauseRenameOfEntityIsNotSupported("Cluster Profile");
        }

        if (isPutRequestStale(request, existingClusterProfile)) {
            throw haltBecauseEtagDoesNotMatch("Cluster Profile", existingClusterProfile.getId());
        }

        clusterProfilesService.update(newClusterProfile, currentUsername(), result);
        return handleCreateOrUpdateResponse(request, response, newClusterProfile, result);
    }

    @Override
    public String etagFor(ClusterProfile entityFromServer) {
        return entityHashingService.md5ForEntity(entityFromServer);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ClusterProfile;
    }

    @Override
    public ClusterProfile doFetchEntityFromConfig(String id) {
        return clusterProfilesService.findProfile(id);
    }

    @Override
    public ClusterProfile buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return ClusterProfileRepresenter.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(ClusterProfile clusterProfile) {
        boolean canAdminister = apiAuthenticationHelper.doesUserHasPermissions(currentUsername(), SupportedAction.ADMINISTER, CLUSTER_PROFILE, clusterProfile.getId());
        return outputWriter -> ClusterProfileRepresenter.toJSON(outputWriter, clusterProfile, canAdminister);
    }

    private boolean isRenameAttempt(String profileIdFromRequestParam, String profileIdFromRequestBody) {
        return !StringUtils.equals(profileIdFromRequestBody, profileIdFromRequestParam);
    }

    private void haltIfEntityWithSameIdExists(ClusterProfile clusterProfile) {
        if (doFetchEntityFromConfig(clusterProfile.getId()) == null) {
            return;
        }

        clusterProfile.addError("id", format("Cluster Profile ids should be unique. Cluster Profile with id '%s' already exists.", clusterProfile.getId()));
        throw haltBecauseEntityAlreadyExists(jsonWriter(clusterProfile), "Cluster Profile", clusterProfile.getId());
    }
}
