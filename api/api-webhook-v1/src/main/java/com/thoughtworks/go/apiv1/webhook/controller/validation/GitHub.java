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

package com.thoughtworks.go.apiv1.webhook.controller.validation;

import com.thoughtworks.go.apiv1.webhook.request.WebhookRequest;
import com.thoughtworks.go.config.exceptions.HttpException;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

import java.security.MessageDigest;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.singleton;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class GitHub implements ValidateAuth.Provider {
    public static final Set<String> PUSH = singleton("push");
    public static final Set<String> PR = singleton("pull_request");
    public static final Set<String> PING = singleton("ping");

    public static String calculateSignature(String secret, String content) {
        return "sha1=" + new HmacUtils(HmacAlgorithms.HMAC_SHA_1, secret).hmacHex(content);
    }

    @Override
    public void auth(String webhookSecret, WebhookRequest request, Function<String, HttpException> fail) {
        final String signature = request.authToken();

        if (isBlank(signature)) {
            throw fail.apply("No HMAC signature specified via 'X-Hub-Signature' header!");
        }

        final String expectedSignature = calculateSignature(webhookSecret, request.contents());

        if (!MessageDigest.isEqual(expectedSignature.getBytes(), signature.getBytes())) {
            throw fail.apply("HMAC signature specified via 'X-Hub-Signature' did not match!");
        }
    }

    public GitHub() {
    }
}
