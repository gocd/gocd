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

package com.thoughtworks.go.apiv1.admin.security;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.admin.security.representers.RoleRepresenter;
import com.thoughtworks.go.apiv1.admin.security.representers.RolesRepresenter;
import com.thoughtworks.go.config.InvalidPluginTypeException;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RolesConfig;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.RoleConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;
import org.springframework.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.util.Collections;
import java.util.Map;

import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static spark.Spark.*;

public class RolesControllerV1Delegate extends ApiController implements CrudController<Role> {
    private final RoleConfigService roleConfigService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;
    private final Localizer localizer;

    public RolesControllerV1Delegate(RoleConfigService roleConfigService, ApiAuthenticationHelper apiAuthenticationHelper,
                                     EntityHashingService entityHashingService, Localizer localizer) {
        super(ApiVersion.v1);
        this.roleConfigService = roleConfigService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
        this.localizer = localizer;
    }

    @Override
    public void setupRoutes() {
        path(controllerPath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", this::verifyContentType);
            before("/*", this::verifyContentType);
            before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd401);
            before("/*", mimeType, apiAuthenticationHelper::checkAdminUserAnd401);

            get("", mimeType, this::index, GsonTransformer.getInstance());
            post("", mimeType, this::create, GsonTransformer.getInstance());

            get(Routes.Roles.NAME_PATH, mimeType, this::show, GsonTransformer.getInstance());
            put(Routes.Roles.NAME_PATH, mimeType, this::update, GsonTransformer.getInstance());
            delete(Routes.Roles.NAME_PATH, mimeType, this::destroy, GsonTransformer.getInstance());

            exception(InvalidPluginTypeException.class, (ex, req, res) -> {
                res.body(this.messageJson(ex));
                res.status(HttpStatus.BAD_REQUEST.value());
            });

            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    @Override
    public String controllerBasePath() {
        return Routes.Roles.BASE;
    }

    public Object index(Request req, Response res) throws InvalidPluginTypeException {
        String pluginType = req.queryParams("type");
        RolesConfig roles = roleConfigService.getRoles().ofType(pluginType);
        String etag = entityHashingService.md5ForEntity(roles);

        if (fresh(req, etag)) {
            return notModified(res);
        } else {
            setEtagHeader(res, etag);
            return RolesRepresenter.toJSON(roles, RequestContext.requestContext(req));
        }
    }

    public Object show(Request req, Response res) {
        Role role = getEntityFromConfig(req.params("role_name"));

        if (isGetOrHeadRequestFresh(req, role)) {
            return notModified(res);
        } else {
            setEtagHeader(role, res);
            return RoleRepresenter.toJSON(role, RequestContext.requestContext(req));
        }
    }

    public Map create(Request req, Response res) {
        Role role = getEntityFromRequestBody(req);

        haltIfEntityBySameNameInRequestExists(req, role);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        roleConfigService.create(UserHelper.getUserName(), role, result);

        return handleCreateOrUpdateResponse(req, res, role, result);
    }

    public Map update(Request req, Response res) {
        Role roleFromServer = roleConfigService.findRole(req.params(":role_name"));
        Role roleFromRequest = getEntityFromRequestBody(req);

        if (isRenameAttempt(roleFromServer, roleFromRequest)) {
            throw haltBecauseRenameOfEntityIsNotSupported("roles");
        }

        if (!isPutRequestFresh(req, roleFromServer)) {
            throw haltBecauseEtagDoesNotMatch("role", roleFromServer.getName());
        }

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        roleConfigService.update(UserHelper.getUserName(), etagFor(roleFromServer), roleFromRequest, result);
        return handleCreateOrUpdateResponse(req, res, roleFromRequest, result);
    }

    public Map destroy(Request req, Response res) {
        Role role = getEntityFromConfig(req.params("role_name"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        roleConfigService.delete(UserHelper.getUserName(), role, result);

        res.status(result.httpCode());
        return Collections.singletonMap("message", result.message(localizer));
    }

    @Override
    public String etagFor(Role entityFromServer) {
        return entityHashingService.md5ForEntity(entityFromServer);
    }

    public boolean isRenameAttempt(Role fromServer, Role fromRequest) {
        return !fromServer.getName().equals(fromRequest.getName());
    }

    @Override
    public Localizer getLocalizer() {
        return localizer;
    }

    @Override
    public Role doGetEntityFromConfig(String name) {
        return roleConfigService.findRole(name);
    }

    @Override
    public Role getEntityFromRequestBody(Request req) {
        GsonTransformer gsonTransformer = GsonTransformer.getInstance();
        JsonReader jsonReader = gsonTransformer.jsonReaderFrom(req.body());
        return RoleRepresenter.fromJSON(jsonReader);
    }

    @Override
    public Map jsonize(Request req, Role role) {
        return RoleRepresenter.toJSON(role, RequestContext.requestContext(req));
    }

    private void haltIfEntityBySameNameInRequestExists(Request req, Role role) {
        if (roleConfigService.findRole(role.getName().toString()) == null) {
            return;
        }
        role.addError("name", "Role names should be unique. Role with the same name exists.");
        throw haltBecauseEntityAlreadyExists(jsonize(req, role), "role", role.getName());
    }
}
