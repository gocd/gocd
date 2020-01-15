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

import com.thoughtworks.go.apiv1.webhook.request.payload.GitHubPayload;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.junit5.FileSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.MimeType;
import spark.Request;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.*;

class GitHubRequestTest {
    @Test
    void shouldSupportJsonAndUrlEncodedPayload() {
        Request request = newRequest("push", "", "{}", APPLICATION_JSON);

        GitHubRequest gitHubRequest = new GitHubRequest(request);

        assertThat(gitHubRequest.supportedContentType())
            .hasSize(2)
            .contains(APPLICATION_FORM_URLENCODED_VALUE, APPLICATION_JSON_VALUE);
    }

    @ParameterizedTest
    @FileSource(files = "/github-payload.json")
    void shouldParseWebhookRequestToGitHubRequest(String body) {
        Request request = newRequest("push", "", body, APPLICATION_JSON);

        GitHubRequest gitHubRequest = new GitHubRequest(request);

        assertThat(gitHubRequest.getEvent()).isEqualTo("push");
        assertPayload(gitHubRequest.getPayload());
    }

    @ParameterizedTest
    @FileSource(files = "/github-url-encoded-payload.json")
    void shouldParseRequestWithUrlEncodedPayload(String urlEncodedPayload) {
        Request request = newRequest("push", "", urlEncodedPayload, APPLICATION_FORM_URLENCODED);

        GitHubRequest gitHubRequest = new GitHubRequest(request);

        assertThat(gitHubRequest.getEvent()).isEqualTo("push");
        assertPayload(gitHubRequest.getPayload());
    }

    @Nested
    class Validate {
        @ParameterizedTest
        @ValueSource(strings = {"merge", "issue_created", "foo"})
        void shouldErrorOutIfEventTypeIsNotPingOrPush(String event) {
            Request request = newRequest(event, "", "{}", APPLICATION_JSON);

            assertThatCode(() -> new GitHubRequest(request).validate("webhook-secret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(format("Invalid event type '%s'. Allowed events are [ping, push].", event));
        }

        @Test
        void shouldErrorOutIfSignatureHeaderIsNull() {
            Request request = newRequest("push", null, "{}", APPLICATION_JSON);

            assertThatCode(() -> new GitHubRequest(request).validate("webhook-secret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("No HMAC signature specified via 'X-Hub-Signature' header!");

        }

        @Test
        void shouldErrorOutIfSignatureHeaderDoesNotMatch() {
            Request request = newRequest("push", "random-signature", "{}", APPLICATION_JSON);

            assertThatCode(() -> new GitHubRequest(request).validate("webhook-secret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("HMAC signature specified via 'X-Hub-Signature' did not match!");

        }

        @ParameterizedTest
        @ValueSource(strings = {"push", "ping"})
        void shouldBeValidWhenEventTypeAndSignatureMatches(String event) {
            Request request = newRequest(event, "sha1=021757dde54540ff083dcf680688c4c9676e5c44", "{}", APPLICATION_JSON);

            assertThatCode(() -> new GitHubRequest(request).validate("webhook-secret"))
                .doesNotThrowAnyException();

        }
    }

    private Request newRequest(String event, String signature, String body, MimeType contentType) {
        Request request = mock(Request.class);
        when(request.headers("X-Hub-Signature")).thenReturn(signature);
        when(request.headers("X-GitHub-Event")).thenReturn(event);
        when(request.contentType()).thenReturn(contentType.toString());
        when(request.body()).thenReturn(body);

        return request;
    }

    private void assertPayload(GitHubPayload payload) {
        assertThat(payload.getZen()).isEqualTo("Keep it logically awesome.");
        assertThat(payload.getBranch()).isEqualTo("release");
        assertThat(payload.getFullName()).isEqualTo("gocd/spaceship");
        assertThat(payload.getHostname()).isEqualTo("github.com");
    }
}
