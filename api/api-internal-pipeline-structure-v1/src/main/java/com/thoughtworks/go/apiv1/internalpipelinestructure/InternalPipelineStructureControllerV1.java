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

package com.thoughtworks.go.apiv1.internalpipelinestructure;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.internalpipelinestructure.representers.InternalPipelineStructuresRepresenter;
import com.thoughtworks.go.config.TemplatesConfig;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.server.service.PipelineConfigService;
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
public class InternalPipelineStructureControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final PipelineConfigService pipelineConfigService;
    private final TemplateConfigService templateConfigService;

    @Autowired
    public InternalPipelineStructureControllerV1(ApiAuthenticationHelper apiAuthenticationHelper,
                                                 PipelineConfigService pipelineConfigService,
                                                 TemplateConfigService templateConfigService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.pipelineConfigService = pipelineConfigService;
        this.templateConfigService = templateConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.InternalPipelineStructure.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            // uncomment the line below to set the content type on the base path
            before("", mimeType, this::setContentType);
            // uncomment the line below to set the content type on nested routes
            before("/*", mimeType, this::setContentType);

            // uncomment for the `index` action
            get("", mimeType, this::index);

            // change the line below to enable appropriate security
            before("", mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            // to be implemented
        });
    }

    public String index(Request request, Response response) throws IOException {
        PipelineGroups groups = pipelineConfigService.viewableGroupsForUserIncludingConfigRepos(currentUsername());

        TemplatesConfig templateConfigs = templateConfigService.templateConfigsThatCanBeViewedBy(currentUsername());

        return writerForTopLevelObject(request, response, outputWriter -> InternalPipelineStructuresRepresenter.toJSON(outputWriter, groups, templateConfigs));
    }

}
