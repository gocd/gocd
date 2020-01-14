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

package com.thoughtworks.go.apiv1.webhook;

import com.google.gson.JsonObject;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.service.ServerConfigService;
import com.thoughtworks.go.spark.Routes;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.h2.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
public class GitHubControllerV1 extends WebhookControllerV1 implements GuessUrlWebHookController {

    @Autowired
    public GitHubControllerV1(ApiAuthenticationHelper apiAuthenticationHelper,
                              MaterialUpdateService materialUpdateService,
                              ServerConfigService serverConfigService) {
        super(apiAuthenticationHelper, materialUpdateService, serverConfigService);
    }

    @Override
    public String controllerBasePath() {
        return Routes.Webhook.GITHUB_BASE;
    }

    @Override
    protected void verifyContentOrigin(Request request) {
        String signature = request.headers("X-Hub-Signature");

        if (StringUtils.isBlank(signature)) {
            throw new BadRequestException("No HMAC signature specified via 'X-Hub-Signature' header!");
        }

        String expectedSignature = "sha1=" + new HmacUtils(HmacAlgorithms.HMAC_SHA_1, webhookSecret()).hmacHex(request.body());

        if (!Utils.compareSecure(expectedSignature.getBytes(), signature.getBytes())) {
            throw new BadRequestException("HMAC signature specified via 'X-Hub-Signature' did not match!");
        }
    }

    @Override
    protected void premptPingCall(Request request) {
        String event = request.headers("X-GitHub-Event");

        if (equalsIgnoreCase(event, "ping")) {
            throw new AcceptedAndIgnoredException(payload(request).get("zen").getAsString());
        }
    }


    @Override
    protected void allowOnlyPushEvent(Request request) {
        String event = request.headers("X-GitHub-Event");
        if (!equalsIgnoreCase(event, "push")) {
            throw new AcceptedAndIgnoredException(String.format("Ignoring event of type '%s'.", event));
        }
    }

    @Override
    protected void verifyPayload(Request request) {
        try {
            payload(request);
        } catch (Exception e) {
            LOGGER.warn("Could not understand github webhook payload:", e);
            throw new BadRequestException("Could not understand the payload!");
        }
    }

    @Override
    public String repoHostName(Request request) throws Exception {
        JsonObject payload = payload(request);

        JsonObject repository = payload.getAsJsonObject("repository");
        return new URI(repository.get("html_url").getAsString()).getHost();
    }

    @Override
    public String repoFullName(Request request) {
        JsonObject payload = payload(request);
        return payload.getAsJsonObject("repository").get("full_name").getAsString();
    }

    @Override
    public List<String> webhookUrls(Request request) throws Exception {
        return this.possibleUrls(request);
    }

    @Override
    protected String repoBranch(Request request) {
        JsonObject payload = payload(request);
        return RegExUtils.replaceAll(payload.get("ref").getAsString(), "refs/heads/", "");
    }

    @Override
    protected String repoNameForLogger(Request request) throws Exception {
        return String.join("/", repoHostName(request), repoFullName(request));
    }

    private JsonObject payload(Request request) {
        if (StringUtils.equals(request.contentType(), APPLICATION_FORM_URLENCODED_VALUE)) {
            List<NameValuePair> formData = URLEncodedUtils.parse(request.body(), StandardCharsets.UTF_8);
            return GsonTransformer.getInstance().fromJson(formData.get(0).getValue(), JsonObject.class);
        } else if (StringUtils.equals(request.contentType(), APPLICATION_JSON_VALUE)) {
            return GsonTransformer.getInstance().fromJson(request.body(), JsonObject.class);
        }

        throw new BadRequestException("Could not understand the payload!");
    }
}
