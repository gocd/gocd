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

import com.thoughtworks.go.apiv1.webhook.request.HostedBitbucketRequest;
import spark.Request;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

public class HostedBitbucketRequestBuilder {
    final Request req = mock(Request.class);

    public HostedBitbucketRequestBuilder() {
        when(req.contentType()).thenReturn(APPLICATION_JSON_UTF8_VALUE);
    }

    public HostedBitbucketRequestBuilder event(final String event) {
        when(req.headers("X-Event-Key")).thenReturn(event);
        return this;
    }

    public HostedBitbucketRequestBuilder auth(final String auth) {
        when(req.headers("X-Hub-Signature")).thenReturn(auth);
        return this;
    }

    public HostedBitbucketRequestBuilder body(final String body) {
        when(req.body()).thenReturn(body);
        return this;
    }

    public HostedBitbucketRequest build() {
        return new HostedBitbucketRequest(this.req);
    }
}

