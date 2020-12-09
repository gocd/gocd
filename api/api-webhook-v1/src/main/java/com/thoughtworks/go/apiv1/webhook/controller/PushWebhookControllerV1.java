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

package com.thoughtworks.go.apiv1.webhook.controller;

import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.apiv1.webhook.controller.validation.Bitbucket;
import com.thoughtworks.go.apiv1.webhook.controller.validation.GitHub;
import com.thoughtworks.go.apiv1.webhook.controller.validation.GitLab;
import com.thoughtworks.go.apiv1.webhook.controller.validation.HostedBitbucket;
import com.thoughtworks.go.apiv1.webhook.request.BitbucketRequest;
import com.thoughtworks.go.apiv1.webhook.request.GitHubRequest;
import com.thoughtworks.go.apiv1.webhook.request.GitLabRequest;
import com.thoughtworks.go.apiv1.webhook.request.HostedBitbucketRequest;
import com.thoughtworks.go.apiv1.webhook.request.payload.push.*;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.service.ServerConfigService;
import com.thoughtworks.go.spark.Routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.util.List;

import static com.thoughtworks.go.util.Iters.cat;
import static spark.Spark.path;
import static spark.Spark.post;

@Component
public class PushWebhookControllerV1 extends BaseWebhookController {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    protected final MaterialUpdateService materialUpdateService;
    protected final ServerConfigService serverConfigService;

    @Autowired
    public PushWebhookControllerV1(MaterialUpdateService materialUpdateService,
                                   ServerConfigService serverConfigService) {
        super(ApiVersion.v1);
        this.materialUpdateService = materialUpdateService;
        this.serverConfigService = serverConfigService;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            post(Routes.Webhook.Notify.GITHUB, mimeType, this::github);
            post(Routes.Webhook.Notify.GITLAB, mimeType, this::gitlab);
            post(Routes.Webhook.Notify.BITBUCKET, mimeType, this::bitbucket);
            post(Routes.Webhook.Notify.HOSTED_BITBUCKET, mimeType, this::hostedBitbucket);
        });
    }

    protected String hostedBitbucket(Request request, Response response) {
        final HostedBitbucketRequest req = new HostedBitbucketRequest(request);
        validate(req, cat(HostedBitbucket.PUSH, HostedBitbucket.PING));

        if (is(HostedBitbucket.PING, req)) {
            return acknowledge(response);
        }

        return notify(response, req.parsePayload(HostedBitbucketPush.class), req.scmNamesQuery());
    }

    protected String bitbucket(Request request, Response response) {
        final BitbucketRequest req = new BitbucketRequest(request);
        validate(req, Bitbucket.PUSH);

        return notify(response, req.parsePayload(BitbucketPush.class), req.scmNamesQuery());
    }

    protected String gitlab(Request request, Response response) {
        final GitLabRequest req = new GitLabRequest(request);
        validate(req, GitLab.PUSH);

        return notify(response, req.parsePayload(GitLabPush.class), req.scmNamesQuery());
    }

    protected String github(Request request, Response response) {
        final GitHubRequest req = new GitHubRequest(request);
        validate(req, cat(GitHub.PUSH, GitHub.PING));

        if (is(GitHub.PING, req)) {
            return acknowledge(response);
        }

        return notify(response, req.parsePayload(GitHubPush.class), req.scmNamesQuery());
    }

    @Override
    public String webhookSecret() {
        return serverConfigService.getWebhookSecret();
    }

    private String notify(Response response, PushPayload payload, List<String> scmNames) {
        LOGGER.info("[WebHook] Noticed a git push to {} on branch {} with scm names {}.", payload.fullName(), payload.branch(), scmNames);

        if ("".equals(payload.branch())) { // probably a tag
            return accepted(response, "Ignoring push to non-branch.");
        }

        if (materialUpdateService.updateGitMaterial(payload.branch(), payload.repoUrls(), scmNames)) {
            return success(response);
        }

        return accepted(response, "No matching materials!");
    }
}
