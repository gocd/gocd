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

package com.thoughtworks.go.apiv4.agents;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv4.agents.representers.AgentsRepresenter;
import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.function.Consumer;

import static spark.Spark.*;

@Component
public class AgentsControllerV4 extends ApiController implements SparkSpringController, CrudController<Agents> {
    private final AgentService agentService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;

    @Autowired
    public AgentsControllerV4(AgentService agentService, ApiAuthenticationHelper apiAuthenticationHelper) {
        super(ApiVersion.v4);
        this.agentService = agentService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
    }

    @Override
    public String controllerBasePath() {
        return Routes.AgentsAPI.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", this::setContentType);
            before("/*", this::setContentType);
            before("", mimeType, apiAuthenticationHelper::checkUserAnd403);
            before("/*", mimeType, apiAuthenticationHelper::checkAdminUserAnd403);

            get("", mimeType, this::index);
        });
    }

    public String index(Request request, Response response) throws IOException {
        return writerForTopLevelObject(request, response,
                outputWriter -> AgentsRepresenter.toJSON(outputWriter, agentService.agentEnvironmentMap()));
    }

    @Override
    public String etagFor(Agents entityFromServer) {
        return null; // to be implemented
    }

    @Override
    public Agents doFetchEntityFromConfig(String name) {
        return null;
    }

    @Override
    public Agents buildEntityFromRequestBody(Request req) {
        return null;
    }

    @Override
    public String jsonize(Request req, Agents o) {
        return null; // to be implemented
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(Agents agentConfigs) {
        return null;
    }
}
