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

package com.thoughtworks.go.apiv1.adminsconfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.go.api.CrudController;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.adminsconfig.representers.AdminsRepresenter;
import com.thoughtworks.go.config.AdminsConfig;
import com.thoughtworks.go.config.InvalidPluginTypeException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.AdminsConfigService;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import org.springframework.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;
import static spark.Spark.exception;

public class AdminControllerV1Delegate extends ApiController implements CrudController<AdminsConfig>{
  private final ApiAuthenticationHelper apiAuthenticationHelper;
  private final EntityHashingService entityHashingService;
  private final AdminsConfigService adminsConfigService;


  public AdminControllerV1Delegate(ApiAuthenticationHelper apiAuthenticationHelper,
                                   EntityHashingService entityHashingService,AdminsConfigService service) {
    super(ApiVersion.v1);
    this.apiAuthenticationHelper = apiAuthenticationHelper;
    this.entityHashingService = entityHashingService;
    this.adminsConfigService = service;
  }

  @Override
  public void setupRoutes() {
    path(controllerPath(), () -> {
      before("", mimeType, this::setContentType);
      before("/*", mimeType, this::setContentType);
      before("", this::verifyContentType);
      before("/*", this::verifyContentType);
      before("", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
      before("/*", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);

      get("", mimeType, this::show);
      patch("", mimeType, this::replaceAndUpdateAdmins);
      put("", mimeType, this::replaceAndUpdateAdmins);

      exception(InvalidPluginTypeException.class, (ex, req, res) -> {
        res.body(this.messageJson(ex));
        res.status(HttpStatus.BAD_REQUEST.value());
      });

      exception(RecordNotFoundException.class, this::notFound);
    });
  }

  public String show(Request req, Response res) throws IOException {
      AdminsConfig adminConf = getEntityFromConfig(req.params("admins"));
    if (isGetOrHeadRequestFresh(req, adminConf)) {
      return notModified(res);
    } else {
      setEtagHeader(adminConf, res);
      return writerForTopLevelObject(req, res, writer -> AdminsRepresenter.toJSON(writer, adminConf));
    }
  }

  public String replaceAndUpdateAdmins(Request req, Response res) throws IOException {
    AdminsConfig reqAdminsConfig = getEntityFromRequestBody(req);
    HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
    adminsConfigService.replace(SessionUtils.currentUsername(), reqAdminsConfig, result);
    AdminsConfig adminConf = getEntityFromConfig(req.params("admins"));
    return handleCreateOrUpdateResponse(req, res, adminConf, result);
  }

  @Override
  public String etagFor(AdminsConfig entityFromServer) {
    return entityHashingService.md5ForEntity(entityFromServer);
  }

  @Override
  public AdminsConfig doGetEntityFromConfig(String name) {
    return adminsConfigService.findAdmins();
  }

  @Override
  public AdminsConfig getEntityFromRequestBody(Request req) {
    JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
    String protocol = req.requestMethod();
    return AdminsRepresenter.fromJSON(jsonReader,protocol,getEntityFromConfig(req.params("admins")));
  }

  @Override
  public String jsonize(Request req, AdminsConfig admins) {
    return jsonizeAsTopLevelObject(req, writer -> AdminsRepresenter.toJSON(writer, admins));
  }

  @Override
  public JsonNode jsonNode(Request req, AdminsConfig admins) throws IOException {
    String jsonize = jsonize(req, admins);
    return new ObjectMapper().readTree(jsonize);
  }

  @Override
  public String controllerBasePath() {
    return Routes.Admins.BASE;
  }

}
