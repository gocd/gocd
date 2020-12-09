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

import com.thoughtworks.go.apiv1.webhook.helpers.WithMockRequests;
import com.thoughtworks.go.apiv1.webhook.request.payload.push.GitHubPush;
import com.thoughtworks.go.junit5.FileSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.*;

class GitHubRequestTest implements WithMockRequests {
    @Test
    void acceptsJsonAndUrlEncodedPayloadTypes() {
        assertEquals(Set.of(APPLICATION_FORM_URLENCODED, APPLICATION_JSON, APPLICATION_JSON_UTF8),
                github().build().supportedContentTypes());
    }

    @ParameterizedTest
    @FileSource(files = "/github-push.json")
    void parsePayload(String body) {
        assertPayload(github().body(body).build().parsePayload(GitHubPush.class));
    }

    @ParameterizedTest
    @FileSource(files = "/github-push-url-encoded.json")
    void parsesUrlEncodedPayload(String urlEncodedPayload) {
        assertPayload(github().encoded().body(urlEncodedPayload).build().parsePayload(GitHubPush.class));
    }

    private void assertPayload(GitHubPush payload) {
        assertEquals("release", payload.branch());
        assertEquals("gocd/spaceship", payload.fullName());
        assertEquals("github.com", payload.hostname());
    }
}
