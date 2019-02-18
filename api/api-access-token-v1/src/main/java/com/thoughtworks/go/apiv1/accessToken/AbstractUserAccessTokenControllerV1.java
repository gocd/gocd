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
import com.thoughtworks.go.config.exceptions.ConflictException;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.server.service.AccessTokenService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;

import static spark.Spark.exception;

abstract class AbstractUserAccessTokenControllerV1 extends ApiController implements SparkSpringController {
    protected final ApiAuthenticationHelper apiAuthenticationHelper;
    protected AccessTokenService accessTokenService;

    public AbstractUserAccessTokenControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, AccessTokenService AccessTokenService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.accessTokenService = AccessTokenService;
    }

    public String getAllAccessTokens(Request request, Response response) throws Exception {
        List<AccessToken> allTokens = allTokens();
        return writerForTopLevelObject(request, response, outputWriter -> AccessTokensRepresenter.toJSON(outputWriter, urlContext(), allTokens));
    }

    public String revokeAccessToken(Request request, Response response) throws Exception {
        long id = Long.parseLong(request.params(":id"));
        final JsonReader reader = GsonTransformer.getInstance().jsonReaderFrom(request.body());
        String revokeCause = reader.optString("cause").orElse(null);

        AccessToken revokeAccessToken = accessTokenService.revokeAccessToken(id, currentUsernameString(), revokeCause);

        return renderAccessToken(request, response, revokeAccessToken);
    }

    public String getAccessToken(Request request, Response response) throws Exception {
        final AccessToken token = accessTokenService.find(Long.parseLong(request.params(":id")), currentUsernameString());

        return renderAccessToken(request, response, token);
    }

    String renderAccessToken(Request request, Response response, AccessToken token) throws IOException {
        return writerForTopLevelObject(request, response, outputWriter -> AccessTokenRepresenter.toJSON(outputWriter, urlContext(), token));
    }

    protected abstract List<AccessToken> allTokens();

    abstract Routes.FindUrlBuilder<Long> urlContext();

    void addExceptionHandlers() {
        exception(RecordNotFoundException.class, this::notFound);
        exception(NotAuthorizedException.class, this::renderForbiddenResponse);
        exception(ConflictException.class, this::renderForbiddenResponse);
    }
}
