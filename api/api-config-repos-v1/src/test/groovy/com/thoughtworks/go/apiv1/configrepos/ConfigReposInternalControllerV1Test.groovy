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
import com.thoughtworks.go.config.GoRepoConfigDataSource
import com.thoughtworks.go.config.PartialConfigParseResult
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.ConfigReposConfig
import com.thoughtworks.go.config.remote.PartialConfig
import com.thoughtworks.go.domain.materials.MaterialConfig
import com.thoughtworks.go.server.service.ConfigRepoService
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

class ConfigReposInternalControllerV1Test implements SecurityServiceTrait, ControllerTrait<ConfigReposInternalControllerV1> {
  private static final String TEST_PLUGIN_ID = "test.configrepo.plugin"
  private static final String TEST_REPO_URL = "https://fakeurl.com"
  private static final String ID_1 = "repo-01"
  private static final String ID_2 = "repo-02"

  @Mock
  ConfigRepoService service

  @Mock
  GoRepoConfigDataSource dataSource

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  ConfigReposInternalControllerV1 createControllerInstance() {
    new ConfigReposInternalControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), service, dataSource)
  }

  @Nested
  class IndexSecurity implements SecurityTestTrait, AdminUserSecurity {
    @Override
    String getControllerMethodUnderTest() {
      return "listRepos"
    }

    @Override
    void makeHttpCall() {
      getWithApiHeader(controller.controllerBasePath(), [:])
    }
  }

  @Nested
  class GetSecurity implements SecurityTestTrait, AdminUserSecurity {
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
    void 'should list existing config repos with associated parse results'() {
      ConfigReposConfig repos = new ConfigReposConfig(repo(ID_1), repo(ID_2))
      when(service.getConfigRepos()).thenReturn(repos)
      when(dataSource.getLastParseResult(repos.get(0).getMaterialConfig())).thenReturn(null)
      when(dataSource.getLastParseResult(repos.get(1).getMaterialConfig())).thenReturn(new PartialConfigParseResult("abc", new PartialConfig()))

      getWithApiHeader(controller.controllerBasePath())

      assertThatResponse().
        isOk().
        hasJsonBody([
          _links   : [
            self: [href: "http://test.host/go$Routes.ConfigRepos.BASE".toString()]
          ],
          _embedded: [
            config_repos: [
              expectedRepoJson(ID_1, null, false, null),
              expectedRepoJson(ID_2, "abc", true, null)
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
    void 'should show an individual repo with its parse result'() {
      ConfigRepoConfig repo = repo(ID_1)
      when(service.getConfigRepo(ID_1)).thenReturn(repo)
      when(dataSource.getLastParseResult(repo.getMaterialConfig())).thenReturn(new PartialConfigParseResult("abc", new PartialConfig()))

      getWithApiHeader(controller.controllerPath(ID_1))

      assertThatResponse().
        isOk().
        hasJsonBody(expectedRepoJson(ID_1, "abc", true, null))
    }

    @Test
    void 'should return 404 if the repo does not exist'() {
      getWithApiHeader(controller.controllerPath(ID_1))

      verify(dataSource, never()).getLastParseResult(any() as MaterialConfig)
      assertThatResponse().isNotFound()
    }
  }

  static Map expectedRepoJson(String id, String revision, boolean success, String error) {
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
      configuration: [],

      last_parse   : null == revision ? [:] : [
        revision: revision,
        success : success,
        error   : error
      ]
    ]
  }

  static ConfigRepoConfig repo(String id) {
    HgMaterialConfig materialConfig = new HgMaterialConfig("${TEST_REPO_URL}/$id", "")
    ConfigRepoConfig repo = new ConfigRepoConfig(materialConfig, TEST_PLUGIN_ID, id)

    return repo
  }
}
