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
import com.thoughtworks.go.apiv4.agents.representers.AgentRepresenter;
import com.thoughtworks.go.apiv4.agents.representers.AgentsRepresenter;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.NullAgentInstance;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.EnvironmentConfigService;
import com.thoughtworks.go.server.service.SecurityService;
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
public class AgentsControllerV4 extends ApiController implements SparkSpringController, CrudController<AgentInstance> {
    private final AgentService agentService;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final SecurityService securityService;
    private final EnvironmentConfigService environmentConfigService;

    @Autowired
    public AgentsControllerV4(AgentService agentService, ApiAuthenticationHelper apiAuthenticationHelper, SecurityService securityService, EnvironmentConfigService environmentConfigService) {
        super(ApiVersion.v4);
        this.agentService = agentService;
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.securityService = securityService;
        this.environmentConfigService = environmentConfigService;
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
            before("/*", mimeType, apiAuthenticationHelper::checkUserAnd403);

            get("", mimeType, this::index);
            get(Routes.AgentsAPI.UUID, mimeType, this::show);

            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    public String index(Request request, Response response) throws IOException {
        return writerForTopLevelObject(request, response,
                outputWriter -> AgentsRepresenter.toJSON(outputWriter, agentService.agentEnvironmentMap(), securityService, currentUsername()));
    }

    public String show(Request request, Response response) throws IOException {
        final String uuid = request.params("uuid");

        return writerForTopLevelObject(request, response, outputWriter -> AgentRepresenter.toJSON(outputWriter, fetchEntityFromConfig(uuid), environmentConfigService.environmentsFor(uuid), securityService, currentUsername()));
    }

    @Override
    public String etagFor(AgentInstance entityFromServer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AgentInstance doFetchEntityFromConfig(String uuid) {
        final AgentInstance agentInstance = agentService.findAgent(uuid);

        return agentInstance instanceof NullAgentInstance ? null : agentInstance;
    }

    @Override
    public AgentInstance buildEntityFromRequestBody(Request req) {
        return null;
    }

    @Override
    public String jsonize(Request req, AgentInstance o) {
        return null; // to be implemented
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(AgentInstance agentConfigs) {
        return null;
    }
}
