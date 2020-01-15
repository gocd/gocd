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
import com.thoughtworks.go.apiv1.webhook.request.payload.GitLabPayload;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.h2.util.Utils;
import spark.Request;

import java.util.List;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.collections4.ListUtils.union;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class GitLabRequest extends WebhookRequest<GitLabPayload> implements GuessUrlWebHook {
    private final String token;

    public GitLabRequest(Request request) {
        super(request);
        token = request.headers("X-Gitlab-Token");
    }

    @Override
    public void validate(String webhookSecret) {
        if (!StringUtils.equals(getEvent(), "Push Hook")) {
            throw new BadRequestException(format("Invalid event type '%s'. Only 'Push Hook' event is allowed.", getEvent()));
        }

        if (isBlank(token)) {
            throw new BadRequestException("No token specified in the 'X-Gitlab-Token' header!");
        }

        if (!Utils.compareSecure(token.getBytes(), webhookSecret.getBytes())) {
            throw new BadRequestException("Token specified in the 'X-Gitlab-Token' header did not match!");
        }
    }

    @Override
    public List<String> webhookUrls() {
        return unmodifiableList(union(
            possibleUrls(getPayload().getHostname(), getPayload().getFullName()),
            List.of(
                format("gitlab@%s:%s", getPayload().getHostname(), getPayload().getFullName()),
                format("gitlab@%s:%s/", getPayload().getHostname(), getPayload().getFullName()),
                format("gitlab@%s:%s.git", getPayload().getHostname(), getPayload().getFullName()),
                format("gitlab@%s:%s.git/", getPayload().getHostname(), getPayload().getFullName())
            )
        ));
    }

    @Override
    protected String parseEvent(Request request) {
        return request.headers("X-Gitlab-Event");
    }
}
