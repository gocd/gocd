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
package com.thoughtworks.go.apiv2.elasticprofile;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv2.elasticprofile.representers.ElasticProfileRepresenter;
import com.thoughtworks.go.apiv2.elasticprofile.representers.ElasticProfilesRepresenter;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.elastic.ElasticProfiles;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.policy.SupportedEntity;
import com.thoughtworks.go.server.service.ClusterProfilesService;
import com.thoughtworks.go.server.service.ElasticProfileService;
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
import java.util.stream.Collectors;

import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static java.lang.String.format;
import static spark.Spark.*;

@Component
public class ElasticProfileControllerV2 extends ApiController implements SparkSpringController, CrudController<ElasticProfile> {
    private static final String PROFILE_ID_PARAM = "profile_id";
    private final ElasticProfileService elasticProfileService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;
    private ClusterProfilesService clusterProfilesService;

    @Autowired
    public ElasticProfileControllerV2(ElasticProfileService elasticProfileService, ApiAuthenticationHelper apiAuthenticationHelper, EntityHashingService entityHashingService, ClusterProfilesService clusterProfilesService) {
        super(ApiVersion.v2);
        this.elasticProfileService = elasticProfileService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
        this.clusterProfilesService = clusterProfilesService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.ElasticProfileAPI.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, (request, response) -> {
                String resourceToOperateOn = "*";
                String resourceToOperateWithin = "*";
                if (request.requestMethod().equalsIgnoreCase("GET")) {
                    apiAuthenticationHelper.checkUserAnd403(request, response);
                    return;
                }

                if (request.requestMethod().equalsIgnoreCase("POST")) {
                    resourceToOperateOn = GsonTransformer.getInstance().jsonReaderFrom(request.body()).getString("id");
                    resourceToOperateWithin = GsonTransformer.getInstance().jsonReaderFrom(request.body()).getString("cluster_profile_id");
                }

                apiAuthenticationHelper.checkUserHasPermissions(currentUsername(), getAction(request), SupportedEntity.ELASTIC_AGENT_PROFILE, resourceToOperateOn, resourceToOperateWithin);
            });

            before(Routes.ElasticProfileAPI.ID, mimeType, (request, response) -> {
                ElasticProfile elasticProfile = fetchEntityFromConfig(request.params(PROFILE_ID_PARAM));
                apiAuthenticationHelper.checkUserHasPermissions(currentUsername(), getAction(request), SupportedEntity.ELASTIC_AGENT_PROFILE, elasticProfile.getId(), elasticProfile.getClusterProfileId());
            });

            get("", mimeType, this::index);
            get(Routes.ElasticProfileAPI.ID, mimeType, this::show);

            post("", mimeType, this::create);
            put(Routes.ElasticProfileAPI.ID, mimeType, this::update);
            delete(Routes.ElasticProfileAPI.ID, mimeType, this::destroy);
        });
    }

    public String index(Request request, Response response) throws IOException {
        final ElasticProfiles userSpecificElasticProfiles = elasticProfileService.getPluginProfiles().stream()
                .filter(elasticProfile -> apiAuthenticationHelper.doesUserHasPermissions(currentUsername(), getAction(request), SupportedEntity.ELASTIC_AGENT_PROFILE, elasticProfile.getId(), elasticProfile.getClusterProfileId()))
                .collect(Collectors.toCollection(ElasticProfiles::new));

        return writerForTopLevelObject(request, response, outputWriter -> ElasticProfilesRepresenter.toJSON(outputWriter, userSpecificElasticProfiles));
    }

    public String show(Request request, Response response) throws IOException {
        final ElasticProfile elasticProfile = fetchEntityFromConfig(request.params(PROFILE_ID_PARAM));

        if (isGetOrHeadRequestFresh(request, elasticProfile)) {
            return notModified(response);
        }

        setEtagHeader(elasticProfile, response);
        return writerForTopLevelObject(request, response, writer -> ElasticProfileRepresenter.toJSON(writer, elasticProfile));
    }

    public String create(Request request, Response response) {
        final ElasticProfile elasticProfileToCreate = buildEntityFromRequestBody(request);
        haltIfEntityWithSameIdExists(elasticProfileToCreate);
        ClusterProfile associatedClusterProfile = clusterProfilesService.findProfile(elasticProfileToCreate.getClusterProfileId());
        haltIfSpecifiedClusterProfileDoesntExists(associatedClusterProfile, elasticProfileToCreate);

        final HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        elasticProfileService.create(currentUsername(), elasticProfileToCreate, operationResult);

        return handleCreateOrUpdateResponse(request, response, elasticProfileToCreate, operationResult);
    }

    public String update(Request request, Response response) {
        final String profileId = request.params(PROFILE_ID_PARAM);
        final ElasticProfile existingElasticProfile = fetchEntityFromConfig(profileId);
        final ElasticProfile newElasticProfile = buildEntityFromRequestBody(request);

        if (isRenameAttempt(profileId, newElasticProfile.getId())) {
            throw haltBecauseRenameOfEntityIsNotSupported("elasticProfile");
        }

        if (isPutRequestStale(request, existingElasticProfile)) {
            throw haltBecauseEtagDoesNotMatch("elasticProfile", existingElasticProfile.getId());
        }

        ClusterProfile associatedClusterProfile = clusterProfilesService.findProfile(newElasticProfile.getClusterProfileId());
        haltIfSpecifiedClusterProfileDoesntExists(associatedClusterProfile, newElasticProfile);

        final HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        elasticProfileService.update(currentUsername(), etagFor(existingElasticProfile), newElasticProfile, operationResult);

        return handleCreateOrUpdateResponse(request, response, newElasticProfile, operationResult);
    }

    public String destroy(Request request, Response response) throws IOException {
        ElasticProfile elasticProfile = fetchEntityFromConfig(request.params(PROFILE_ID_PARAM));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        elasticProfileService.delete(currentUsername(), elasticProfile, result);

        return renderHTTPOperationResult(result, request, response);
    }


    private boolean isRenameAttempt(String profileIdFromRequestParam, String profileIdFromRequestBody) {
        return !StringUtils.equals(profileIdFromRequestBody, profileIdFromRequestParam);
    }

    @Override
    public String etagFor(ElasticProfile entityFromServer) {
        return entityHashingService.hashForEntity(entityFromServer);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ElasticProfile;
    }

    @Override
    public ElasticProfile doFetchEntityFromConfig(String profileID) {
        return elasticProfileService.findProfile(profileID);
    }

    @Override
    public ElasticProfile buildEntityFromRequestBody(Request req) {
        return ElasticProfileRepresenter.fromJSON(GsonTransformer.getInstance().jsonReaderFrom(req.body()));
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(ElasticProfile elasticAgentProfileProfile) {
        return writer -> ElasticProfileRepresenter.toJSON(writer, elasticAgentProfileProfile);
    }

    //this is done in command as well, keeping it here for early return instead of failing later during config update command.
    private void haltIfEntityWithSameIdExists(ElasticProfile elasticProfile) {
        if (elasticProfileService.findProfile(elasticProfile.getId()) == null) {
            return;
        }

        elasticProfile.addError("id", format("Elastic profile ids should be unique. Elastic profile with id '%s' already exists.", elasticProfile.getId()));
        throw haltBecauseEntityAlreadyExists(jsonWriter(elasticProfile), "elasticProfile", elasticProfile.getId());
    }

    private void haltIfSpecifiedClusterProfileDoesntExists(ClusterProfile clusterProfile, ElasticProfile elasticProfile) {
        if (clusterProfile != null) {
            return;
        }

        String errorMsg = format("No Cluster Profile exists with the specified cluster_profile_id '%s'.", elasticProfile.getClusterProfileId());
        elasticProfile.addError("cluster_profile_id", errorMsg);

        throw haltBecauseOfReason(errorMsg);
    }
}
