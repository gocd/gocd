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
package com.thoughtworks.go.apiv1.accessToken;

import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.AccessTokenFilter;
import com.thoughtworks.go.server.service.AccessTokenService;
import com.thoughtworks.go.server.service.SecurityAuthConfigService;
import com.thoughtworks.go.spark.Routes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.util.List;

import static spark.Spark.*;

@Component
public class CurrentUserAccessTokenControllerV1 extends AbstractUserAccessTokenControllerV1 {

    private SecurityAuthConfigService authConfigService;
    private AuthorizationExtension extension;

    @Autowired
    public CurrentUserAccessTokenControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, AccessTokenService AccessTokenService, SecurityAuthConfigService authConfigService, AuthorizationExtension extension) {
        super(apiAuthenticationHelper, AccessTokenService);
        this.authConfigService = authConfigService;
        this.extension = extension;
    }

    @Override
    public String controllerBasePath() {
        return Routes.CurrentUserAccessToken.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, this.apiAuthenticationHelper::ensureSecurityEnabled);
            before("/*", mimeType, this.apiAuthenticationHelper::ensureSecurityEnabled);

            before("", mimeType, this::verifyRequestIsNotUsingAccessToken);
            before("/*", mimeType, this::verifyRequestIsNotUsingAccessToken);

            before("", mimeType, this.apiAuthenticationHelper::checkUserAnd403);
            before("/*", mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            get("", mimeType, this::getAllAccessTokens);
            post("", mimeType, this::createAccessToken);
            post(Routes.CurrentUserAccessToken.REVOKE, mimeType, this::revokeAccessToken);
            get(Routes.CurrentUserAccessToken.ID, mimeType, this::getAccessToken);
        });
    }

    public String createAccessToken(Request request, Response response) throws Exception {
        String authConfigId = currentUserAuthConfigId(request);
        SecurityAuthConfig authConfig = authConfigService.findProfile(authConfigId);
        if (!extension.supportsPluginAPICallsRequiredForAccessToken(authConfig)) {
            response.status(422);
            return MessageJson.create(String.format("Can not create Access Token. Please upgrade '%s' plugin to use Access Token Feature.", authConfig.getPluginId()));
        }

        final JsonReader reader = GsonTransformer.getInstance().jsonReaderFrom(request.body());

        String tokenDescription = reader.optString("description").orElse(null);

        AccessToken created = accessTokenService.create(tokenDescription, currentUsernameString(), currentUserAuthConfigId(request));

        if (!created.persisted()) {
            response.status(422);
        }
        return renderAccessToken(request, response, created);
    }

    private String currentUserAuthConfigId(Request request) {
        return SessionUtils.getAuthenticationToken(request.raw()).getAuthConfigId();
    }

    @Override
    protected List<AccessToken> allTokens(AccessTokenFilter filter) {
        return accessTokenService.findAllTokensForUser(currentUsernameString(), filter);
    }

    @Override
    Routes.FindUrlBuilder<Long> urlContext() {
        return new Routes.CurrentUserAccessToken();
    }

}
