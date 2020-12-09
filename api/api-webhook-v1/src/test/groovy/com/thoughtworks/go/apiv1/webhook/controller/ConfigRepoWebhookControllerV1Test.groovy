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

package com.thoughtworks.go.apiv1.webhook.controller

import com.thoughtworks.go.apiv1.webhook.controller.validation.Bitbucket
import com.thoughtworks.go.apiv1.webhook.helpers.Fixtures
import com.thoughtworks.go.apiv1.webhook.helpers.PostHelper
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.domain.materials.MaterialConfig
import com.thoughtworks.go.server.materials.MaterialUpdateService
import com.thoughtworks.go.server.service.ConfigRepoService
import com.thoughtworks.go.server.service.ServerConfigService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.mockito.Mock

import static com.thoughtworks.go.apiv1.webhook.controller.BaseWebhookController.PING_RESPONSE
import static com.thoughtworks.go.apiv1.webhook.helpers.PostHelper.SECRET
import static com.thoughtworks.go.util.Iters.first
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class ConfigRepoWebhookControllerV1Test implements SecurityServiceTrait, ControllerTrait<ConfigRepoWebhookControllerV1>, PostHelper.Mixin {
    static final String REPO_ID = "repo"

    @Mock
    private ConfigRepoService service

    @Mock
    private MaterialUpdateService materialUpdateService

    @Mock
    private ServerConfigService serverConfigService

    @BeforeEach
    void setUp() {
        initMocks(this)
        when(serverConfigService.webhookSecret).thenReturn(SECRET)
    }

    @Override
    ConfigRepoWebhookControllerV1 createControllerInstance() {
        return new ConfigRepoWebhookControllerV1(service, materialUpdateService, serverConfigService)
    }

    @Nested
    class Validation {
        @ParameterizedTest(name = "{0}")
        @ArgumentsSource(Fixtures.ConfigReposWebhook.PullRequest)
        void 'fails if event type is not acceptable'(String subpath, Closure<PostHelper> helper, String fixture) {
            final PostHelper req = helper(fixture)

            final String path = controller.controllerPath(subpath.replace(":id", REPO_ID))
            postWithApiHeader(path, req.header('Not a real event'), req.payload())
            verify(materialUpdateService, never()).updateMaterial(any() as MaterialConfig)

            assertThatResponse().
                    isBadRequest().
                    hasJsonMessage(~/^Invalid event type `Not a real event`\. Allowed events are \[[^]]+]\.$/)
        }

        @ParameterizedTest(name = "{0}")
        @ArgumentsSource(Fixtures.ConfigReposWebhook.PullRequest)
        void 'fails when signature does not match'(String subpath, Closure<PostHelper> helper, String fixture, String event) {
            final PostHelper req = helper(fixture)

            final String path = controller.controllerPath(subpath.replace(":id", REPO_ID))
            postWithApiHeader(path, req.header(event, 'will not auth'), req.payload())
            verify(materialUpdateService, never()).updateMaterial(any() as MaterialConfig)

            assertThatResponse().
                    isUnauthorized().
                    hasJsonMessage(~/.*not match.*/)
        }

        @Nested
        class BitbucketOnly implements PostHelper.Mixin {
            @Mock
            ConfigRepoConfig repo

            @Mock
            MaterialConfig material

            @BeforeEach
            void setup() {
                initMocks(this)

                when(repo.id).thenReturn(REPO_ID)
                when(service.getConfigRepo(REPO_ID)).thenReturn(repo)
                when(repo.repo).thenReturn(material)
                when(materialUpdateService.updateMaterial(material)).thenReturn(true)
            }

            @Test
            void 'extracts webhook secret from first segment of userinfo'() {
                PostHelper req = withBitbucket('/bitbucket-push.json')

                final String path = controller.controllerPath(Routes.Webhook.ConfigRepo.BITBUCKET.replace(":id", REPO_ID))
                def header = req.header(first(Bitbucket.PUSH), "${SECRET}:anything-can-go-here")

                postWithApiHeader(path, header, req.payload())
                verify(materialUpdateService, times(1)).updateMaterial(material)

                assertThatResponse()
                        .isAccepted()
                        .hasJsonMessage("OK!")
            }
        }
    }

    @Nested
    class PingSupport {
        @ParameterizedTest(name = "{0}")
        @ArgumentsSource(Fixtures.ConfigReposWebhook.Ping)
        void 'handles ping request'(String subpath, PostHelper req, String ping) {
            final String path = controller.controllerPath(subpath.replace(":id", REPO_ID))
            postWithApiHeader(path, req.header(ping), req.payload())
            verify(materialUpdateService, never()).updateMaterial(any() as MaterialConfig)

            assertThatResponse().
                    isAccepted().
                    hasJsonMessage(PING_RESPONSE)
        }
    }

    @Nested
    class Payloads {
        @Mock
        ConfigRepoConfig repo

        @Mock
        MaterialConfig material

        @BeforeEach
        void setup() {
            initMocks(this)

            when(repo.id).thenReturn(REPO_ID)
            when(service.getConfigRepo(REPO_ID)).thenReturn(repo)
            when(repo.repo).thenReturn(material)
            when(materialUpdateService.updateMaterial(material)).thenReturn(true)
        }

        @ParameterizedTest(name = "{0}")
        @ArgumentsSource(Fixtures.ConfigReposWebhook.PullRequest)
        void 'pull request events trigger config repos'(String subpath, Closure<PostHelper> helper, String fixture, String event) {
            final PostHelper req = helper(fixture)

            final String path = controller.controllerPath(subpath.replace(":id", REPO_ID))
            postWithApiHeader(path, req.header(event), req.payload())

            verify(materialUpdateService, times(1)).updateMaterial(material)
            assertThatResponse().
                    isAccepted().
                    hasJsonMessage("OK!")
        }

        @ParameterizedTest(name = "{0}")
        @ArgumentsSource(Fixtures.ConfigReposWebhook.Push)
        void 'push events trigger config repos'(String subpath, Closure<PostHelper> helper, String fixture, String event) {
            final PostHelper req = helper(fixture)

            final String path = controller.controllerPath(subpath.replace(":id", REPO_ID))
            postWithApiHeader(path, req.header(event), req.payload())

            verify(materialUpdateService, times(1)).updateMaterial(material)
            assertThatResponse().
                    isAccepted().
                    hasJsonMessage("OK!")
        }

        @ParameterizedTest(name = "{0}")
        @ArgumentsSource(Fixtures.ConfigReposWebhook.PullRequest)
        void 'reports when config repo is already updating'(String subpath, Closure<PostHelper> helper, String fixture, String event) {
            final PostHelper req = helper(fixture)

            when(materialUpdateService.updateMaterial(material)).thenReturn(false)

            final String path = controller.controllerPath(subpath.replace(":id", REPO_ID))
            postWithApiHeader(path, req.header(event), req.payload())

            verify(materialUpdateService, times(1)).updateMaterial(material)
            assertThatResponse().
                    isAccepted().
                    hasJsonMessage("ConfigRepo [repo] is already updating, quietly ignoring this webhook request")
        }

    }
}
