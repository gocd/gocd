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

package com.thoughtworks.go.apiv1.webhook.helpers


import com.thoughtworks.go.apiv1.webhook.controller.validation.Bitbucket
import com.thoughtworks.go.apiv1.webhook.controller.validation.GitHub
import com.thoughtworks.go.apiv1.webhook.controller.validation.GitLab
import com.thoughtworks.go.apiv1.webhook.controller.validation.HostedBitbucket
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider

import java.util.stream.Stream

import static com.thoughtworks.go.apiv1.webhook.helpers.PostHelper.load
import static com.thoughtworks.go.spark.Routes.Webhook.ConfigRepo
import static com.thoughtworks.go.spark.Routes.Webhook.Notify
import static com.thoughtworks.go.util.Iters.first

class Fixtures {
    class ConfigReposWebhook {
        static class PullRequest implements ArgumentsProvider, PostHelper.Mixin {
            @Override
            Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        Arguments.of(ConfigRepo.GITHUB, { String s -> withGitHub(s) }, "/github-pr.json", first(GitHub.PR)),
                        Arguments.of(ConfigRepo.GITLAB, { String s -> withGitLab(s) }, "/gitlab-pr.json", first(GitLab.PR)),
                        Arguments.of(ConfigRepo.BITBUCKET, { String s -> withBitbucket(s) }, "/bitbucket-pr.json", first(Bitbucket.PR)),
                        Arguments.of(ConfigRepo.HOSTED_BITBUCKET, { String s -> withHostedBitbucket(s) }, "/hosted-bitbucket-pr.json", first(HostedBitbucket.PR))
                )
            }
        }

        static class Push implements ArgumentsProvider, PostHelper.Mixin {
            @Override
            Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        Arguments.of(ConfigRepo.GITHUB, { String s -> withGitHub(s) }, "/github-push.json", first(GitHub.PUSH)),
                        Arguments.of(ConfigRepo.GITLAB, { String s -> withGitLab(s) }, "/gitlab-push.json", first(GitLab.PUSH)),
                        Arguments.of(ConfigRepo.BITBUCKET, { String s -> withBitbucket(s) }, "/bitbucket-push.json", first(Bitbucket.PUSH)),
                        Arguments.of(ConfigRepo.HOSTED_BITBUCKET, { String s -> withHostedBitbucket(s) }, "/hosted-bitbucket-push.json", first(HostedBitbucket.PUSH))
                )
            }
        }

        static class Ping implements ArgumentsProvider, PostHelper.Mixin {
            @Override
            Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        Arguments.of(ConfigRepo.GITHUB, withGitHub([:]), first(GitHub.PING)),
                        Arguments.of(ConfigRepo.HOSTED_BITBUCKET, withHostedBitbucket([:]), first(HostedBitbucket.PING))
                )
            }
        }
    }

    class NotifyWebhook {
        static class Push implements ArgumentsProvider, PostHelper.Mixin {
            @Override
            Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        Arguments.of(Notify.GITHUB, { String s -> withGitHub(s) }, "/github-push.json", first(GitHub.PUSH)),
                        Arguments.of(Notify.GITLAB, { String s -> withGitLab(s) }, "/gitlab-push.json", first(GitLab.PUSH)),
                        Arguments.of(Notify.BITBUCKET, { String s -> withBitbucket(s) }, "/bitbucket-push.json", first(Bitbucket.PUSH)),
                        Arguments.of(Notify.HOSTED_BITBUCKET, { String s -> withHostedBitbucket(s) }, "/hosted-bitbucket-push.json", first(HostedBitbucket.PUSH))
                )
            }
        }

        static class PushWithTags implements ArgumentsProvider, PostHelper.Mixin {
            @Override
            Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        Arguments.of(Notify.GITHUB, { Map m -> withGitHub(m) }, githubTagPayload(), first(GitHub.PUSH)),
                        Arguments.of(Notify.GITLAB, { Map m -> withGitLab(m) }, gitlabTagPayload(), first(GitLab.PUSH)),
                        Arguments.of(Notify.BITBUCKET, { Map m -> withBitbucket(m) }, bitbucketTagPayload(), first(Bitbucket.PUSH)),
                        Arguments.of(Notify.HOSTED_BITBUCKET, { Map m -> withHostedBitbucket(m) }, hostedBitbucketTagPayload(), first(HostedBitbucket.PUSH))
                )
            }

            private static Map<String, Object> githubTagPayload() {
                load("/github-push.json") + ["ref": "refs/tags/1.0.0"]
            }

            private static Map<String, Object> gitlabTagPayload() {
                load("/gitlab-push.json") + ["ref": "refs/tags/1.0.0"]
            }

            private static Map<String, Object> bitbucketTagPayload() {
                def base = load("/bitbucket-push.json")
                base["push"]["changes"] = [["new": ["name": "release", "type": "tag"]]]
                return base
            }

            private static Map<String, Object> hostedBitbucketTagPayload() {
                load("/hosted-bitbucket-push.json") + ["changes": [["ref": ["displayId": "v1.0.0", "id": "refs/tags/v1.0.0", "type": "TAG"]]]]
            }
        }

        static class Ping implements ArgumentsProvider, PostHelper.Mixin {
            @Override
            Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        Arguments.of(Notify.GITHUB, withGitHub([:]), first(GitHub.PING)),
                        Arguments.of(Notify.HOSTED_BITBUCKET, withHostedBitbucket([:]), first(HostedBitbucket.PING))
                )
            }
        }
    }
}
