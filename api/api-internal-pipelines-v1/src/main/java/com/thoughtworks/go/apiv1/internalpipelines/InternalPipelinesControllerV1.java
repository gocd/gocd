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

package com.thoughtworks.go.apiv1.internalpipelines;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv1.internalpipelines.representers.PipelineConfigsWithMinimalAttributesRepresenter;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.exceptions.HttpException;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;

import static spark.Spark.*;

@Component
public class InternalPipelinesControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private PipelineConfigService pipelineConfigService;
    private EntityHashingService entityHashingService;

    @Autowired
    public InternalPipelinesControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, PipelineConfigService pipelineConfigService, EntityHashingService entityHashingService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.pipelineConfigService = pipelineConfigService;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Pipeline.INTERNAL_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, this.apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403);

            get("", mimeType, this::index);

            exception(HttpException.class, (Exception exception, Request request, Response response) -> {
                response.status(HttpStatus.UNPROCESSABLE_ENTITY.value());
                response.body(MessageJson.create(exception.getMessage()));
            });
        });
    }

    public String index(Request request, Response response) throws IOException {
        List<PipelineConfigs> pipelineConfigs = pipelineConfigService.viewableOrOperatableGroupsFor(SessionUtils.currentUsername());
        String etag = entityHashingService.md5ForEntity(new PipelineGroups(pipelineConfigs));

        if (fresh(request, etag)) {
            return notModified(response);
        }

        setEtagHeader(response, etag);
        return writerForTopLevelObject(request, response, outputWriter -> PipelineConfigsWithMinimalAttributesRepresenter.toJSON(outputWriter, pipelineConfigs));
    }

}