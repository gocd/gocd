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
import com.thoughtworks.go.apiv1.webhook.request.GitHubRequest;
import com.thoughtworks.go.apiv1.webhook.request.payload.Payload;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.service.ServerConfigService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.util.List;

import static spark.Spark.path;
import static spark.Spark.post;

@Component
public class WebhookControllerV1 extends ApiController implements SparkSpringController {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    protected final MaterialUpdateService materialUpdateService;
    protected final ServerConfigService serverConfigService;

    @Autowired
    public WebhookControllerV1(MaterialUpdateService materialUpdateService,
                               ServerConfigService serverConfigService) {
        super(ApiVersion.v1);
        this.materialUpdateService = materialUpdateService;
        this.serverConfigService = serverConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Webhook.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            post(Routes.Webhook.GITHUB, mimeType, this::github);
        });
    }

    protected String github(Request request, Response response) {
        GitHubRequest githubRequest = new GitHubRequest(request);
        githubRequest.validate(serverConfigService.getWebhookSecret());

        if (StringUtils.equalsIgnoreCase(githubRequest.getEvent(), "ping")) {
            return renderMessage(response, HttpStatus.ACCEPTED.value(), githubRequest.getPayload().getZen());
        }

        return notify(response, githubRequest.webhookUrls(), githubRequest.getPayload());
    }

    private String notify(Response response, List<String> webhookUrls, Payload payload) {
        LOGGER.info("[WebHook] Noticed a git push to {} on branch {}.", payload.getFullName(), payload.getBranch());

        if (materialUpdateService.updateGitMaterial(payload.getBranch(), webhookUrls)) {
            return renderMessage(response, HttpStatus.ACCEPTED.value(), "OK!");
        }
        return renderMessage(response, HttpStatus.ACCEPTED.value(), "No matching materials!");
    }
}
