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
package com.thoughtworks.go.apiv1.internalpipelinestructure;

import com.google.common.collect.ImmutableMap;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.HaltApiResponses;
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
import java.util.Map;
import java.util.function.Supplier;

import static spark.Spark.*;

@Component
public class InternalPipelineStructureControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final Map<String, Supplier<PipelineGroups>> pipelineGroupAuthorizationRegistry;
    private final ImmutableMap<String, Supplier<TemplatesConfig>> templateAuthorizationRegistry;

    @Autowired
    public InternalPipelineStructureControllerV1(ApiAuthenticationHelper apiAuthenticationHelper,
                                                 PipelineConfigService pipelineConfigService,
                                                 TemplateConfigService templateConfigService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.pipelineGroupAuthorizationRegistry = ImmutableMap.<String, Supplier<PipelineGroups>>builder()
                .put("view", () -> pipelineConfigService.viewableGroupsForUserIncludingConfigRepos(currentUsername()))
                .put("operate", () -> pipelineConfigService.viewableOrOperatableGroupsForIncludingConfigRepos(currentUsername()))
                .put("administer", () -> pipelineConfigService.adminGroupsForIncludingConfigRepos(currentUsername()))
                .build();

        this.templateAuthorizationRegistry = ImmutableMap.<String, Supplier<TemplatesConfig>>builder()
                .put("view", () -> templateConfigService.templateConfigsThatCanBeViewedBy(currentUsername()))
                .put("administer", () -> templateConfigService.templateConfigsThatCanBeEditedBy(currentUsername()))
                .build();
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
        String pipelineGroupAuthorizationType = request.queryParamOrDefault("pipeline_group_authorization", "view");
        String templateAuthorizationType = request.queryParamOrDefault("template_authorization", "view");

        Supplier<PipelineGroups> pipelineGroupsSupplier = pipelineGroupAuthorizationRegistry.get(pipelineGroupAuthorizationType);
        Supplier<TemplatesConfig> templatesConfigSupplier = templateAuthorizationRegistry.get(templateAuthorizationType);

        if (pipelineGroupsSupplier == null || templatesConfigSupplier == null) {
            HaltApiResponses.haltBecauseOfReason("Bad query parameter.");
        }

        PipelineGroups groups = pipelineGroupsSupplier.get();
        TemplatesConfig templateConfigs = templatesConfigSupplier.get();

        return writerForTopLevelObject(request, response, outputWriter -> InternalPipelineStructuresRepresenter.toJSON(outputWriter, groups, templateConfigs));
    }

}
