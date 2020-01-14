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

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.service.ServerConfigService;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.util.List;

import static spark.Spark.path;
import static spark.Spark.post;

public abstract class WebhookControllerV1 extends ApiController implements SparkSpringController {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    protected final MaterialUpdateService materialUpdateService;
    protected final ServerConfigService serverConfigService;

    public WebhookControllerV1(ApiAuthenticationHelper apiAuthenticationHelper,
                               MaterialUpdateService materialUpdateService,
                               ServerConfigService serverConfigService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.materialUpdateService = materialUpdateService;
        this.serverConfigService = serverConfigService;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
//            before("notify", mimeType, this::verifyContentOrigin);
//            before("notify", mimeType, this::premptPingCall);
//            before("notify", mimeType, this::allowOnlyPushEvent);

            post("notify", mimeType, this::notify);
        });
    }

    protected String notify(Request request, Response response) throws Exception {
        this.verifyContentOrigin(request);
        this.premptPingCall(request);
        this.verifyPayload(request);
        this.allowOnlyPushEvent(request);

        String branch = repoBranch(request);
        LOGGER.info("[WebHook] Noticed a git push to {} on branch {}.", repoNameForLogger(request), branch);

        if (materialUpdateService.updateGitMaterial(branch, this.webhookUrls(request))) {
            return renderMessage(response, HttpStatus.ACCEPTED.value(), "OK!");
        }
        return renderMessage(response, HttpStatus.ACCEPTED.value(), "No matching materials!");
    }

    protected abstract void verifyContentOrigin(Request request);

    protected abstract void premptPingCall(Request request);

    protected abstract void verifyPayload(Request request);

    protected abstract void allowOnlyPushEvent(Request request);

    protected abstract List<String> webhookUrls(Request request) throws Exception;

    protected abstract String repoBranch(Request request);

    protected abstract String repoNameForLogger(Request request) throws Exception;

    protected String webhookSecret() {
        return serverConfigService.getWebhookSecret();
    }
}
