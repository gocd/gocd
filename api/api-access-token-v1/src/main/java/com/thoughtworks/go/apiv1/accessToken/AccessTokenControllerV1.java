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

package com.thoughtworks.go.apiv1.accessToken;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.accessToken.representers.AccessTokenRepresenter;
import com.thoughtworks.go.apiv1.accessToken.representers.AccessTokensRepresenter;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.AccessTokenService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;

import static spark.Spark.*;

@Component
public class AccessTokenControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private AccessTokenService accessTokenService;

    @Autowired
    public AccessTokenControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, AccessTokenService AccessTokenService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.accessTokenService = AccessTokenService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.AccessToken.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            before("/*", mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            get("", mimeType, this::getAllAccessTokens);
            post("", mimeType, this::createAccessToken);
            patch(String.format("%s%s/revoke", Routes.AccessToken.USERNAME, Routes.AccessToken.TOKEN_NAME), mimeType, this::revokeAccessToken);
            get(Routes.AccessToken.TOKEN_NAME, mimeType, this::getAccessToken);

            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    public String createAccessToken(Request request, Response response) throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        final JsonReader reader = GsonTransformer.getInstance().jsonReaderFrom(request.body());

        String tokenName = reader.getString("name");
        String tokenDescription = reader.optString("description").orElse(null);

        AccessToken created = accessTokenService.create(tokenName, tokenDescription, currentUsername(), currentUserAuthConfigId(request), result);

        if (result.isSuccessful()) {
            return renderAccessToken(request, response, created, true);
        }

        return renderHTTPOperationResult(result, request, response);
    }

    public String getAccessToken(Request request, Response response) throws Exception {
        final AccessToken token = accessTokenService.find(request.params("token_name"), currentUsername().getUsername().toString());

        if (token == null) {
            throw new RecordNotFoundException();
        }

        return renderAccessToken(request, response, token, false);
    }

    public String getAllAccessTokens(Request request, Response response) throws Exception {
        List<AccessToken> allTokens = accessTokenService.findAllTokensForUser(currentUsername());
        return writerForTopLevelObject(request, response, outputWriter -> AccessTokensRepresenter.toJSON(outputWriter, allTokens));
    }

    public String revokeAccessToken(Request request, Response response) throws Exception {
        String tokenName = request.params("token_name");
        String username = request.params("username");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        accessTokenService.revokeAccessToken(tokenName, username, result);

        if (result.isSuccessful()) {
            return renderAccessToken(request, response, accessTokenService.find(tokenName, username), true);
        }

        return renderHTTPOperationResult(result, request, response);
    }

    private String renderAccessToken(Request request, Response response, AccessToken token, boolean includeTokenValue) throws IOException {
        return writerForTopLevelObject(request, response, outputWriter -> AccessTokenRepresenter.toJSON(outputWriter, token, includeTokenValue));
    }

    private String currentUserAuthConfigId(Request request) {
        AuthenticationToken<?> authenticationToken = SessionUtils.getAuthenticationToken(request.raw());
        return authenticationToken.getAuthConfigId();
    }

}
