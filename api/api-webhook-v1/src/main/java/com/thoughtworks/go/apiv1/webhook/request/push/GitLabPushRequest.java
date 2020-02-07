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

package com.thoughtworks.go.apiv1.webhook.request.push;

import com.thoughtworks.go.apiv1.webhook.request.WebhookRequest;
import com.thoughtworks.go.apiv1.webhook.request.mixins.GuessUrlWebHook;
import com.thoughtworks.go.apiv1.webhook.request.mixins.gitlab.GitLabAuth;
import com.thoughtworks.go.apiv1.webhook.request.mixins.gitlab.GitLabEvents;
import com.thoughtworks.go.apiv1.webhook.request.payload.push.GitLabPushPayload;
import spark.Request;

import java.util.List;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.collections4.ListUtils.union;

public class GitLabPushRequest extends WebhookRequest<GitLabPushPayload> implements GitLabEvents, GitLabAuth, GuessUrlWebHook {
    public GitLabPushRequest(Request request) {
        super(request);
    }

    @Override
    public String[] allowedEvents() {
        return new String[]{"Push Hook"};
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
}
