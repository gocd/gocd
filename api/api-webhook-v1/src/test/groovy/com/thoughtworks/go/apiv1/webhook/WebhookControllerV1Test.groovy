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

package com.thoughtworks.go.apiv1.webhook

import com.thoughtworks.go.server.materials.MaterialUpdateService
import com.thoughtworks.go.server.service.ServerConfigService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito

import static org.mockito.ArgumentMatchers.anyCollection
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.MockitoAnnotations.initMocks

class WebhookControllerV1Test implements SecurityServiceTrait, ControllerTrait<WebhookControllerV1> {
  @Mock
  private MaterialUpdateService materialUpdateService
  @Mock
  private ServerConfigService serverConfigService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  WebhookControllerV1 createControllerInstance() {
    new WebhookControllerV1(materialUpdateService, serverConfigService)
  }

  @Nested
  class Github {
    def payload = [
      "zen"       : "Keep it logically awesome.",
      "ref"       : "refs/heads/release",
      "repository": [
        "full_name": "gocd/spaceship",
        "html_url" : "https://github.com/gocd/spaceship"
      ]
    ]

    @Test
    void 'should error out if event type is not acceptable'() {
      def headers = [
        "X-Hub-Signature": "",
        "X-GitHub-Event" : "Foo"
      ]

      postWithApiHeader(controller.controllerPath(Routes.Webhook.GITHUB), headers, payload)

      assertThatResponse().isBadRequest()
        .hasJsonMessage("Invalid event type 'Foo'. Allowed events are [ping, push].")
    }

    @Test
    void 'should error out signature does not match'() {
      def headers = [
        "X-Hub-Signature": "foobar",
        "X-GitHub-Event" : "push"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("webhook-secret")

      postWithApiHeader(controller.controllerPath(Routes.Webhook.GITHUB), headers, payload)

      assertThatResponse().isBadRequest()
        .hasJsonMessage("HMAC signature specified via 'X-Hub-Signature' did not match!")
    }

    @Test
    void 'should handle ping request'() {
      def headers = [
        "X-Hub-Signature": "sha1=c699381b869f24e74db3ee95609c52ee1aad1a48",
        "X-GitHub-Event" : "PiNg"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("webhook-secret")

      postWithApiHeader(controller.controllerPath(Routes.Webhook.GITHUB), headers, payload)

      assertThatResponse()
        .isAccepted()
        .hasJsonMessage("Keep it logically awesome.")
    }

    @Test
    void 'should error out when material is not configured in GoCD'() {
      def headers = [
        "X-Hub-Signature": "sha1=c699381b869f24e74db3ee95609c52ee1aad1a48",
        "X-GitHub-Event" : "PuSh"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("webhook-secret")
      Mockito.when(materialUpdateService.updateGitMaterial(eq("release"), anyCollection())).thenReturn(false)

      postWithApiHeader(controller.controllerPath(Routes.Webhook.GITHUB), headers, payload)

      assertThatResponse()
        .isAccepted()
        .hasJsonMessage("No matching materials!")
    }

    @Test
    void 'should handle push request'() {
      def headers = [
        "X-Hub-Signature": "sha1=c699381b869f24e74db3ee95609c52ee1aad1a48",
        "X-GitHub-Event" : "PuSh"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("webhook-secret")
      Mockito.when(materialUpdateService.updateGitMaterial(eq("release"), anyCollection())).thenReturn(true)

      postWithApiHeader(controller.controllerPath(Routes.Webhook.GITHUB), headers, payload)

      assertThatResponse()
        .isAccepted()
        .hasJsonMessage("OK!")
    }
  }
}
