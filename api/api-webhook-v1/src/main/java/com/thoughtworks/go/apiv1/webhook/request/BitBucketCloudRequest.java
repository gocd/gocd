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

import com.thoughtworks.go.apiv1.webhook.GuessUrlWebHook;
import com.thoughtworks.go.apiv1.webhook.request.payload.BitBucketCloudPayload;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.h2.util.Utils;
import spark.Request;

import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.*;

public class BitBucketCloudRequest extends WebhookRequest<BitBucketCloudPayload> implements GuessUrlWebHook {
    private static final Pattern TOKEN_MATCHER = Pattern.compile("Basic (.*)", Pattern.CASE_INSENSITIVE);
    private final String token;

    public BitBucketCloudRequest(Request request) {
        super(request);
        token = getTokenFromHeader(request);
    }

    @Override
    public void validate(String webhookSecret) {
        if (!equalsAny(getEvent(), "repo:push")) {
            throw new BadRequestException(format("Invalid event type '%s'. Allowed events are [repo:push, diagnostics:ping]", getEvent()));
        }

        if (isBlank(token)) {
            throw new BadRequestException("No token specified via basic authentication!");
        }

        if (!Utils.compareSecure(token.getBytes(), webhookSecret.getBytes())) {
            throw new BadRequestException("Token specified via basic authentication did not match!");
        }

        if (!StringUtils.equals(getPayload().getScmType(), "git")) {
            throw new BadRequestException("Only 'git' repositories are currently supported!");
        }
    }

    @Override
    public List<String> webhookUrls() {
        return this.possibleUrls(getPayload().getHostname(), getPayload().getFullName());
    }

    @Override
    protected String parseEvent(Request request) {
        return request.headers("X-Event-Key");
    }

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
