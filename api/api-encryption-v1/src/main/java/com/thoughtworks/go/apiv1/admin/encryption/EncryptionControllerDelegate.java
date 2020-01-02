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
package com.thoughtworks.go.apiv1.admin.encryption;


import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.api.util.HaltApiMessages;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.apiv1.admin.encryption.representers.EncryptedValueRepresenter;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.spark.Routes;
import org.isomorphism.util.FixedIntervalRefillStrategy;
import org.isomorphism.util.TokenBucket;
import org.isomorphism.util.TokenBuckets;
import org.springframework.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static spark.Spark.*;

public class EncryptionControllerDelegate extends ApiController {

    private final GoCipher cipher;
    private final long requestsPerMinute;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final Cache<Username, TokenBucket> rateLimiters;
    private final Ticker ticker;


    public EncryptionControllerDelegate(ApiAuthenticationHelper apiAuthenticationHelper, GoCipher cipher, long requestsPerMinute, Ticker ticker) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.cipher = cipher;
        this.requestsPerMinute = requestsPerMinute;
        this.rateLimiters = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();
        this.ticker = ticker;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Encrypt.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);

            before("", mimeType, apiAuthenticationHelper::checkAnyAdminUserAnd403);
            before("/*", mimeType, apiAuthenticationHelper::checkAnyAdminUserAnd403);

            before("", mimeType, this::checkRateLimitAvailable);

            post("", mimeType, this::encrypt);

            exception(CryptoException.class, (CryptoException exception, Request request, Response response) -> {
                response.status(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.body(MessageJson.create(HaltApiMessages.errorWhileEncryptingMessage()));
            });
        });
    }

    public String encrypt(Request request, Response response) throws CryptoException, IOException {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(request.body());
        String value = jsonReader.getString("value");

        String encrypt = cipher.encrypt(value);

        return writerForTopLevelObject(request, response, writer -> EncryptedValueRepresenter.toJSON(writer, encrypt));
    }

    private void checkRateLimitAvailable(Request request, Response response) throws ExecutionException {
        TokenBucket tokenBucket = rateLimiters.get(currentUsername(), () -> TokenBuckets.builder()
            .withCapacity(requestsPerMinute)
            .withInitialTokens(requestsPerMinute)
            .withRefillStrategy(new FixedIntervalRefillStrategy(ticker, requestsPerMinute, 1, TimeUnit.MINUTES))
            .build());

        response.header("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.header("X-RateLimit-Remaining", String.valueOf(tokenBucket.getNumTokens()));

        if (!tokenBucket.tryConsume()) {
            throw HaltApiResponses.haltBecauseRateLimitExceeded();
        }
    }
}
