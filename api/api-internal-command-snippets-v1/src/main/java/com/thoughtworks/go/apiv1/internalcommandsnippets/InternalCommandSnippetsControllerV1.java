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
package com.thoughtworks.go.apiv1.internalcommandsnippets;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.apiv1.internalcommandsnippets.representers.CommandSnippetsRepresenter;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.lookups.CommandRepositoryService;
import com.thoughtworks.go.server.service.lookups.CommandSnippet;
import com.thoughtworks.go.server.service.lookups.CommandSnippets;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import java.io.IOException;
import java.util.List;

import static spark.Spark.*;

@Component
public class InternalCommandSnippetsControllerV1 extends ApiController implements SparkSpringController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final CommandRepositoryService commandRepositoryService;
    private final EntityHashingService entityHashingService;
    private final String PREFIX_PARAM = "prefix";

    @Autowired
    public InternalCommandSnippetsControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, CommandRepositoryService commandRepositoryService, EntityHashingService entityHashingService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.commandRepositoryService = commandRepositoryService;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.InternalCommandSnippets.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);

            get("", mimeType, this::index);
        });
    }

    public String index(Request request, Response response) throws IOException {
        String prefix = request.queryParams(PREFIX_PARAM);
        if (StringUtils.isEmpty(prefix)) {
            throw HaltApiResponses.haltBecauseRequiredParamMissing(PREFIX_PARAM);
        }

        List<CommandSnippet> commandSnippets = commandRepositoryService.lookupCommand(prefix);

        String etag = etagFor(commandSnippets);

        if (fresh(request, etag)) {
            return notModified(response);
        }

        setEtagHeader(response, etag);
        return writerForTopLevelObject(request, response, outputWriter -> CommandSnippetsRepresenter.toJSON(outputWriter, commandSnippets, prefix));
    }

    private String etagFor(List<CommandSnippet> commandSnippets) {
        return entityHashingService.md5ForEntity(new CommandSnippets(commandSnippets));
    }
}
