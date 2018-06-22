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

package com.thoughtworks.go.apiv4.admin.templateconfig;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv4.shared.representers.stages.ConfigHelperOptions;
import com.thoughtworks.go.apiv4.admin.templateconfig.representers.TemplateConfigRepresenter;
import com.thoughtworks.go.apiv4.admin.templateconfig.representers.TemplatesConfigRepresenter;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.config.TemplateToPipelines;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.TemplateConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import spark.Filter;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;

import static com.thoughtworks.go.api.util.HaltApiResponses.*;
import static spark.Spark.*;

public class TemplateConfigControllerV4Delegate extends ApiController implements CrudController<PipelineTemplateConfig> {

  private final TemplateConfigService templateConfigService;
  private final ApiAuthenticationHelper apiAuthenticationHelper;
  private final EntityHashingService entityHashingService;

  public TemplateConfigControllerV4Delegate(TemplateConfigService templateConfigService, ApiAuthenticationHelper apiAuthenticationHelper, EntityHashingService entityHashingService) {
    super(ApiVersion.v4);
    this.templateConfigService = templateConfigService;
    this.apiAuthenticationHelper = apiAuthenticationHelper;
    this.entityHashingService = entityHashingService;
  }

  @Override
  public String etagFor(PipelineTemplateConfig pipelineTemplateConfig) {
    return entityHashingService.md5ForEntity(pipelineTemplateConfig);
  }

  @Override
  public PipelineTemplateConfig doGetEntityFromConfig(String name) {
    return templateConfigService.loadForView(name, new HttpLocalizedOperationResult());
  }

  @Override
  public PipelineTemplateConfig getEntityFromRequestBody(Request req) {
    JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
      return TemplateConfigRepresenter.fromJSON(jsonReader);
  }

  @Override
  public String jsonize(Request req, PipelineTemplateConfig templateConfig) {
    return jsonizeAsTopLevelObject(req, writer -> TemplateConfigRepresenter.toJSON(writer, templateConfig));
  }

  @Override
  public JsonNode jsonNode(Request req, PipelineTemplateConfig pipelineTemplateConfig) throws IOException {
    String jsonize = jsonize(req, pipelineTemplateConfig);
    return new ObjectMapper().readTree(jsonize);
  }

  @Override
  public String controllerBasePath() {
    return Routes.PipelineTemplateConfig.BASE;
  }

  @Override
  public void setupRoutes() {
    path(controllerPath(), () -> {
      before("", mimeType, this::setContentType);
      before("/*", mimeType, this::setContentType);
      before("", this::verifyContentType);
      before("/*", this::verifyContentType);
      before("", mimeType, onlyOn(apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403, "POST"));
      before("", mimeType, onlyOn(apiAuthenticationHelper::checkViewAccessToTemplateAnd403, "GET", "HEAD"));
      before(Routes.PipelineTemplateConfig.NAME, mimeType, onlyOn(apiAuthenticationHelper::checkViewAccessToTemplateAnd403, "GET", "HEAD"));
      before(Routes.PipelineTemplateConfig.NAME, mimeType, onlyOn(apiAuthenticationHelper::checkAdminOrTemplateAdminAnd403, "PUT", "PATCH", "DELETE"));

      get("", mimeType, this::index);
      post("", mimeType, this::create);

      get(Routes.PipelineTemplateConfig.NAME, mimeType, this::show);
      put(Routes.PipelineTemplateConfig.NAME, mimeType, this::update);
      delete(Routes.PipelineTemplateConfig.NAME, mimeType, this::destroy);

      exception(RecordNotFoundException.class, this::notFound);
    });
  }

  public static Filter onlyOn(Filter filter, String... allowedMethods) {
    return (request, response) -> {
      if (Sets.newHashSet(allowedMethods).contains(request.requestMethod())) {
        filter.handle(request, response);
      }
    };
  }

  public String index(Request req, Response res) throws IOException {
    List<TemplateToPipelines> templatesList = templateConfigService.getTemplatesList(currentUsername());

    return writerForTopLevelObject(req, res, writer -> TemplatesConfigRepresenter.toJSON(writer, templatesList));
  }

  public String destroy(Request req, Response res) throws IOException {
    PipelineTemplateConfig existingTemplateConfig = getEntityFromConfig(req.params("template_name"));

    HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
    templateConfigService.deleteTemplateConfig(SessionUtils.currentUsername(), existingTemplateConfig, result);


    return renderHTTPOperationResult(result, req, res);
  }

  public String update(Request req, Response res) throws IOException {
    PipelineTemplateConfig existingTemplateConfig =  getEntityFromConfig(req.params("template_name"));
    PipelineTemplateConfig templateConfigFromRequest = getEntityFromRequestBody(req);

    if (isRenameAttempt(existingTemplateConfig, templateConfigFromRequest)) {
      throw haltBecauseRenameOfEntityIsNotSupported("templates");
    }

    if (!isPutRequestFresh(req, existingTemplateConfig)) {
      throw haltBecauseEtagDoesNotMatch("template", existingTemplateConfig.name());
    }

    HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
    templateConfigService.updateTemplateConfig(SessionUtils.currentUsername(), templateConfigFromRequest, result, etagFor(existingTemplateConfig));
    return handleCreateOrUpdateResponse(req, res, templateConfigFromRequest, result);
  }

  public String create(Request req, Response res) throws IOException {
    PipelineTemplateConfig templateConfigFromRequest = getEntityFromRequestBody(req);

    haltIfEntityBySameNameInRequestExists(req, templateConfigFromRequest, new HttpLocalizedOperationResult());

    HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

    Username userName = SessionUtils.currentUsername();
    templateConfigService.createTemplateConfig(userName, templateConfigFromRequest, result);

    return handleCreateOrUpdateResponse(req, res, templateConfigFromRequest, result);
  }

  public String show(Request req, Response res) throws IOException {
    PipelineTemplateConfig templateConfig = getEntityFromConfig(req.params("template_name"));
    if (isGetOrHeadRequestFresh(req, templateConfig)) {
      return notModified(res);
    } else {
      setEtagHeader(templateConfig, res);
      return writerForTopLevelObject(req, res, writer -> TemplateConfigRepresenter.toJSON(writer, templateConfig));
    }
  }

  private boolean isRenameAttempt(PipelineTemplateConfig existingTemplateConfig, PipelineTemplateConfig templateConfigFromRequest) {
    return !existingTemplateConfig.name().equals(templateConfigFromRequest.name());
  }


  private void haltIfEntityBySameNameInRequestExists(Request req, PipelineTemplateConfig templateConfig, HttpLocalizedOperationResult result) throws IOException {
    if (templateConfigService.loadForView(templateConfig.name().toString(), result) == null) {
      return;
    }
    templateConfig.addError("name", LocalizedMessage.resourceAlreadyExists("template", templateConfig.name().toString()));
    throw haltBecauseEntityAlreadyExists(jsonNode(req, templateConfig), "template", templateConfig.name());
  }
}
