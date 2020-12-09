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

import com.thoughtworks.go.apiv1.webhook.request.BitbucketRequest;
import spark.Request;

import java.util.Base64;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class BitbucketRequestBuilder {
    final Request req = mock(Request.class);

    public BitbucketRequestBuilder() {
        when(req.contentType()).thenReturn(APPLICATION_JSON_VALUE);
    }

    public BitbucketRequestBuilder event(final String event) {
        when(req.headers("X-Event-Key")).thenReturn(event);
        return this;
    }

    public BitbucketRequestBuilder auth(final String auth) {
        when(req.headers("Authorization")).thenReturn("Basic " + Base64.getEncoder().encodeToString(auth.getBytes()));
        return this;
    }

    public BitbucketRequestBuilder body(final String body) {
        when(req.body()).thenReturn(body);
        return this;
    }

    public BitbucketRequest build() {
        return new BitbucketRequest(this.req);
    }
}
