/*
 * Copyright 2021 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.webhook.controller.validation;

import com.thoughtworks.go.apiv1.webhook.helpers.WithMockRequests;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.thoughtworks.go.util.Iters.cat;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

class WebhookValidationTest implements WithMockRequests {
    private static final String SECRET = "you'll never guess!";

    @Nested
    class GitHubHooks {
        private static final String AUTH = "sha1=c678873db7ba38d8b6b69dd07a1ac8fdce8166fb";
        private WebhookValidation validator;

        @BeforeEach
        void setup() {
            validator = () -> SECRET;
        }

        @ParameterizedTest
        @ValueSource(strings = {"nope", "nay", "not yes"})
        void failsWhenEventNameIsNotAccepted(String event) {
            final BadRequestException e = assertThrows(BadRequestException.class, () ->
                    validator.validate(
                            github().event(event).auth(AUTH).build(),
                            GitHub.PUSH
                    )
            );
            assertEquals(format("Invalid event type `%s`. Allowed events are [push].", event), e.getMessage());
        }

        @Test
        void failsWhenAuthorizationHeaderIsAbsent() {
            final NotAuthorizedException e = assertThrows(NotAuthorizedException.class, () ->
                    validator.validate(
                            github().event("push").build(),
                            GitHub.PUSH
                    )
            );
            assertEquals("No HMAC signature specified via 'X-Hub-Signature' header!", e.getMessage());
        }

        @Test
        void failsWhenTokenDoesNotMatch() {
            final NotAuthorizedException e = assertThrows(NotAuthorizedException.class, () ->
                    validator.validate(
                            github().event("push").auth("these are not the secrets you are looking for").build(),
                            GitHub.PUSH
                    )
            );
            assertEquals("HMAC signature specified via 'X-Hub-Signature' did not match!", e.getMessage());
        }

        @Test
        void succeedsWhenTokenMatches() {
            assertDoesNotThrow(() -> validator.validate(
                    github().event("push").auth(AUTH).build(),
                    GitHub.PUSH)
            );
        }
    }

    @Nested
    class GitLabHooks {
        private static final String AUTH = SECRET;
        private WebhookValidation validator;

        @BeforeEach
        void setup() {
            validator = () -> SECRET;
        }

        @ParameterizedTest
        @ValueSource(strings = {"nope", "nay", "not yes"})
        void failsWhenEventNameIsNotAccepted(String event) {
            final BadRequestException e = assertThrows(BadRequestException.class, () ->
                    validator.validate(
                            gitlab().event(event).auth(AUTH).build(),
                            GitLab.PUSH
                    )
            );
            assertEquals(format("Invalid event type `%s`. Allowed events are [Push Hook].", event), e.getMessage());
        }

        @Test
        void failsWhenAuthorizationHeaderIsAbsent() {
            final NotAuthorizedException e = assertThrows(NotAuthorizedException.class, () ->
                    validator.validate(
                            gitlab().event("Push Hook").build(),
                            GitLab.PUSH
                    )
            );
            assertEquals("No token specified in the 'X-Gitlab-Token' header!", e.getMessage());
        }

        @Test
        void failsWhenTokenDoesNotMatch() {
            final NotAuthorizedException e = assertThrows(NotAuthorizedException.class, () ->
                    validator.validate(
                            github().event("Puah Hook").auth("these are not the secrets you are looking for").build(),
                            GitLab.PUSH
                    )
            );
            assertEquals("HMAC signature specified via 'X-Hub-Signature' did not match!", e.getMessage());
        }

        @Test
        void succeedsWhenTokenMatches() {
            assertDoesNotThrow(() -> validator.validate(
                    gitlab().event("Push Hook").auth(AUTH).build(),
                    GitLab.PUSH)
            );
        }
    }

    @Nested
    class BitbucketHooks {
        private static final String AUTH = SECRET;
        private WebhookValidation validator;

        @BeforeEach
        void setup() {
            validator = () -> SECRET;
        }

        @ParameterizedTest
        @ValueSource(strings = {"nope", "nay", "not yes"})
        void failsWhenEventNameIsNotAccepted(String event) {
            final BadRequestException e = assertThrows(BadRequestException.class, () ->
                    validator.validate(
                            bitbucket().event(event).auth(AUTH).build(),
                            Bitbucket.PUSH
                    )
            );
            assertEquals(format("Invalid event type `%s`. Allowed events are [repo:push].", event), e.getMessage());
        }

        @Test
        void failsWhenAuthorizationHeaderIsAbsent() {
            final NotAuthorizedException e = assertThrows(NotAuthorizedException.class, () ->
                    validator.validate(
                            bitbucket().event("repo:push").build(),
                            Bitbucket.PUSH
                    )
            );
            assertEquals("No token specified via basic authentication!", e.getMessage());
        }

        @Test
        void failsWhenTokenDoesNotMatch() {
            final NotAuthorizedException e = assertThrows(NotAuthorizedException.class, () ->
                    validator.validate(
                            bitbucket().event("repo:push").auth("these are not the secrets you are looking for").build(),
                            Bitbucket.PUSH
                    )
            );
            assertEquals("Token specified via basic authentication did not match!", e.getMessage());
        }

        @Test
        void succeedsWhenTokenMatches() {
            assertDoesNotThrow(() -> validator.validate(
                    bitbucket().event("repo:push").auth(AUTH).build(),
                    Bitbucket.PUSH)
            );
        }
    }

    @Nested
    class HostedBitbucketHooks {
        private static final String AUTH = "sha256=462c740e6257160ade468267ae5e71705ef2185a7908859a26383595468b0882";
        private WebhookValidation validator;

        @BeforeEach
        void setup() {
            validator = () -> SECRET;
        }

        @ParameterizedTest
        @ValueSource(strings = {"nope", "nay", "not yes"})
        void failsWhenEventNameIsNotAccepted(String event) {
            final BadRequestException e = assertThrows(BadRequestException.class, () ->
                    validator.validate(
                            hostedBitbucket().event(event).auth(AUTH).build(),
                            HostedBitbucket.PUSH
                    )
            );
            assertEquals(format("Invalid event type `%s`. Allowed events are [repo:refs_changed].", event), e.getMessage());
        }

        @Test
        void failsWhenAuthorizationHeaderIsAbsent() {
            final NotAuthorizedException e = assertThrows(NotAuthorizedException.class, () ->
                    validator.validate(
                            hostedBitbucket().event("repo:push").build(),
                            HostedBitbucket.PUSH
                    )
            );
            assertEquals("No HMAC signature specified via 'X-Hub-Signature' header!", e.getMessage());
        }

        @Test
        void failsWhenTokenDoesNotMatch() {
            final NotAuthorizedException e = assertThrows(NotAuthorizedException.class, () ->
                    validator.validate(
                            hostedBitbucket().event("repo:refs_changed").auth("these are not the secrets you are looking for").build(),
                            HostedBitbucket.PUSH
                    )
            );
            assertEquals("HMAC signature specified via 'X-Hub-Signature' did not match!", e.getMessage());
        }

        @Test
        void allowsPingWithoutAuthorization() {
            assertDoesNotThrow(() -> validator.validate(
                    hostedBitbucket().event("diagnostics:ping").build(),
                    cat(HostedBitbucket.PING, HostedBitbucket.PUSH)
                    )
            );
        }

        @Test
        void succeedsWhenTokenMatches() {
            assertDoesNotThrow(() -> validator.validate(
                    hostedBitbucket().event("repo:refs_changed").auth(AUTH).build(),
                    HostedBitbucket.PUSH)
            );
        }
    }
}