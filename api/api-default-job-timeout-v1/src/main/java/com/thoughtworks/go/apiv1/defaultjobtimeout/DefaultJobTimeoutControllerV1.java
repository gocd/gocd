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
package com.thoughtworks.go.apiv1.defaultjobtimeout;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv1.defaultjobtimeout.representers.DefaultJobTimeOutRepresenter;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.ServerConfigService;
import com.thoughtworks.go.spark.Routes.DefaultJobTimeout;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;

@Component
public class DefaultJobTimeoutControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;
    private final ServerConfigService serverConfigService;

    @Autowired
    public DefaultJobTimeoutControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, EntityHashingService entityHashingService, ServerConfigService serverConfigService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
        this.serverConfigService = serverConfigService;
    }

    @Override
    public String controllerBasePath() {
        return DefaultJobTimeout.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);

            get("", mimeType, this::index);
            post("", mimeType, this::createOrUpdate);
            put("", mimeType, this::createOrUpdate);


            exception(GoConfigInvalidException.class, (Exception exception, Request request, Response response) -> {
                response.status(HttpStatus.UNPROCESSABLE_ENTITY.value());
                response.body(MessageJson.create(exception.getMessage()));
            });
        });
    }

    String createOrUpdate(Request request, Response response) throws IOException {
        String defaultJobTimeout = buildEntityFromRequestBody(request);
        serverConfigService.createOrUpdateDefaultJobTimeout(defaultJobTimeout);
        return writerForTopLevelObject(request, response, writer -> DefaultJobTimeOutRepresenter.toJSON(writer, defaultJobTimeout));
    }

    String index(Request request, Response response) throws IOException {
        String defaultJobTimeout = serverConfigService.getDefaultJobTimeout();
        return writerForTopLevelObject(request, response, writer -> DefaultJobTimeOutRepresenter.toJSON(writer, defaultJobTimeout));
    }

    private String buildEntityFromRequestBody(Request request) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());
        return DefaultJobTimeOutRepresenter.fromJson(jsonReader);
    }
}
