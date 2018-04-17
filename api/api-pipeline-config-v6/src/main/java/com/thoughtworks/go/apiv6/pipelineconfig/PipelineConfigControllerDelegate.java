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

package com.thoughtworks.go.apiv6.pipelineconfig;


import com.fasterxml.jackson.databind.JsonNode;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.i18n.Localizer;
import spark.Request;

import java.io.IOException;

public class PipelineConfigControllerDelegate extends ApiController implements CrudController<PipelineConfig> {
  public PipelineConfigControllerDelegate() {
    super(ApiVersion.v6);
  }

  @Override
  public String etagFor(PipelineConfig entityFromServer) {
    return null;
  }

  @Override
  public Localizer getLocalizer() {
    return null;
  }

  @Override
  public PipelineConfig doGetEntityFromConfig(String name) {
    return null;
  }

  @Override
  public PipelineConfig getEntityFromRequestBody(Request req) {
    return null;
  }

  @Override
  public String jsonize(Request req, PipelineConfig stageConfigs) {
    return null;
  }

  @Override
  public JsonNode jsonNode(Request req, PipelineConfig stageConfigs) throws IOException {
    return null;
  }

  @Override
  public String controllerBasePath() {
    return null;
  }

  @Override
  public void setupRoutes() {

  }
}
