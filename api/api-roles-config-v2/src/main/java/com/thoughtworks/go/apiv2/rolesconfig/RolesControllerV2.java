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
package com.thoughtworks.go.apiv2.rolesconfig;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv2.rolesconfig.representers.GoCDRolesBulkUpdateRequestRepresenter;
import com.thoughtworks.go.apiv2.rolesconfig.representers.RoleRepresenter;
import com.thoughtworks.go.apiv2.rolesconfig.representers.RolesRepresenter;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RolesConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.update.GoCDRolesBulkUpdateRequest;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.RoleConfigService;
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
public class RolesControllerV2 extends ApiController implements SparkSpringController, CrudController<Role> {
    private final RoleConfigService roleConfigService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;

    @Autowired
    public RolesControllerV2(RoleConfigService roleConfigService, ApiAuthenticationHelper apiAuthenticationHelper,
                             EntityHashingService entityHashingService) {
        super(ApiVersion.v2);
        this.roleConfigService = roleConfigService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);
            before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);

            get("", mimeType, this::index);
            post("", mimeType, this::create);
            patch("", mimeType, this::bulkUpdate);

            get(Routes.Roles.NAME_PATH, mimeType, this::show);
            put(Routes.Roles.NAME_PATH, mimeType, this::update);
            delete(Routes.Roles.NAME_PATH, mimeType, this::destroy);
        });
    }

    @Override
    public String controllerBasePath() {
        return Routes.Roles.BASE;
    }

    public String index(Request req, Response res) throws IOException {
        String pluginType = req.queryParams("type");
        RolesConfig roles = roleConfigService.getRoles().ofType(pluginType);
        String etag = entityHashingService.md5ForEntity(roles);

        if (fresh(req, etag)) {
            return notModified(res);
        } else {
            setEtagHeader(res, etag);
            return writerForTopLevelObject(req, res, writer -> RolesRepresenter.toJSON(writer, roles));
        }
    }

    public String show(Request req, Response res) throws IOException {
        Role role = fetchEntityFromConfig(req.params("role_name"));

        if (isGetOrHeadRequestFresh(req, role)) {
            return notModified(res);
        } else {
            setEtagHeader(role, res);
            return writerForTopLevelObject(req, res, writer -> RoleRepresenter.toJSON(writer, role));
        }
    }

    public String create(Request req, Response res) {
        Role role = buildEntityFromRequestBody(req);

        haltIfEntityBySameNameInRequestExists(req, role);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        roleConfigService.create(SessionUtils.currentUsername(), role, result);

        return handleCreateOrUpdateResponse(req, res, role, result);
    }

    public String update(Request req, Response res) {
        Role roleFromServer = roleConfigService.findRole(req.params(":role_name"));
        Role roleFromRequest = buildEntityFromRequestBody(req);

        if (isRenameAttempt(roleFromServer, roleFromRequest)) {
            throw haltBecauseRenameOfEntityIsNotSupported("roles");
        }

        if (isPutRequestStale(req, roleFromServer)) {
            throw haltBecauseEtagDoesNotMatch("role", roleFromServer.getName());
        }

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        roleConfigService.update(SessionUtils.currentUsername(), etagFor(roleFromServer), roleFromRequest, result);
        return handleCreateOrUpdateResponse(req, res, roleFromRequest, result);
    }

    public String destroy(Request req, Response res) throws IOException {
        Role role = fetchEntityFromConfig(req.params("role_name"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        roleConfigService.delete(SessionUtils.currentUsername(), role, result);


        return renderHTTPOperationResult(result, req, res);
    }

    public String bulkUpdate(Request req, Response res) throws IOException {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        GoCDRolesBulkUpdateRequest bulkUpdateRequest = GoCDRolesBulkUpdateRequestRepresenter.fromJSON(jsonReader);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        roleConfigService.bulkUpdate(bulkUpdateRequest, SessionUtils.currentUsername(), result);
        if (result.isSuccessful()) {
            RolesConfig goCDRoles = roleConfigService.getRoles().ofType("gocd");
            return writerForTopLevelObject(req, res, writer -> RolesRepresenter.toJSON(writer, goCDRoles));
        } else {
            return renderHTTPOperationResult(result, req, res);
        }
    }


    @Override
    public String etagFor(Role entityFromServer) {
        return entityHashingService.md5ForEntity(entityFromServer);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.Role;
    }

    public boolean isRenameAttempt(Role fromServer, Role fromRequest) {
        return !fromServer.getName().equals(fromRequest.getName());
    }

    @Override
    public Role doFetchEntityFromConfig(String name) {
        return roleConfigService.findRole(name);
    }

    @Override
    public Role buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return RoleRepresenter.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(Role role) {
        return writer -> RoleRepresenter.toJSON(writer, role);
    }

    private void haltIfEntityBySameNameInRequestExists(Request req, Role role) {
        if (roleConfigService.findRole(role.getName().toString()) == null) {
            return;
        }
        role.addError("name", "Role names should be unique. Role with the same name exists.");
        throw haltBecauseEntityAlreadyExists(jsonWriter(role), "role", role.getName());
    }
}
