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

package com.thoughtworks.go.apiv1.webhook.controller

import com.thoughtworks.go.apiv1.webhook.controller.validation.Bitbucket
import com.thoughtworks.go.apiv1.webhook.helpers.Fixtures
import com.thoughtworks.go.apiv1.webhook.helpers.PostHelper
import com.thoughtworks.go.server.materials.MaterialUpdateService
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
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class PushWebhookControllerV1Test implements SecurityServiceTrait, ControllerTrait<PushWebhookControllerV1> {
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
    PushWebhookControllerV1 createControllerInstance() {
        new PushWebhookControllerV1(materialUpdateService, serverConfigService)
    }

    @Nested
    class Validation {
        @ParameterizedTest(name = "{0}")
        @ArgumentsSource(Fixtures.NotifyWebhook.Push)
        void 'fails if event type is not acceptable'(String path, Closure<PostHelper> helper, String fixture) {
            final PostHelper req = helper(fixture)

            postWithApiHeader(controller.controllerPath(path), req.header('Not a real event'), req.payload())
            verify(materialUpdateService, never()).updateGitMaterial(any(), anyCollection(), anyList())

            assertThatResponse().
                    isBadRequest().
                    hasJsonMessage(~/^Invalid event type `Not a real event`\. Allowed events are \[[^]]+]\.$/)
        }

        @ParameterizedTest(name = "{0}")
        @ArgumentsSource(Fixtures.NotifyWebhook.Push)
        void 'fails when signature does not match'(String path, Closure<PostHelper> helper, String fixture, String event) {
            final PostHelper req = helper(fixture)

            postWithApiHeader(controller.controllerPath(path), req.header(event, 'will not auth'), req.payload())
            verify(materialUpdateService, never()).updateGitMaterial(any(), anyCollection(), anyList())

            assertThatResponse().
                    isUnauthorized().
                    hasJsonMessage(~/.*not match.*/)
        }

        @Nested
        class BitbucketOnly implements PostHelper.Mixin {
            @Test
            void 'extracts webhook secret from first segment of userinfo'() {
                PostHelper req = withBitbucket('/bitbucket-push.json')
                when(materialUpdateService.updateGitMaterial(eq("release"), anyCollection(), anyList())).thenReturn(true)

                def path = controller.controllerPath(Routes.Webhook.Notify.BITBUCKET)
                def header = req.header(first(Bitbucket.PUSH), "${SECRET}:anything-can-go-here")

                postWithApiHeader(path, header, req.payload())
                verify(materialUpdateService, times(1)).updateGitMaterial(any(), anyCollection(), anyList())

                assertThatResponse()
                        .isAccepted()
                        .hasJsonMessage("OK!")
            }
        }
    }

    @Nested
    class PingSupport {
        @ParameterizedTest(name = "{0}")
        @ArgumentsSource(Fixtures.NotifyWebhook.Ping)
        void 'handles ping request'(String path, PostHelper req, String ping) {
            postWithApiHeader(controller.controllerPath(path), req.header(ping), req.payload())
            verify(materialUpdateService, never()).updateGitMaterial(any(), anyCollection(), anyList())

            assertThatResponse().
                    isAccepted().
                    hasJsonMessage(PING_RESPONSE)
        }
    }

    @Nested
    class Payloads {
        @ParameterizedTest(name = "{0}")
        @ArgumentsSource(Fixtures.NotifyWebhook.Push)
        void 'warns when material is not configured in GoCD'(String path, Closure<PostHelper> helper, String fixture, String event) {
            final PostHelper req = helper(fixture)

            when(materialUpdateService.updateGitMaterial(eq("release"), anyCollection(), anyList())).thenReturn(false)
            postWithApiHeader(controller.controllerPath(path), req.header(event), req.payload())

            verify(materialUpdateService, times(1)).updateGitMaterial(eq("release"), anyCollection(), anyList())
            assertThatResponse()
                    .isAccepted()
                    .hasJsonMessage("No matching materials!")
        }

        @ParameterizedTest(name = "{0}")
        @ArgumentsSource(Fixtures.NotifyWebhook.Push)
        void 'push event notifies materials'(String path, Closure<PostHelper> helper, String fixture, String event) {
            final PostHelper req = helper(fixture)

            when(materialUpdateService.updateGitMaterial(eq("release"), anyCollection(), anyList())).thenReturn(true)
            postWithApiHeader(controller.controllerPath(path), req.header(event), req.payload())

            verify(materialUpdateService, times(1)).updateGitMaterial(eq("release"), anyCollection(), anyList())
            assertThatResponse()
                    .isAccepted()
                    .hasJsonMessage("OK!")
        }

        @ParameterizedTest(name = "{0}")
        @ArgumentsSource(Fixtures.NotifyWebhook.Push)
        void 'accepts and notifies by scm names'(String path, Closure<PostHelper> helper, String fixture, String event) {
            final PostHelper req = helper(fixture)

            when(materialUpdateService.updateGitMaterial(eq("release"), anyCollection(), eq(["abc"]))).thenReturn(true)
            postWithApiHeader(controller.controllerPath(path) + "?SCM_NAME=abc", req.header(event), req.payload())

            verify(materialUpdateService).updateGitMaterial(eq("release"), anyCollection(), eq(["abc"]))
            assertThatResponse()
                    .isAccepted()
                    .hasJsonMessage("OK!")
        }

        @ParameterizedTest(name = "{0}")
        @ArgumentsSource(Fixtures.NotifyWebhook.PushWithTags)
        void 'ignores tags and other non-branch changes'(String path, Closure<PostHelper> helper, Map fixture, String event) {
            final PostHelper req = helper(fixture)

            postWithApiHeader(controller.controllerPath(path), req.header(event), req.payload())
            verify(materialUpdateService, never()).updateGitMaterial(any(), anyCollection(), anyList())

            assertThatResponse()
                    .isAccepted()
                    .hasJsonMessage("Ignoring push to non-branch.")
        }
    }
}
