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

package com.thoughtworks.go.apiv1.webhook.request.mixins.bitbucketcloud;

import com.thoughtworks.go.apiv1.webhook.request.mixins.HasAuth;
import org.h2.util.Utils;
import spark.Request;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.split;

public interface BitBucketCloudAuth extends HasAuth {
    Pattern TOKEN_MATCHER = Pattern.compile("Basic (.*)", Pattern.CASE_INSENSITIVE);

    default void validateAuth(String webhookSecret) {
        String token = getTokenFromHeader(request());

        if (isBlank(token)) {
            throw die("No token specified via basic authentication!");
        }

        if (!Utils.compareSecure(token.getBytes(), webhookSecret.getBytes())) {
            throw die("Token specified via basic authentication did not match!");
        }

        if (!"git".equals(scmType())) {
            throw die("Only 'git' repositories are currently supported!");
        }
    }

    String scmType();

    private String getTokenFromHeader(Request request) {
        String authorizationHeader = request.headers("Authorization");
        if (isBlank(authorizationHeader)) {
            return null;
        }

        Matcher matcher = TOKEN_MATCHER.matcher(authorizationHeader);
        if (matcher.matches()) {
            String credentials = matcher.group(1);
            if (isBlank(credentials)) {
                return null;
            }

            String encodedCredentials = new String(Base64.getDecoder().decode(credentials), UTF_8);
            return encodedCredentials.contains(":") ? split(encodedCredentials, ":", 2)[1] : encodedCredentials;
        }
        return null;
    }
}
