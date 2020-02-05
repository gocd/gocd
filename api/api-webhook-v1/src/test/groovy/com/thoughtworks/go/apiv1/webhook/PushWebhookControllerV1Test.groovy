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

class PushWebhookControllerV1Test implements SecurityServiceTrait, ControllerTrait<PushWebhookControllerV1> {
  @Mock
  private MaterialUpdateService materialUpdateService
  @Mock
  private ServerConfigService serverConfigService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  PushWebhookControllerV1 createControllerInstance() {
    new PushWebhookControllerV1(materialUpdateService, serverConfigService)
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

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.GITHUB), headers, payload)

      assertThatResponse().isBadRequest()
        .hasJsonMessage("Invalid event type `Foo`. Allowed events are [push, ping].")
    }

    @Test
    void 'should error out signature does not match'() {
      def headers = [
        "X-Hub-Signature": "foobar",
        "X-GitHub-Event" : "push"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("webhook-secret")

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.GITHUB), headers, payload)

      assertThatResponse().isBadRequest()
        .hasJsonMessage("HMAC signature specified via 'X-Hub-Signature' did not match!")
    }

    @Test
    void 'should handle ping request'() {
      def headers = [
        "X-Hub-Signature": "sha1=c699381b869f24e74db3ee95609c52ee1aad1a48",
        "X-GitHub-Event" : "ping"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("webhook-secret")

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.GITHUB), headers, payload)

      assertThatResponse()
        .isAccepted()
        .hasJsonMessage("Keep it logically awesome.")
    }

    @Test
    void 'should error out when material is not configured in GoCD'() {
      def headers = [
        "X-Hub-Signature": "sha1=c699381b869f24e74db3ee95609c52ee1aad1a48",
        "X-GitHub-Event" : "push"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("webhook-secret")
      Mockito.when(materialUpdateService.updateGitMaterial(eq("release"), anyCollection())).thenReturn(false)

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.GITHUB), headers, payload)

      assertThatResponse()
        .isAccepted()
        .hasJsonMessage("No matching materials!")
    }

    @Test
    void 'should handle push request'() {
      def headers = [
        "X-Hub-Signature": "sha1=c699381b869f24e74db3ee95609c52ee1aad1a48",
        "X-GitHub-Event" : "push"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("webhook-secret")
      Mockito.when(materialUpdateService.updateGitMaterial(eq("release"), anyCollection())).thenReturn(true)

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.GITHUB), headers, payload)

      assertThatResponse()
        .isAccepted()
        .hasJsonMessage("OK!")
    }
  }

  @Nested
  class GitLab {
    def payload = [
      "ref"    : "refs/heads/release",
      "project": [
        "path_with_namespace": "gocd/spaceship",
        "http_url"           : "https://github.com/gocd/spaceship"
      ]
    ]

    @Test
    void 'should error out if event type is not acceptable'() {
      def headers = [
        "X-Gitlab-Token": "",
        "X-Gitlab-Event": "Foo"
      ]

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.GITLAB), headers, payload)

      assertThatResponse().isBadRequest()
        .hasJsonMessage("Invalid event type `Foo`. Allowed events are [Push Hook].")
    }

    @Test
    void 'should error out signature does not match'() {
      def headers = [
        "X-Gitlab-Token": "foobar",
        "X-Gitlab-Event": "Push Hook"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("webhook-secret")

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.GITLAB), headers, payload)

      assertThatResponse().isBadRequest()
        .hasJsonMessage("Token specified in the 'X-Gitlab-Token' header did not match!")
    }

    @Test
    void 'should error out when material is not configured in GoCD'() {
      def headers = [
        "X-Gitlab-Token": "c699381b869f24e74db3ee95609c52ee1aad1a48",
        "X-Gitlab-Event": "Push Hook"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("c699381b869f24e74db3ee95609c52ee1aad1a48")
      Mockito.when(materialUpdateService.updateGitMaterial(eq("release"), anyCollection())).thenReturn(false)

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.GITLAB), headers, payload)

      assertThatResponse()
        .isAccepted()
        .hasJsonMessage("No matching materials!")
    }

    @Test
    void 'should handle push request'() {
      def headers = [
        "X-Gitlab-Token": "c699381b869f24e74db3ee95609c52ee1aad1a48",
        "X-Gitlab-Event": "Push Hook"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("c699381b869f24e74db3ee95609c52ee1aad1a48")
      Mockito.when(materialUpdateService.updateGitMaterial(eq("release"), anyCollection())).thenReturn(true)

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.GITLAB), headers, payload)

      assertThatResponse()
        .isAccepted()
        .hasJsonMessage("OK!")
    }
  }

  @Nested
  class BitBucket {
    def payload = [
      "push"      : [
        "changes": [["new": ["name": "release", "type": "branch"]]]
      ],
      "repository": [
        "scm"  : "git", "full_name": "gocd/spaceship",
        "links": [
          "html": ["href": "https://bitbucket.org/gocd/spaceship"]
        ]
      ]
    ]


    @Test
    void 'should error out if event type is not acceptable'() {
      def headers = [
        "Authorization": "",
        "X-Event-Key"  : "Foo"
      ]

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.BIT_BUCKET_CLOUD), headers, payload)

      assertThatResponse().isBadRequest()
        .hasJsonMessage("Invalid event type `Foo`. Allowed events are [repo:push].")
    }

    @Test
    void 'should error out when signature does not match'() {
      def headers = [
        "Authorization": "Basic ${encodeToBase64("c699381b869f24e74db3ee95609c52ee1aad1a48")}",
        "X-Event-Key"  : "repo:push"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("webhook-secret")

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.BIT_BUCKET_CLOUD), headers, payload)

      assertThatResponse().isBadRequest()
        .hasJsonMessage("Token specified via basic authentication did not match!")
    }

    @Test
    void 'should error out when material is not configured in GoCD'() {
      def headers = [
        "Authorization": "Basic ${encodeToBase64("c699381b869f24e74db3ee95609c52ee1aad1a48")}",
        "X-Event-Key"  : "repo:push"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("c699381b869f24e74db3ee95609c52ee1aad1a48")
      Mockito.when(materialUpdateService.updateGitMaterial(eq("release"), anyCollection())).thenReturn(false)

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.BIT_BUCKET_CLOUD), headers, payload)

      assertThatResponse()
        .isAccepted()
        .hasJsonMessage("No matching materials!")
    }

    @Test
    void 'should handle push request'() {
      def headers = [
        "Authorization": "Basic ${encodeToBase64("c699381b869f24e74db3ee95609c52ee1aad1a48")}",
        "X-Event-Key"  : "repo:push"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("c699381b869f24e74db3ee95609c52ee1aad1a48")
      Mockito.when(materialUpdateService.updateGitMaterial(eq("release"), anyCollection())).thenReturn(true)

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.BIT_BUCKET_CLOUD), headers, payload)

      assertThatResponse()
        .isAccepted()
        .hasJsonMessage("OK!")
    }
  }

  @Nested
  class BitBucketServer {
    def payload = [
      "changes"   : [["ref": ["displayId": "release", "id": "refs/heads/release", "type": "BRANCH"]]],
      "eventKey"  : "repo:refs_changed",
      "repository": [
        "links": [
          "clone": [["href": "ssh://git@bitbucket-server/gocd/spaceship.git", "name": "ssh"],
                    ["href": "http://user:pass@bitbucket-server/scm/gocd/spaceship.git", "name": "http"]]
        ],
        "scmId": "git",
        "name" : "Foo"
      ]
    ]

    @Test
    void 'should error out if event type is not acceptable'() {
      def headers = [
        "Authorization": "",
        "X-Event-Key"  : "Foo"
      ]

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.BIT_BUCKET_SERVER), headers, payload)

      assertThatResponse().isBadRequest()
        .hasJsonMessage("Invalid event type `Foo`. Allowed events are [repo:refs_changed, diagnostics:ping].")
    }

    @Test
    void 'should error out when signature does not match'() {
      def headers = [
        "X-Hub-Signature": "sha256=invalid-signature",
        "X-Event-Key"    : "repo:refs_changed"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("webhook-secret")

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.BIT_BUCKET_SERVER), headers, payload)

      assertThatResponse().isBadRequest()
        .hasJsonMessage("HMAC signature specified via 'X-Hub-Signature' did not match!")
    }

    @Test
    void 'should error out when material is not configured in GoCD'() {
      def headers = [
        "X-Hub-Signature": "sha256=5b421defd19aacb4772fa869beb40ad1518b76774c61e85d70552e2ec9169204",
        "X-Event-Key"    : "repo:refs_changed"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("webhook-secret")
      Mockito.when(materialUpdateService.updateGitMaterial(eq("release"), anyCollection())).thenReturn(false)

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.BIT_BUCKET_SERVER), headers, payload)

      assertThatResponse()
        .isAccepted()
        .hasJsonMessage("No matching materials!")
    }

    @Test
    void 'should handle ref changed request'() {
      def headers = [
        "X-Hub-Signature": "sha256=5b421defd19aacb4772fa869beb40ad1518b76774c61e85d70552e2ec9169204",
        "X-Event-Key"    : "repo:refs_changed"
      ]
      Mockito.when(serverConfigService.getWebhookSecret()).thenReturn("webhook-secret")
      Mockito.when(materialUpdateService.updateGitMaterial(eq("release"), anyCollection())).thenReturn(true)

      postWithApiHeader(controller.controllerPath(Routes.Webhook.Push.BIT_BUCKET_SERVER), headers, payload)

      assertThatResponse()
        .isAccepted()
        .hasJsonMessage("OK!")
    }
  }

  static def encodeToBase64(String token) {
    return Base64.encoder.encodeToString(token.getBytes())
  }
}
