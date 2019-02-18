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

package com.thoughtworks.go.apiv1.admin.pipelinegroups;


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.admin.pipelinegroups.representers.PipelineGroupRepresenter;
import com.thoughtworks.go.apiv1.admin.pipelinegroups.representers.PipelineGroupsRepresenter;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.exceptions.HttpException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.PipelineConfigsService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static spark.Spark.*;

@Component
public class PipelineGroupsControllerV1 extends ApiController implements SparkSpringController, CrudController<PipelineConfigs> {
    private final PipelineConfigsService pipelineConfigsService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;
    private final SecurityService securityService;

    @Autowired
    public PipelineGroupsControllerV1(PipelineConfigsService pipelineConfigsService, ApiAuthenticationHelper apiAuthenticationHelper, EntityHashingService entityHashingService, SecurityService securityService) {
        super(ApiVersion.v1);
        this.pipelineConfigsService = pipelineConfigsService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
        this.securityService = securityService;
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);
            before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            before(Routes.PipelineGroupsAdmin.NAME_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupAdminUserAnd403);

            get("", mimeType, this::index);
            post("", mimeType, this::create);

            get(Routes.PipelineGroupsAdmin.NAME_PATH, mimeType, this::show);
            put(Routes.PipelineGroupsAdmin.NAME_PATH, mimeType, this::update);
            delete(Routes.PipelineGroupsAdmin.NAME_PATH, mimeType, this::destroy);

            exception(HttpException.class, this::httpException);
        });

    }

    @Override
    public String controllerBasePath() {
        return Routes.PipelineGroupsAdmin.BASE;
    }

    public String index(Request req, Response res) throws IOException {
        PipelineGroups pipelineGroups = new PipelineGroups(streamAllPipelineGroups().toArray(PipelineConfigs[]::new));
        String etag = entityHashingService.md5ForEntity(pipelineGroups);

        if (fresh(req, etag)) {
            return notModified(res);
        } else {
            setEtagHeader(res, etag);
            return writerForTopLevelObject(req, res, writer -> PipelineGroupsRepresenter.toJSON(writer, pipelineGroups));
        }
    }

    public String create(Request req, Response res) {
        PipelineConfigs pipelineConfigsFromReq = buildEntityFromRequestBody(req);
        String groupName = pipelineConfigsFromReq.getGroup();
        Optional<PipelineConfigs> pipelineConfigsFromServer = findPipelineGroup(groupName);

        if (pipelineConfigsFromServer.isPresent()) {
            pipelineConfigsFromReq.addError("name", LocalizedMessage.resourceAlreadyExists("pipeline group", groupName));
            throw haltBecauseEntityAlreadyExists(jsonWriter(pipelineConfigsFromReq), "pipeline group", groupName);
        }

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pipelineConfigsService.createGroup(SessionUtils.currentUsername(), pipelineConfigsFromReq, result);

        return handleCreateOrUpdateResponse(req, res, pipelineConfigsFromReq, result);
    }

    public String show(Request req, Response res) throws IOException {
        PipelineConfigs pipelineConfigs = fetchEntityFromConfig(req.params("group_name"));
        if (isGetOrHeadRequestFresh(req, pipelineConfigs)) {
            return notModified(res);
        } else {
            setEtagHeader(pipelineConfigs, res);
            return writerForTopLevelObject(req, res, writer -> PipelineGroupRepresenter.toJSON(writer, pipelineConfigs));
        }
    }

    public String update(Request req, Response res) {
        PipelineConfigs pipelineConfigsFromServer = fetchEntityFromConfig(req.params("group_name"));
        PipelineConfigs pipelineConfigsFromReq = buildEntityFromRequestBody(req);

        if (isRenameAttempt(pipelineConfigsFromServer, pipelineConfigsFromReq)) {
            throw haltBecauseRenameOfEntityIsNotSupported("pipeline group");
        }

        if (isPutRequestStale(req, pipelineConfigsFromServer)) {
            throw haltBecauseEtagDoesNotMatch("pipeline group", pipelineConfigsFromServer.getGroup());
        }

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PipelineConfigs updatedPipelineConfigs = pipelineConfigsService.updateGroupAuthorization(SessionUtils.currentUsername(), pipelineConfigsFromReq, etagFor(pipelineConfigsFromServer), entityHashingService, securityService, result);
        return handleCreateOrUpdateResponse(req, res, updatedPipelineConfigs, result);
    }

    public String destroy(Request req, Response res) throws IOException {
        PipelineConfigs pipelineConfigs = fetchEntityFromConfig(req.params("group_name"));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pipelineConfigsService.deleteGroup(SessionUtils.currentUsername(), pipelineConfigs, result);

        return renderHTTPOperationResult(result, req, res);
    }

    @Override
    public String etagFor(PipelineConfigs pipelineConfigs) {
        return entityHashingService.md5ForEntity(pipelineConfigs);
    }

    @Override
    public PipelineConfigs doFetchEntityFromConfig(String name) {
        return findPipelineGroup(name).orElseThrow(() -> new RecordNotFoundException("Pipeline group with name " + name + " was not found!"));
    }

    private Optional<PipelineConfigs> findPipelineGroup(String name) {
        Stream<PipelineConfigs> pipelineGroupStream = streamAllPipelineGroups().filter(p -> p.getGroup().equals(name));
        return pipelineGroupStream.findFirst();
    }

    @Override
    public PipelineConfigs buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return PipelineGroupRepresenter.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(PipelineConfigs pipelineConfigs) {
        return writer -> PipelineGroupRepresenter.toJSON(writer, pipelineConfigs);
    }

    private Stream<PipelineConfigs> streamAllPipelineGroups() {
        return pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString()).stream();
    }

    private boolean isRenameAttempt(PipelineConfigs fromServer, PipelineConfigs fromRequest) {
        return !fromServer.getGroup().equals(fromRequest.getGroup());
    }
}
