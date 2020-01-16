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

import com.thoughtworks.go.apiv1.webhook.request.payload.BitBucketServerPayload;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.junit5.FileSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import spark.Request;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.*;

class BitBucketServerRequestTest {
    @Test
    void shouldSupportOnlyJsonPayload() {
        Request request = newRequest("repo:refs_changed", "", "{}");

        BitBucketServerRequest bitBucketServerRequest = new BitBucketServerRequest(request);

        assertThat(bitBucketServerRequest.supportedContentType())
            .hasSize(2)
            .contains(APPLICATION_JSON, APPLICATION_JSON_UTF8);
    }

    @ParameterizedTest
    @FileSource(files = "/bitbucket-server-payload.json")
    void shouldParsePayloadJson(String body) {
        Request request = newRequest("repo:refs_changed", "", body);

        BitBucketServerRequest bitBucketServerRequest = new BitBucketServerRequest(request);

        assertPayload(bitBucketServerRequest.getPayload());
    }

    @ParameterizedTest
    @FileSource(files = "/bitbucket-server-payload.json")
    void shouldGetRepoUrlsWithoutCredentialsFromPayload(String body) {
        Request request = newRequest("repo:refs_changed", "", body);

        BitBucketServerRequest bitBucketServerRequest = new BitBucketServerRequest(request);

        assertThat(bitBucketServerRequest.webhookUrls())
            .hasSize(2)
            .contains("ssh://bitbucket-server/gocd/spaceship.git",
                "http://bitbucket-server/scm/gocd/spaceship.git");
    }

    @Nested
    class Validate {
        @ParameterizedTest
        @ValueSource(strings = {"merge", "issue_created", "foo"})
        void shouldAllowOnlyRefChangedEvent(String event) {
            Request request = newRequest(event, "", "{}");

            assertThatCode(() -> new BitBucketServerRequest(request).validate("webhook-secret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(format("Invalid event type '%s'. Allowed events are [repo:refs_changed, diagnostics:ping]", event));
        }

        @Test
        void shouldSkipValidationForPingRequest() {
            Request request = newRequest("diagnostics:ping", "", "{}");

            assertThatCode(() -> new BitBucketServerRequest(request).validate("webhook-secret"))
                .doesNotThrowAnyException();
        }

        @Test
        void shouldErrorOutIfXHubSignatureHeaderIsMissing() {
            Request request = newRequest("repo:refs_changed", "", "{}");

            assertThatCode(() -> new BitBucketServerRequest(request).validate("webhook-secret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("No HMAC signature specified via 'X-Hub-Signature' header!");

        }

        @Test
        void shouldErrorOutWhenSignatureDoesNotMatch() {
            Request request = newRequest("repo:refs_changed", "random-signature", "{}");

            assertThatCode(() -> new BitBucketServerRequest(request).validate("webhook-secret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("HMAC signature specified via 'X-Hub-Signature' did not match!");
        }

        @Test
        void shouldErrorOutIfScmTypeIsNotGit() {
            String body = "{ \"repository\": { \"scmId\": \"svn\" } }";

            Request request = newRequest("repo:refs_changed", "sha256=3fdbe90db77c598e4bd54de9abefb5b5aa6797e8188bb9a09fa2e00ef9ab286e", body);

            assertThatCode(() -> new BitBucketServerRequest(request).validate("021757dde54540ff083dcf680688c4c9676e5c44"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only 'git' repositories are currently supported!");
        }

        @Test
        void shouldNotErrorOuForValidRequest() {
            String body = "{ \"repository\": { \"scmId\": \"git\" } }";

            Request request = newRequest("repo:refs_changed", "sha256=cd05cc9d2638cb5379a8bfc208fa8c16d50caf661f8047fe63bfe60e6b6efe50", body);

            assertThatCode(() -> new BitBucketServerRequest(request).validate("021757dde54540ff083dcf680688c4c9676e5c44"))
                .doesNotThrowAnyException();

        }
    }

    private void assertPayload(BitBucketServerPayload payload) {
        assertThat(payload.getBranch()).isEqualTo("release");
        assertThat(payload.getHostname()).isEqualTo("bitbucket-server");
        assertThat(payload.getScmType()).isEqualTo("git");
    }

    private Request newRequest(String event, String signature, String body) {
        Request request = mock(Request.class);
        when(request.headers("X-Hub-Signature")).thenReturn(signature);
        when(request.headers("X-Event-Key")).thenReturn(event);
        when(request.contentType()).thenReturn(APPLICATION_JSON_UTF8_VALUE);
        when(request.body()).thenReturn(body);
        return request;
    }
}
