/*
 * Copyright 2021 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.internalpipelinegroups;

import com.google.common.collect.ImmutableMap;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.apiv1.internalpipelinegroups.models.PipelineGroupsViewModel;
import com.thoughtworks.go.apiv1.internalpipelinegroups.representers.InternalPipelineGroupsRepresenter;
import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.server.service.EnvironmentConfigService;
import com.thoughtworks.go.server.service.PipelineConfigService;
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

/**
 * Internal PipelineGroups API, to be used on the environments SPA while editing pipelines.
 */
@Component
public class InternalPipelineGroupsControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final Map<String, Supplier<PipelineGroups>> pipelineGroupAuthorizationRegistry;
    private final EnvironmentConfigService environmentConfigService;

    @Autowired
    public InternalPipelineGroupsControllerV1(ApiAuthenticationHelper apiAuthenticationHelper,
                                              PipelineConfigService pipelineConfigService,
                                              EnvironmentConfigService environmentConfigService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.pipelineGroupAuthorizationRegistry = ImmutableMap.<String, Supplier<PipelineGroups>>builder()
                .put("view", () -> pipelineConfigService.viewableGroupsForUserIncludingConfigRepos(currentUsername()))
                .put("operate", () -> pipelineConfigService.viewableOrOperatableGroupsForIncludingConfigRepos(currentUsername()))
                .put("administer", () -> pipelineConfigService.adminGroupsForIncludingConfigRepos(currentUsername()))
                .build();
        this.environmentConfigService = environmentConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.InternalPipelineGroups.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", this.mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            before("/*", this.mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            get("", mimeType, this::index);
        });
    }

    public String index(Request request, Response response) throws IOException {
        String pipelineGroupAuthorizationType = request.queryParamOrDefault("pipeline_group_authorization", "view");
        Supplier<PipelineGroups> pipelineGroupsSupplier = pipelineGroupAuthorizationRegistry.get(pipelineGroupAuthorizationType);

        if (pipelineGroupsSupplier == null) {
            HaltApiResponses.haltBecauseOfReason("Bad query parameter.");
        }

        EnvironmentsConfig environments = new EnvironmentsConfig();
        environments.addAll(environmentConfigService.getEnvironments());

        PipelineGroupsViewModel pipelineGroupsViewModel = new PipelineGroupsViewModel(pipelineGroupsSupplier.get(), environments);

        return writerForTopLevelObject(request, response, outputWriter -> InternalPipelineGroupsRepresenter.toJSON(outputWriter, pipelineGroupsViewModel));
    }
}
