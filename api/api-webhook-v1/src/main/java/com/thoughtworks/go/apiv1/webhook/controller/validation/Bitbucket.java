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

package com.thoughtworks.go.apiv1.webhook.controller.validation;

import com.thoughtworks.go.apiv1.webhook.request.WebhookRequest;
import com.thoughtworks.go.config.exceptions.HttpException;

import java.security.MessageDigest;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.singleton;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class Bitbucket implements ValidateAuth.Provider {
    public static final Set<String> PUSH = singleton("repo:push");
    public static final Set<String> PR = Set.of("pullrequest:created", "pullrequest:fulfilled", "pullrequest:rejected");

    @Override
    public void auth(final String webhookSecret, final WebhookRequest request, final Function<String, HttpException> fail) {
        final String token = request.authToken();

        if (isBlank(token)) {
            throw fail.apply("No token specified via basic authentication!");
        }

        if (!MessageDigest.isEqual(webhookSecret.getBytes(), token.getBytes())) {
            throw fail.apply("Token specified via basic authentication did not match!");
        }
    }


    public Bitbucket() {
    }
}
