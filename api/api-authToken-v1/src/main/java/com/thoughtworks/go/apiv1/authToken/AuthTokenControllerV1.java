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

package com.thoughtworks.go.apiv1.authToken;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.authToken.representers.AuthTokenRepresenter;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.AuthToken;
import com.thoughtworks.go.server.service.AuthTokenService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;

import static spark.Spark.*;

@Component
public class AuthTokenControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private AuthTokenService authTokenService;

    @Autowired
    public AuthTokenControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, AuthTokenService authTokenService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.authTokenService = authTokenService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.AuthToken.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);

            post("", mimeType, this::createAuthToken);
            get(Routes.AuthToken.TOKEN_NAME, mimeType, this::getAuthToken);
        });
    }

    public String createAuthToken(Request request, Response response) throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        final JsonReader reader = GsonTransformer.getInstance().jsonReaderFrom(request.body());

        String tokenName = reader.getString("name");
        String tokenDescription = reader.optString("description").orElse(null);

        authTokenService.create(tokenName, tokenDescription, result);

        if (result.isSuccessful()) {
            return renderAuthToken(request, response, findAuthToken(tokenName), true);
        }

        return renderHTTPOperationResult(result, request, response);
    }

    public String getAuthToken(Request request, Response response) throws Exception {
        final AuthToken token = findAuthToken(request.params("token_name"));
        if (token == null) {
            throw new RecordNotFoundException();
        }

        return renderAuthToken(request, response, token, false);
    }

    private String renderAuthToken(Request request, Response response, AuthToken token, boolean includeTokenValue) throws IOException {
        return writerForTopLevelObject(request, response, outputWriter -> AuthTokenRepresenter.toJSON(outputWriter, token, includeTokenValue));
    }

    private AuthToken findAuthToken(String tokenName) {
        return authTokenService.find(tokenName);
    }
}
