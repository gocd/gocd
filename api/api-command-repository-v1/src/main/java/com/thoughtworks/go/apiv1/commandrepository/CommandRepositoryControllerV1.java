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

package com.thoughtworks.go.apiv1.commandrepository;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv1.commandrepository.representer.CommandRepositoryLocationRepresenter;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.server.service.ServerConfigService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.function.Consumer;

import static spark.Spark.*;

@Component
public class CommandRepositoryControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private ServerConfigService serverConfigService;

    @Autowired
    public CommandRepositoryControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, ServerConfigService serverConfigService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.serverConfigService = serverConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.CommandRepository.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            get("", mimeType, this::index);
            put("", mimeType, this::update);

            before("", mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
        });
    }

    String index(Request request, Response response) throws IOException {
        String commandRepositoryLocation = serverConfigService.getCommandRepositoryLocation();
        return writerForTopLevelObject(request, response, jsonWriter(commandRepositoryLocation));
    }

    String update(Request request, Response response) throws IOException {
        String updatedLocation = buildEntityFromRequestBody(request);
        try {
            serverConfigService.updateCommandRepoLocation(updatedLocation);
        } catch (GoConfigInvalidException e) {
            response.status(HttpStatus.UNPROCESSABLE_ENTITY.value());
            return MessageJson.create(e.getAllErrorMessages(), jsonWriter(updatedLocation));
        }
        return writerForTopLevelObject(request, response, jsonWriter(updatedLocation));
    }

    private Consumer<OutputWriter> jsonWriter(String updatedLocation) {
        return writer -> CommandRepositoryLocationRepresenter.toJSON(writer, updatedLocation);
    }

    public String buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return jsonReader.getString("location");
    }
}
