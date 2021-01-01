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

package com.thoughtworks.go.apiv1.webhook.controller.validation;

import com.thoughtworks.go.apiv1.webhook.request.*;
import com.thoughtworks.go.config.exceptions.HttpException;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;

import java.util.function.Function;

public interface ValidateAuth {
    String webhookSecret();

    NotAuthorizedException unauthorized(String message);

    default void auth(GitHubRequest request) {
        new GitHub().auth(webhookSecret(), request, this::unauthorized);
    }

    default void auth(GitLabRequest request) {
        new GitLab().auth(webhookSecret(), request, this::unauthorized);
    }

    default void auth(BitbucketRequest request) {
        new Bitbucket().auth(webhookSecret(), request, this::unauthorized);
    }

    default void auth(HostedBitbucketRequest request) {
        new HostedBitbucket().auth(webhookSecret(), request, this::unauthorized);
    }

    interface Provider {
        void auth(String webhookSecret, WebhookRequest request, Function<String, HttpException> fail);
    }
}
