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

package com.thoughtworks.go.apiv1.elasticprofile;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.elasticprofile.representers.ElasticProfileRepresenter;
import com.thoughtworks.go.apiv1.elasticprofile.representers.ElasticProfilesRepresenter;
import com.thoughtworks.go.config.PluginProfiles;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
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

import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static java.lang.String.format;
import static spark.Spark.*;

@Component
public class ElasticProfileControllerV1 extends ApiController implements SparkSpringController, CrudController<ElasticProfile> {
    private static final String PROFILE_ID_PARAM = "profile_id";
    private final ElasticProfileService elasticProfileService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;

    @Autowired
    public ElasticProfileControllerV1(ElasticProfileService elasticProfileService, ApiAuthenticationHelper apiAuthenticationHelper, EntityHashingService entityHashingService) {
        super(ApiVersion.v1);
        this.elasticProfileService = elasticProfileService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
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
            before("", mimeType, apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403);
            before("/*", mimeType, apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403);

            get("", mimeType, this::index);
            get(Routes.ElasticProfileAPI.ID, mimeType, this::show);

            post("", mimeType, this::create);
            put(Routes.ElasticProfileAPI.ID, mimeType, this::update);
            delete(Routes.ElasticProfileAPI.ID, mimeType, this::destroy);

            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    public String index(Request request, Response response) throws IOException {
        final PluginProfiles<ElasticProfile> elasticProfiles = elasticProfileService.getPluginProfiles();
        return writerForTopLevelObject(request, response,
                outputWriter -> ElasticProfilesRepresenter.toJSON(outputWriter, elasticProfiles));
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
        return entityHashingService.md5ForEntity(entityFromServer);
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
    public Consumer<OutputWriter> jsonWriter(ElasticProfile elasticProfile) {
        return writer -> ElasticProfileRepresenter.toJSON(writer, elasticProfile);
    }

    private void haltIfEntityWithSameIdExists(ElasticProfile elasticProfile) {
        if (elasticProfileService.findProfile(elasticProfile.getId()) == null) {
            return;
        }
        elasticProfile.addError("id", format("Elastic profile ids should be unique. Elastic profile with id '%s' already exists.", elasticProfile.getId()));
        throw haltBecauseEntityAlreadyExists(jsonWriter(elasticProfile), "elasticProfile", elasticProfile.getId());
    }
}
