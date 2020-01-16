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

import com.thoughtworks.go.apiv1.webhook.request.payload.BitBucketCloudPayload;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.junit5.FileSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import spark.Request;

import java.util.Base64;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.*;

class BitBucketCloudRequestTest {
    @Test
    void shouldSupportOnlyJsonPayload() {
        Request request = newRequestWithAuthorizationHeader("repo:push", "", "{}");

        BitBucketCloudRequest bitBucketCloudRequest = new BitBucketCloudRequest(request);

        assertThat(bitBucketCloudRequest.supportedContentType())
            .hasSize(2)
            .contains(APPLICATION_JSON, APPLICATION_JSON_UTF8);
    }

    @ParameterizedTest
    @FileSource(files = "/bitbucket-payload.json")
    void shouldParsePayloadJson(String body) {
        Request request = newRequestWithAuthorizationHeader("repo:push", "", body);

        BitBucketCloudRequest bitBucketCloudRequest = new BitBucketCloudRequest(request);

        assertPayload(bitBucketCloudRequest.getPayload());
    }

    @ParameterizedTest
    @FileSource(files = "/bitbucket-payload.json")
    void shouldGuessTheWebhookUrls(String body) {
        Request request = newRequestWithAuthorizationHeader("repo:push", "", body);

        BitBucketCloudRequest bitBucketCloudRequest = new BitBucketCloudRequest(request);

        assertThat(bitBucketCloudRequest.webhookUrls())
            .hasSize(20)
            .contains(
                "https://bitbucket.org/gocd/spaceship",
                "https://bitbucket.org/gocd/spaceship/",
                "https://bitbucket.org/gocd/spaceship.git",
                "https://bitbucket.org/gocd/spaceship.git/",
                "http://bitbucket.org/gocd/spaceship",
                "http://bitbucket.org/gocd/spaceship/",
                "http://bitbucket.org/gocd/spaceship.git",
                "http://bitbucket.org/gocd/spaceship.git/",
                "git://bitbucket.org/gocd/spaceship",
                "git://bitbucket.org/gocd/spaceship/",
                "git://bitbucket.org/gocd/spaceship.git",
                "git://bitbucket.org/gocd/spaceship.git/",
                "git@bitbucket.org:gocd/spaceship",
                "git@bitbucket.org:gocd/spaceship/",
                "git@bitbucket.org:gocd/spaceship.git",
                "git@bitbucket.org:gocd/spaceship.git/",
                "ssh://git@bitbucket.org/gocd/spaceship",
                "ssh://git@bitbucket.org/gocd/spaceship/",
                "ssh://git@bitbucket.org/gocd/spaceship.git",
                "ssh://git@bitbucket.org/gocd/spaceship.git/"
            );
    }

    @Nested
    class Validate {
        @ParameterizedTest
        @ValueSource(strings = {"merge", "issue_created", "foo"})
        void shouldErrorOutIfEventTypeIsNotPush(String event) {
            Request request = newRequestWithAuthorizationHeader(event, "", "{}");

            assertThatCode(() -> new BitBucketCloudRequest(request).validate("webhook-secret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(format("Invalid event type '%s'. Allowed events are [repo:push]", event));
        }

        @Test
        void shouldErrorOutIfAuthorizationHeaderIsNotSpecified() {
            Request request = newRequestWithoutAuthorizationHeader("repo:push", "{}");

            assertThatCode(() -> new BitBucketCloudRequest(request).validate("webhook-secret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("No token specified via basic authentication!");

        }

        @Test
        void shouldErrorOutIfTokenDoesNotMatch() {
            Request request = newRequestWithAuthorizationHeader("repo:push", "random-signature", "{}");

            assertThatCode(() -> new BitBucketCloudRequest(request).validate("webhook-secret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Token specified via basic authentication did not match!");
        }

        @Test
        void shouldErrorOutIfScmTypeIsNotGit() {
            String body = "{ \"repository\": { \"scm\": \"svn\" } }";

            Request request = newRequestWithAuthorizationHeader("repo:push", "021757dde54540ff083dcf680688c4c9676e5c44", body);

            assertThatCode(() -> new BitBucketCloudRequest(request).validate("021757dde54540ff083dcf680688c4c9676e5c44"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only 'git' repositories are currently supported!");
        }

        @Test
        void shouldBeValidWhenTokenMatches() {
            String body = "{ \"repository\": { \"scm\": \"git\" } }";

            Request request = newRequestWithAuthorizationHeader("repo:push", "021757dde54540ff083dcf680688c4c9676e5c44", body);

            assertThatCode(() -> new BitBucketCloudRequest(request).validate("021757dde54540ff083dcf680688c4c9676e5c44"))
                .doesNotThrowAnyException();

        }
    }

    private void assertPayload(BitBucketCloudPayload payload) {
        assertThat(payload.getBranch()).isEqualTo("release");
        assertThat(payload.getFullName()).isEqualTo("gocd/spaceship");
        assertThat(payload.getHostname()).isEqualTo("bitbucket.org");
        assertThat(payload.getScmType()).isEqualTo("git");
    }

    private Request newRequestWithAuthorizationHeader(String event, String token, String body) {
        Request request = newRequestWithoutAuthorizationHeader(event, body);
        when(request.headers("Authorization")).thenReturn("Basic " + Base64.getEncoder().encodeToString(token.getBytes()));
        return request;
    }

    private Request newRequestWithoutAuthorizationHeader(String event, String body) {
        Request request = mock(Request.class);
        when(request.headers("X-Event-Key")).thenReturn(event);
        when(request.contentType()).thenReturn(APPLICATION_JSON_VALUE);
        when(request.body()).thenReturn(body);
        return request;
    }
}
