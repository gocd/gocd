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

package com.thoughtworks.go.apiv1.webhook.helpers;

import com.thoughtworks.go.apiv1.webhook.request.GitHubRequest;
import spark.Request;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class GitHubRequestBuilder {
    final Request req = mock(Request.class);

    public GitHubRequestBuilder() {
        when(req.contentType()).thenReturn(APPLICATION_JSON_VALUE);
    }

    public GitHubRequestBuilder event(final String event) {
        when(req.headers("X-GitHub-Event")).thenReturn(event);
        return this;
    }

    public GitHubRequestBuilder auth(final String auth) {
        when(req.headers("X-Hub-Signature")).thenReturn(auth);
        return this;
    }

    public GitHubRequestBuilder encoded() {
        when(req.contentType()).thenReturn(APPLICATION_FORM_URLENCODED_VALUE);
        return this;
    }

    public GitHubRequestBuilder body(final String body) {
        when(req.body()).thenReturn(body);
        return this;
    }

    public GitHubRequest build() {
        return new GitHubRequest(this.req);
    }
}
