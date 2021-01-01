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
package com.thoughtworks.go.apiv1.accessToken;

import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.server.service.AccessTokenFilter;
import com.thoughtworks.go.server.service.AccessTokenService;
import com.thoughtworks.go.spark.Routes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static spark.Spark.*;

@Component
public class AdminUserAccessTokenControllerV1 extends AbstractUserAccessTokenControllerV1 {

    @Autowired
    public AdminUserAccessTokenControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, AccessTokenService AccessTokenService) {
        super(apiAuthenticationHelper, AccessTokenService);
    }

    @Override
    public String controllerBasePath() {
        return Routes.AdminUserAccessToken.BASE;
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

            before("", mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);
            before("/*", mimeType, this.apiAuthenticationHelper::checkAdminUserAnd403);

            get("", mimeType, this::getAllAccessTokens);
            post(Routes.AdminUserAccessToken.REVOKE, mimeType, this::revokeAccessToken);
            get(Routes.AdminUserAccessToken.ID, mimeType, this::getAccessToken);
        });
    }

    @Override
    protected List<AccessToken> allTokens(AccessTokenFilter filter) {
        return accessTokenService.findAllTokensForAllUsers(filter);
    }

    @Override
    Routes.FindUrlBuilder<Long> urlContext() {
        return new Routes.AdminUserAccessToken();
    }


}
