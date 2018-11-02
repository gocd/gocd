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

  @BeforeEach
  void setUp() {
    initMocks(this)
    mch = new MaterialConfigHelper(pd)
  }

  @Override
  ConfigReposControllerV1 createControllerInstance() {
    new ConfigReposControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), service, mch)
  }

  @Nested
  class IndexSecurity implements SecurityTestTrait, AdminUserSecurity {
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
  class ShowSecurity implements SecurityTestTrait, AdminUserSecurity {
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
    void 'should show an individual repo '() {
      ConfigRepoConfig repo = repo(ID_1)
      when(service.getConfigRepo(ID_1)).thenReturn(repo)

      getWithApiHeader(controller.controllerPath(ID_1))

      assertThatResponse().
        isOk().
        hasJsonBody(expectedRepoJson(ID_1))
    }

    @Test
    void 'should return 404 if the repo does not exist'() {
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
    void 'should be able to create new config repo'() {
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
    void 'should not be able to create new config repo with the same id as an existing'() {
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
          data: payload
        ])
    }
  }

  static Map expectedRepoJson(String id) {
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
          url        : "${TEST_REPO_URL}/$id".toString(),
          auto_update: true
        ]
      ],
      configuration: []
    ]
  }

  static ConfigRepoConfig repo(String id) {
    HgMaterialConfig materialConfig = new HgMaterialConfig("${TEST_REPO_URL}/$id", "")
    return new ConfigRepoConfig(materialConfig, TEST_PLUGIN_ID, id)
  }
}
