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

import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.webhook.GuessUrlWebHook;
import com.thoughtworks.go.apiv1.webhook.request.payload.GitHubPayload;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.h2.util.Utils;
import org.springframework.util.MimeType;
import spark.Request;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.MediaType.*;

public class GitHubRequest extends WebhookRequest<GitHubPayload> implements GuessUrlWebHook {
    protected final String signature;

    public GitHubRequest(Request request) {
        super(request);
        signature = request.headers("X-Hub-Signature");
    }

    @Override
    public void validate(String webhookSecret) {
        if (!StringUtils.equalsAny(getEvent(), "push", "ping")) {
            LOGGER.error(format("[WebHook] Invalid event type '%s'. Allowed events are [ping, push].", getEvent()));
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
            LOGGER.error("[WebHook] No HMAC signature specified via 'X-Hub-Signature' header!");
            throw new BadRequestException("No HMAC signature specified via 'X-Hub-Signature' header!");
        }

        String expectedSignature = "sha1=" + new HmacUtils(HmacAlgorithms.HMAC_SHA_1, webhookSecret)
            .hmacHex(getRawBody());

        if (!Utils.compareSecure(expectedSignature.getBytes(), signature.getBytes())) {
            LOGGER.error("[WebHook] HMAC signature specified via 'X-Hub-Signature' did not match!");
            throw new BadRequestException("HMAC signature specified via 'X-Hub-Signature' did not match!");
        }
    }

    @Override
    protected List<MimeType> supportedContentType() {
        return List.of(APPLICATION_JSON, APPLICATION_JSON_UTF8, APPLICATION_FORM_URLENCODED);
    }

    @Override
    protected GitHubPayload parsePayload(MimeType contentType) {
        if (contentType.equals(APPLICATION_FORM_URLENCODED)) {
            LOGGER.debug(format("[WebHook] Parsing '%s' payload!", contentType));
            List<NameValuePair> formData = URLEncodedUtils.parse(getRawBody(), StandardCharsets.UTF_8);
            return GsonTransformer.getInstance().fromJson(formData.get(0).getValue(), getParameterClass());
        }

        return super.parsePayload(contentType);
    }

    @Override
    protected String parseEvent(Request request) {
        return request.headers("X-GitHub-Event");
    }
}
