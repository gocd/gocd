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

import com.thoughtworks.go.apiv1.webhook.request.payload.GitLabPayload;
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

class GitLabRequestTest {
    @Test
    void shouldSupportOnlyJsonPayload() {
        Request request = newRequest("Push Hook", "", "{}");

        GitLabRequest gitLabRequest = new GitLabRequest(request);

        assertThat(gitLabRequest.supportedContentType())
            .hasSize(2)
            .contains(APPLICATION_JSON, APPLICATION_JSON_UTF8);
    }

    @ParameterizedTest
    @FileSource(files = "/github-lab.json")
    void shouldParsePayloadJson(String body) {
        Request request = newRequest("Push Hook", "", body);

        GitLabRequest gitLabRequest = new GitLabRequest(request);

        assertPayload(gitLabRequest.getPayload());
    }

    @ParameterizedTest
    @FileSource(files = "/github-lab.json")
    void shouldGuessTheGitLabUrls(String body) {
        Request request = newRequest("Push Hook", "", body);

        GitLabRequest gitLabRequest = new GitLabRequest(request);

        assertThat(gitLabRequest.webhookUrls())
            .hasSize(24)
            .contains(
                "https://gitlab.example.com/gocd/spaceship",
                "https://gitlab.example.com/gocd/spaceship/",
                "https://gitlab.example.com/gocd/spaceship.git",
                "https://gitlab.example.com/gocd/spaceship.git/",
                "http://gitlab.example.com/gocd/spaceship",
                "http://gitlab.example.com/gocd/spaceship/",
                "http://gitlab.example.com/gocd/spaceship.git",
                "http://gitlab.example.com/gocd/spaceship.git/",
                "git://gitlab.example.com/gocd/spaceship",
                "git://gitlab.example.com/gocd/spaceship/",
                "git://gitlab.example.com/gocd/spaceship.git",
                "git://gitlab.example.com/gocd/spaceship.git/",
                "git@gitlab.example.com:gocd/spaceship",
                "git@gitlab.example.com:gocd/spaceship/",
                "git@gitlab.example.com:gocd/spaceship.git",
                "git@gitlab.example.com:gocd/spaceship.git/",
                "ssh://git@gitlab.example.com/gocd/spaceship",
                "ssh://git@gitlab.example.com/gocd/spaceship/",
                "ssh://git@gitlab.example.com/gocd/spaceship.git",
                "ssh://git@gitlab.example.com/gocd/spaceship.git/",
                "gitlab@gitlab.example.com:gocd/spaceship",
                "gitlab@gitlab.example.com:gocd/spaceship/",
                "gitlab@gitlab.example.com:gocd/spaceship.git",
                "gitlab@gitlab.example.com:gocd/spaceship.git/"
            );
    }

    @Nested
    class Validate {
        @ParameterizedTest
        @ValueSource(strings = {"merge", "issue_created", "foo"})
        void shouldErrorOutIfEventTypeIsNotPush(String event) {
            Request request = newRequest(event, "", "{}");

            assertThatCode(() -> new GitLabRequest(request).validate("webhook-secret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(format("Invalid event type '%s'. Only 'Push Hook' event is allowed.", event));
        }

        @Test
        void shouldErrorOutIfTokenFromHeaderIsNull() {
            Request request = newRequest("Push Hook", null, "{}");

            assertThatCode(() -> new GitLabRequest(request).validate("webhook-secret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("No token specified in the 'X-Gitlab-Token' header!");

        }

        @Test
        void shouldErrorOutIfSignatureHeaderDoesNotMatch() {
            Request request = newRequest("Push Hook", "random-signature", "{}");

            assertThatCode(() -> new GitLabRequest(request).validate("webhook-secret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Token specified in the 'X-Gitlab-Token' header did not match!");

        }

        @Test
        void shouldBeValidWhenSignatureMatches() {
            Request request = newRequest("Push Hook", "021757dde54540ff083dcf680688c4c9676e5c44", "{}");

            assertThatCode(() -> new GitLabRequest(request).validate("021757dde54540ff083dcf680688c4c9676e5c44"))
                .doesNotThrowAnyException();

        }
    }

    private Request newRequest(String event, String signature, String body) {
        Request request = mock(Request.class);
        when(request.headers("X-Gitlab-Token")).thenReturn(signature);
        when(request.headers("X-Gitlab-Event")).thenReturn(event);
        when(request.contentType()).thenReturn(APPLICATION_JSON_VALUE);
        when(request.body()).thenReturn(body);

        return request;
    }

    private void assertPayload(GitLabPayload payload) {
        assertThat(payload.getBranch()).isEqualTo("release");
        assertThat(payload.getFullName()).isEqualTo("gocd/spaceship");
        assertThat(payload.getHostname()).isEqualTo("gitlab.example.com");
    }
}
