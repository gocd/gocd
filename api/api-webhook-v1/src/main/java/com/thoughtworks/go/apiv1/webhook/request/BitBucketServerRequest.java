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

import com.thoughtworks.go.apiv1.webhook.request.payload.BitBucketServerPayload;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.h2.util.Utils;
import spark.Request;

import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.equalsAny;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class BitBucketServerRequest extends WebhookRequest<BitBucketServerPayload> {
    private final String signature;

    public BitBucketServerRequest(Request request) {
        super(request);
        signature = request.headers("X-Hub-Signature");
    }

    @Override
    public void validate(String webhookSecret) {
        if (!equalsAny(getEvent(), "repo:refs_changed", "diagnostics:ping")) {
            LOGGER.error(format("[WebHook] Invalid event type '%s'. Allowed events are [repo:refs_changed, diagnostics:ping]", getEvent()));
            throw new BadRequestException(format("Invalid event type '%s'. Allowed events are [repo:refs_changed, diagnostics:ping]", getEvent()));
        }

        if (StringUtils.equals(getEvent(), "diagnostics:ping")) {
            LOGGER.debug("[WebHook] Skipping validation for ping request!");
            return;
        }

        if (isBlank(signature)) {
            LOGGER.error("[WebHook] No HMAC signature specified via 'X-Hub-Signature' header!");
            throw new BadRequestException("No HMAC signature specified via 'X-Hub-Signature' header!");
        }

        String expectedSignature = "sha256=" + new HmacUtils(HmacAlgorithms.HMAC_SHA_256, webhookSecret)
            .hmacHex(getRawBody());

        if (!Utils.compareSecure(expectedSignature.getBytes(), signature.getBytes())) {
            LOGGER.error("[WebHook] HMAC signature specified via 'X-Hub-Signature' did not match!");
            throw new BadRequestException("HMAC signature specified via 'X-Hub-Signature' did not match!");
        }

        if (!StringUtils.equals(getPayload().getScmType(), "git")) {
            LOGGER.error("[WebHook] Only 'git' repositories are currently supported!");
            throw new BadRequestException("Only 'git' repositories are currently supported!");
        }
    }

    @Override
    public List<String> webhookUrls() {
        return getPayload().getCloneUrls();
    }

    @Override
    protected String parseEvent(Request request) {
        return request.headers("X-Event-Key");
    }
}
