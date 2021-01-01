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
package com.thoughtworks.go.apiv1.usersearch;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.usersearch.representers.UserSearchResultsRepresenter;
import com.thoughtworks.go.presentation.UserSearchModel;
import com.thoughtworks.go.server.security.UserSearchService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;

import static com.thoughtworks.go.api.util.HaltApiResponses.haltBecauseOfReason;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static spark.Spark.*;

@Component
public class UserSearchControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final UserSearchService userSearchService;

    @Autowired
    public UserSearchControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, UserSearchService userSearchService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.userSearchService = userSearchService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.UserSearch.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);

            before("", this.mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            get("", this.mimeType, this::show);
        });
    }

    public String show(Request req, Response res) throws IOException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String searchTerm = req.queryParams("q");
        if (isBlank(searchTerm)) {
            throw haltBecauseOfReason("Search term not specified!");
        }
        List<UserSearchModel> userSearchModels = userSearchService.search(searchTerm, result);

        if (result.isSuccessful()) {
            return writerForTopLevelObject(req, res, writer -> UserSearchResultsRepresenter.toJSON(writer, searchTerm, userSearchModels));
        } else {
            return renderHTTPOperationResult(result, req, res);
        }
    }

}
