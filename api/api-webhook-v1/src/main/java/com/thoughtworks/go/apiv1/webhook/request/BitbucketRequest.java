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

package com.thoughtworks.go.apiv1.webhook.request;

import spark.Request;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.split;

public class BitbucketRequest extends WebhookRequest {
    private static final Pattern TOKEN_MATCHER = Pattern.compile("Basic (.*)", Pattern.CASE_INSENSITIVE);

    public BitbucketRequest(Request request) {
        super(request);
    }

    @Override
    public String authToken() {
        final String authorizationHeader = request().headers("Authorization");
        if (isBlank(authorizationHeader)) {
            return null;
        }

        final Matcher matcher = TOKEN_MATCHER.matcher(authorizationHeader);
        if (matcher.matches()) {
            final String credentials = matcher.group(1);
            if (isBlank(credentials)) {
                return null;
            }

            final String decodedCredentials = new String(Base64.getDecoder().decode(credentials), UTF_8);
            return decodedCredentials.contains(":") ? split(decodedCredentials, ":", 2)[0] : decodedCredentials;
        }
        return null;
    }

    @Override
    public String event() {
        return request().headers("X-Event-Key");
    }
}
