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
import com.thoughtworks.go.apiv1.webhook.request.payload.GitHubPayload;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.h2.util.Utils;
import spark.Request;

import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.equalsAnyIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class GitHubRequest extends WebhookRequest<GitHubPayload> implements GuessUrlWebHook {
    protected final String signature;

    public GitHubRequest(Request request) {
        super(request);
        signature = request.headers("X-Hub-Signature");
    }

    @Override
    public void validate(String webhookSecret) {
        if (!equalsAnyIgnoreCase(getEvent(), "push", "ping")) {
            throw new BadRequestException(format("Invalid event type '%s'. Allowed events are [ping, push].", getEvent()));
        }

        validateSignature(webhookSecret);
    }

    @Override
    public List<String> webhookUrls() {
        return possibleUrls(getPayload().getHostname(), getPayload().getFullName());
    }

    private void validateSignature(String webhookSecret) {
        if (isBlank(signature)) {
            throw new BadRequestException("No HMAC signature specified via 'X-Hub-Signature' header!");
        }

        String expectedSignature = "sha1=" + new HmacUtils(HmacAlgorithms.HMAC_SHA_1, webhookSecret)
            .hmacHex(getRawBody());

        if (!Utils.compareSecure(expectedSignature.getBytes(), signature.getBytes())) {
            throw new BadRequestException("HMAC signature specified via 'X-Hub-Signature' did not match!");
        }
    }

    @Override
    protected String parseEvent(Request request) {
        return request.headers("X-GitHub-Event");
    }
}
