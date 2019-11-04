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

package com.thoughtworks.go.apiv5.admin.templateconfig;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv5.admin.templateconfig.representers.TemplatesInternalRepresenter;
import com.thoughtworks.go.config.TemplatesConfig;
import com.thoughtworks.go.server.service.TemplateConfigService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;

@Component
public class TemplateConfigInternalControllerV5 extends ApiController
    implements SparkSpringController {

  private final TemplateConfigService templateConfigService;
  private final ApiAuthenticationHelper apiAuthenticationHelper;

  @Autowired
  public TemplateConfigInternalControllerV5(TemplateConfigService templateConfigService,
      ApiAuthenticationHelper apiAuthenticationHelper) {
    super(ApiVersion.v5);
    this.templateConfigService = templateConfigService;
    this.apiAuthenticationHelper = apiAuthenticationHelper;
  }

  @Override
  public String controllerBasePath() {
    return Routes.PipelineTemplateConfig.INTERNAL_BASE;
  }

  @Override
  public void setupRoutes() {
    path(controllerPath(), () -> {
      before("", mimeType, this::setContentType);
      before("/*", mimeType, this::setContentType);
      before("", mimeType, this::verifyContentType);
      before("/*", mimeType, this::verifyContentType);
      before("", mimeType, onlyOn(apiAuthenticationHelper::checkViewAccessToTemplateAnd403, "GET", "HEAD"));

      get("", mimeType, this::listTemplates);
    });
  }

  public String listTemplates(Request req, Response res) throws IOException {
    TemplatesConfig templateConfigs = templateConfigService.templateConfigsThatCanBeViewedBy(currentUsername());
    return writerForTopLevelObject(req, res, writer -> TemplatesInternalRepresenter.toJSON(writer, templateConfigs));
  }
}
