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
package com.thoughtworks.go.apiv1.admin.pipelinegroups;


import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv1.admin.pipelinegroups.representers.PipelineGroupRepresenter;
import com.thoughtworks.go.apiv1.admin.pipelinegroups.representers.PipelineGroupsRepresenter;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.PipelineConfigsService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEntityAlreadyExists;
import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseEtagDoesNotMatch;
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
            before("", mimeType, onlyOn(apiAuthenticationHelper::checkAdminUserAnd403, "POST"));
            before("", mimeType, onlyOn(apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403, "GET", "HEAD"));
            before(Routes.PipelineGroupsAdmin.NAME_PATH, mimeType, apiAuthenticationHelper::checkPipelineGroupAdminOfPipelineOrGroupInURLUserAnd403);

            get("", mimeType, this::index);
            post("", mimeType, this::create);

            get(Routes.PipelineGroupsAdmin.NAME_PATH, mimeType, this::show);
            put(Routes.PipelineGroupsAdmin.NAME_PATH, mimeType, this::update);
            delete(Routes.PipelineGroupsAdmin.NAME_PATH, mimeType, this::destroy);
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
            pipelineConfigsFromReq.addError("name", EntityType.PipelineGroup.alreadyExists(groupName));
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

        if (isPutRequestStale(req, pipelineConfigsFromServer)) {
            throw haltBecauseEtagDoesNotMatch("pipeline group", pipelineConfigsFromServer.getGroup());
        }

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PipelineConfigs updatedPipelineConfigs = pipelineConfigsService.updateGroup(SessionUtils.currentUsername(), pipelineConfigsFromReq, pipelineConfigsFromServer, result);
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
    public EntityType getEntityType() {
        return EntityType.PipelineGroup;
    }

    @Override
    public PipelineConfigs doFetchEntityFromConfig(String name) {
        return findPipelineGroup(name).orElseThrow(() -> new RecordNotFoundException(EntityType.PipelineGroup, name));
    }

    private Optional<PipelineConfigs> findPipelineGroup(String name) {
        return streamAllPipelineGroups().filter(p -> p.getGroup().equalsIgnoreCase(name)).findFirst();
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
}
