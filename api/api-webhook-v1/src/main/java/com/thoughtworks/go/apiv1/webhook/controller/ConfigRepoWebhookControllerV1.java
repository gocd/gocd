/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.apiv1.webhook.request.payload.Payload;
import com.thoughtworks.go.apiv1.webhook.request.payload.pr.BitbucketPR;
import com.thoughtworks.go.apiv1.webhook.request.payload.pr.GitHubPR;
import com.thoughtworks.go.apiv1.webhook.request.payload.pr.GitLabPR;
import com.thoughtworks.go.apiv1.webhook.request.payload.pr.HostedBitbucketPR;
import com.thoughtworks.go.apiv1.webhook.request.payload.push.BitbucketPush;
import com.thoughtworks.go.apiv1.webhook.request.payload.push.GitHubPush;
import com.thoughtworks.go.apiv1.webhook.request.payload.push.GitLabPush;
import com.thoughtworks.go.apiv1.webhook.request.payload.push.HostedBitbucketPush;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.service.ConfigRepoService;
import com.thoughtworks.go.server.service.ServerConfigService;
import com.thoughtworks.go.spark.Routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import static com.thoughtworks.go.util.Iters.cat;
import static java.lang.String.format;
import static spark.Spark.path;
import static spark.Spark.post;

@Component
public class ConfigRepoWebhookControllerV1 extends BaseWebhookController {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final ConfigRepoService service;
    private final MaterialUpdateService materialUpdateService;
    private final ServerConfigService serverConfigService;

    @Autowired
    public ConfigRepoWebhookControllerV1(ConfigRepoService service, MaterialUpdateService materialUpdateService, ServerConfigService serverConfigService) {
        super(ApiVersion.v1);
        this.service = service;
        this.materialUpdateService = materialUpdateService;
        this.serverConfigService = serverConfigService;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            post(Routes.Webhook.ConfigRepo.GITHUB, mimeType, this::github);
            post(Routes.Webhook.ConfigRepo.GITLAB, mimeType, this::gitlab);
            post(Routes.Webhook.ConfigRepo.BITBUCKET, mimeType, this::bitbucket);
            post(Routes.Webhook.ConfigRepo.HOSTED_BITBUCKET, mimeType, this::hostedBitbucket);
        });
    }

    String github(Request req, Response res) {
        final GitHubRequest github = new GitHubRequest(req);
        validate(github, cat(GitHub.PUSH, GitHub.PR, GitHub.PING));

        if (is(GitHub.PING, github)) {
            return acknowledge(res);
        }

        final Payload payload = determinePayload(github);

        if (payload instanceof GitHubPR) {
            final GitHubPR pr = (GitHubPR) payload;

            if (!pr.isInteresting()) {
                LOGGER.debug("[WebHook] Ignoring {} because we are not interested in action: {}", pr.descriptor(), pr.action());
                return success(res);
            }
        }

        return triggerRepoUpdate(res, payload, repoFromRequest(req));
    }

    String gitlab(Request req, Response res) {
        final GitLabRequest gitlab = new GitLabRequest(req);
        validate(gitlab, cat(GitLab.PUSH, GitLab.PR));

        return triggerRepoUpdate(res, determinePayload(gitlab), repoFromRequest(req));
    }

    String bitbucket(Request req, Response res) {
        final BitbucketRequest bitbucket = new BitbucketRequest(req);
        validate(bitbucket, cat(Bitbucket.PUSH, Bitbucket.PR));

        return triggerRepoUpdate(res, determinePayload(bitbucket), repoFromRequest(req));
    }

    String hostedBitbucket(Request req, Response res) {
        final HostedBitbucketRequest hostedBitbucket = new HostedBitbucketRequest(req);
        validate(hostedBitbucket, cat(HostedBitbucket.PUSH, HostedBitbucket.PR, HostedBitbucket.PING));

        if (is(HostedBitbucket.PING, hostedBitbucket)) {
            return acknowledge(res);
        }

        return triggerRepoUpdate(res, determinePayload(hostedBitbucket), repoFromRequest(req));
    }

    @Override
    public String webhookSecret() {
        return serverConfigService.getWebhookSecret();
    }

    private String triggerRepoUpdate(Response res, Payload payload, ConfigRepoConfig repo) {
        LOGGER.info("[WebHook] {} notifies ConfigRepo [{}]", payload.descriptor(), repo.getId());
        if (!materialUpdateService.updateMaterial(repo.getRepo())) {
            final String msg = format("ConfigRepo [%s] is already updating, quietly ignoring this webhook request", repo.getId());
            LOGGER.info("[WebHook] {}", msg);
            return accepted(res, msg);
        }
        return success(res);
    }

    private Payload determinePayload(GitHubRequest hook) {
        return is(GitHub.PR, hook) ? hook.parsePayload(GitHubPR.class) : hook.parsePayload(GitHubPush.class);
    }

    private Payload determinePayload(GitLabRequest hook) {
        return is(GitLab.PR, hook) ? hook.parsePayload(GitLabPR.class) : hook.parsePayload(GitLabPush.class);
    }

    private Payload determinePayload(BitbucketRequest hook) {
        return is(Bitbucket.PR, hook) ? hook.parsePayload(BitbucketPR.class) : hook.parsePayload(BitbucketPush.class);
    }

    private Payload determinePayload(HostedBitbucketRequest hook) {
        return is(HostedBitbucket.PR, hook) ? hook.parsePayload(HostedBitbucketPR.class) : hook.parsePayload(HostedBitbucketPush.class);
    }

    private ConfigRepoConfig repoFromRequest(Request req) {
        String repoId = req.params(":id");
        ConfigRepoConfig repo = service.getConfigRepo(repoId);

        if (null == repo) {
            throw new RecordNotFoundException(EntityType.ConfigRepo, repoId);
        }

        return repo;
    }
}
