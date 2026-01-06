/*
 * Copyright Thoughtworks, Inc.
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


import com.github.benmanes.caffeine.cache.Caffeine;
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
import com.thoughtworks.go.spark.GlobalExceptionMapper;
import com.thoughtworks.go.spark.Routes;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.caffeine.CaffeineProxyManager;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.time.Duration;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static spark.Spark.*;

public class EncryptionControllerDelegate extends ApiController {

    private final GoCipher cipher;
    private final long requestsPerMinute;
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final ProxyManager<Username> rateLimiters;

    public EncryptionControllerDelegate(ApiAuthenticationHelper apiAuthenticationHelper, GoCipher cipher, long requestsPerMinute, TimeMeter timeMeter) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.cipher = cipher;
        this.requestsPerMinute = requestsPerMinute;
        this.rateLimiters = new CaffeineProxyManager<>(
            Caffeine.newBuilder(),
            Duration.ofMinutes(1),
            ClientSideConfig.getDefault().withClientClock(timeMeter));
    }

    @Override
    public String controllerBasePath() {
        return Routes.Encrypt.BASE;
    }

    @Override
    public void setupRoutes(GlobalExceptionMapper exceptionMapper) {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);

            before("", mimeType, apiAuthenticationHelper::checkAnyAdminUserAnd403);
            before("/*", mimeType, apiAuthenticationHelper::checkAnyAdminUserAnd403);

            before("", mimeType, this::checkRateLimitAvailable);

            post("", mimeType, this::encrypt);

            exceptionMapper.register(CryptoException.class, (CryptoException exception, Request request, Response response) -> {
                response.status(HTTP_INTERNAL_ERROR);
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

    private void checkRateLimitAvailable(Request request, Response response) {
        BucketProxy tokenBucket = rateLimiters.getProxy(currentUsername(), this::newTokenBucket);

        response.header("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.header("X-RateLimit-Remaining", String.valueOf(tokenBucket.getAvailableTokens()));

        if (!tokenBucket.tryConsume(1)) {
            throw HaltApiResponses.haltBecauseRateLimitExceeded();
        }
    }

    private BucketConfiguration newTokenBucket() {
        return BucketConfiguration
            .builder()
            .addLimit(Bandwidth
                .builder()
                .capacity(requestsPerMinute)
                .refillIntervally(requestsPerMinute, Duration.ofMinutes(1))
                .initialTokens(requestsPerMinute)
                .build())
            .build();
    }
}
