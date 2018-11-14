/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.configrepos

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.configrepos.representers.MaterialConfigHelper
import com.thoughtworks.go.config.materials.PasswordDeserializer
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.ConfigReposConfig
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.ConfigRepoService
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks
import static org.mockito.internal.verification.VerificationModeFactory.times

class ConfigReposControllerV1Test implements SecurityServiceTrait, ControllerTrait<ConfigReposControllerV1> {
  private static final String TEST_PLUGIN_ID = "test.configrepo.plugin"
  private static final String TEST_REPO_URL = "https://fakeurl.com"
  private static final String ID_1 = "repo-01"
  private static final String ID_2 = "repo-02"

  @Mock
  private ConfigRepoService service

  @Mock
  private PasswordDeserializer pd

  private MaterialConfigHelper mch

  @Mock
  private EntityHashingService entityHashingService

  @BeforeEach
  void setUp() {
    initMocks(this)
    mch = new MaterialConfigHelper(pd)
  }

  @Override
  ConfigReposControllerV1 createControllerInstance() {
    new ConfigReposControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), service, mch, entityHashingService)
  }

  @Nested
  class Security {
    @Nested
    class Index implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath(), [:])
      }
    }

    @Nested
    class Show implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "showRepo"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(ID_1), [:])
      }
    }

    @Nested
    class Create implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "createRepo"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerBasePath(), [:])
      }
    }

    @Nested
    class Update implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "updateRepo"
      }

      @Override
      void makeHttpCall() {
        putWithApiHeader(controller.controllerPath(ID_1), [:])
      }
    }

    @Nested
    class Delete implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "deleteRepo"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath(ID_1), [:])
      }
    }
  }

  @Nested
  class Index {

    @BeforeEach
    void setup() {
      loginAsAdmin()
    }

    @Test
    void 'should list existing config repos'() {
      ConfigReposConfig repos = new ConfigReposConfig(repo(ID_1), repo(ID_2))
      when(service.getConfigRepos()).thenReturn(repos)

      getWithApiHeader(controller.controllerBasePath())

      assertThatResponse().
        isOk().
        hasHeader('ETag', "\"${repos.etag()}\"").
        hasJsonBody([
          _links   : [
            self: [href: "http://test.host/go$Routes.ConfigRepos.BASE".toString()]
          ],
          _embedded: [
            config_repos: [
              expectedRepoJson(ID_1),
              expectedRepoJson(ID_2)
            ]
          ]
        ])
    }
  }

  @Nested
  class Show {

    @BeforeEach
    void setup() {
      loginAsAdmin()
    }

    @Test
    void 'fetches a repo'() {
      ConfigRepoConfig repo = repo(ID_1)
      when(service.getConfigRepo(ID_1)).thenReturn(repo)
      when(entityHashingService.md5ForEntity(repo)).thenReturn('md5')

      getWithApiHeader(controller.controllerPath(ID_1))

      assertThatResponse().
        isOk().
        hasHeader('ETag', '"md5"'). // test etag does not change
        hasJsonBody(expectedRepoJson(ID_1))
    }

    @Test
    void 'returns 404 if the repo does not exist'() {
      getWithApiHeader(controller.controllerPath(ID_1))

      assertThatResponse().isNotFound()
    }
  }

  @Nested
  class Create {

    @BeforeEach
    void setup() {
      loginAsAdmin()
    }

    @Test
    void 'creates a new config repo'() {
      String id = "test-repo"

      postWithApiHeader(controller.controllerBasePath(), [
        id           : id,
        plugin_id    : TEST_PLUGIN_ID,
        material     : [
          type      : "hg",
          attributes: [
            name       : null,
            url        : "${TEST_REPO_URL}/$id".toString(),
            auto_update: true
          ]
        ],
        configuration: []
      ])

      verify(service, times(1)).
        createConfigRepo(any() as ConfigRepoConfig, any() as Username, any() as HttpLocalizedOperationResult)

      assertThatResponse().
        isOk().
        hasJsonBody(expectedRepoJson(id))
    }

    @Test
    void 'create fails if an existing repo has the same id'() {
      String id = "test-repo"

      when(service.getConfigRepo(id)).thenReturn(repo(id))

      Map payload = [
        id           : id,
        plugin_id    : TEST_PLUGIN_ID,
        material     : [
          type      : "hg",
          attributes: [
            name       : null,
            url        : "${TEST_REPO_URL}/$id".toString(),
            auto_update: true
          ]
        ],
        configuration: []
      ]
      postWithApiHeader(controller.controllerBasePath(), payload)

      verify(service, never()).
        createConfigRepo(any() as ConfigRepoConfig, any() as Username, any() as HttpLocalizedOperationResult)

      assertThatResponse().
        isUnprocessableEntity().
        hasJsonBody([
          message: "Failed to add config-repo 'test-repo'. Another config-repo with the same name already exists.",
          data   : payload
        ])
    }
  }

  @Nested
  class Update {

    @BeforeEach
    void setup() {
      loginAsAdmin()
    }

    @Test
    void 'updates an existing repo'() {
      String id = "test-repo"

      ConfigRepoConfig existing = new ConfigRepoConfig(new HgMaterialConfig("https://fakeurl.com", null), TEST_PLUGIN_ID, id)
      ConfigRepoConfig repoFromRequest = new ConfigRepoConfig(new HgMaterialConfig("https://newfakeurl.com", null), TEST_PLUGIN_ID, id)
      when(service.getConfigRepo(id)).thenReturn(existing)
      when(entityHashingService.md5ForEntity(existing)).thenReturn('md5')
      when(entityHashingService.md5ForEntity(repoFromRequest)).thenReturn('new_md5')

      putWithApiHeader(controller.controllerPath(id), ['If-Match': 'md5'], [
        id           : id,
        plugin_id    : TEST_PLUGIN_ID,
        material     : [
          type      : "hg",
          attributes: [
            name       : null,
            url        : "https://newfakeurl.com",
            auto_update: true
          ]
        ],
        configuration: []
      ])

      verify(service, times(1)).
        updateConfigRepo(eq(id), eq(repoFromRequest), eq('md5'), any() as Username, any() as HttpLocalizedOperationResult)

      assertThatResponse().
        isOk().
        hasHeader('ETag', '"new_md5"'). // test etag should change
        hasJsonBody(expectedRepoJson(id, "https://newfakeurl.com"))
    }

    @Test
    void 'update fails when config repo does not exist'() {
      when(service.getConfigRepo(ID_1)).thenReturn(null)

      putWithApiHeader(controller.controllerPath(ID_1), [:])

      verify(service, never()).updateConfigRepo(any() as String, any() as ConfigRepoConfig, any() as String, any() as Username, any() as HttpLocalizedOperationResult)

      assertThatResponse().
        isNotFound()
    }

    @Test
    void 'update fails on mismatched etag'() {
      String id = "test-repo"

      ConfigRepoConfig existing = repo(id)
      when(service.getConfigRepo(id)).thenReturn(new ConfigRepoConfig(new HgMaterialConfig(TEST_REPO_URL, ""), TEST_PLUGIN_ID, id) {
        @Override
        String etag() {
          return "no-match!"
        }
      })

      Map payload = [
        id           : id,
        plugin_id    : TEST_PLUGIN_ID,
        material     : [
          type      : "hg",
          attributes: [
            name       : null,
            url        : "${TEST_REPO_URL}/$id".toString(),
            auto_update: true
          ]
        ],
        configuration: []
      ]

      putWithApiHeader(controller.controllerPath(id), ['If-Match': existing.etag()], payload)

      verify(service, never()).
        updateConfigRepo(any() as String, any() as ConfigRepoConfig, any() as String, any() as Username, any() as HttpLocalizedOperationResult)


      assertThatResponse().
        isPreconditionFailed().
        hasJsonMessage("Someone has modified the entity. Please update your copy with the changes and try again.")
    }
  }

  @Nested
  class Delete {

    @BeforeEach
    void setup() {
      loginAsAdmin()
    }

    @Test
    void 'deletes and existing repo'() {
      when(service.getConfigRepo(ID_1)).thenReturn(repo(ID_1))

      deleteWithApiHeader(controller.controllerPath(ID_1))

      verify(service, times(1)).deleteConfigRepo(eq(ID_1), any() as Username, any() as HttpLocalizedOperationResult)

      assertThatResponse().
        isOk()
      // Testing the JSON message is meaningless as we are mocking the ConfigRepoService, which is what sets the message
    }

    @Test
    void 'delete fails when config repo does not exist'() {
      when(service.getConfigRepo(ID_1)).thenReturn(null)

      deleteWithApiHeader(controller.controllerPath(ID_1))

      verify(service, never()).deleteConfigRepo(eq(ID_1), any() as Username, any() as HttpLocalizedOperationResult)

      assertThatResponse().
        isNotFound()
    }
  }

  static Map expectedRepoJson(String id, String url="${TEST_REPO_URL}/$id".toString()) {
    return [
      _links       : [
        self: [href: "http://test.host/go${Routes.ConfigRepos.id(id)}".toString()],
        doc : [href: Routes.ConfigRepos.DOC],
        find: [href: "http://test.host/go${Routes.ConfigRepos.find()}".toString()],
      ],

      id           : id,
      plugin_id    : TEST_PLUGIN_ID,
      material     : [
        type      : "hg",
        attributes: [
          name       : null,
          url        : url,
          auto_update: true
        ]
      ],
      configuration: []
    ]
  }

  static ConfigRepoConfig repo(String id) {
    return new ConfigRepoConfig(new HgMaterialConfig("${TEST_REPO_URL}/$id", ""), TEST_PLUGIN_ID, id) {
      @Override
      String etag() {
        return "etag-for-${id}"
      }
    }
  }
}
