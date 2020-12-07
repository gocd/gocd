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

package com.thoughtworks.go.apiv1.webhook.controller.validation;

import com.thoughtworks.go.apiv1.webhook.request.BitbucketRequest;
import com.thoughtworks.go.apiv1.webhook.request.GitHubRequest;
import com.thoughtworks.go.apiv1.webhook.request.GitLabRequest;
import com.thoughtworks.go.apiv1.webhook.request.HostedBitbucketRequest;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;

import java.util.Set;

public interface WebhookValidation extends ValidateAuth, ValidateEvent {
    default BadRequestException fail(String message) {
        return new BadRequestException(message);
    }

    default NotAuthorizedException unauthorized(String message) {
        return new NotAuthorizedException(message);
    }

    default void validate(GitHubRequest req, Set<String> allowedEvents) {
        auth(req);
        validateEvent(req, allowedEvents);
    }

    default void validate(GitLabRequest req, Set<String> allowedEvents) {
        auth(req);
        validateEvent(req, allowedEvents);
    }

    default void validate(BitbucketRequest req, Set<String> allowedEvents) {
        auth(req);
        validateEvent(req, allowedEvents);
    }

    default void validate(HostedBitbucketRequest req, Set<String> allowedEvents) {
        auth(req);
        validateEvent(req, allowedEvents);
    }
}
