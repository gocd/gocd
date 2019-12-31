/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv3.configrepos

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.config.*
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig
import com.thoughtworks.go.config.policy.Allow
import com.thoughtworks.go.config.policy.Policy
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.ConfigReposConfig
import com.thoughtworks.go.config.remote.PartialConfig
import com.thoughtworks.go.domain.materials.Modification
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.materials.MaterialUpdateService
import com.thoughtworks.go.server.service.ConfigRepoService
import com.thoughtworks.go.server.service.MaterialConfigConverter
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.helper.MaterialConfigsMother.hg
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class ConfigReposInternalControllerV3Test implements SecurityServiceTrait, ControllerTrait<ConfigReposInternalControllerV3> {
  private static final String TEST_PLUGIN_ID = "test.configrepo.plugin"
  private static final String TEST_REPO_URL = "https://fakeurl.com"
  private static final String ID_1 = "repo-01"
  private static final String ID_2 = "repo-02"

  @Mock
  ConfigRepoService service

  @Mock
  GoRepoConfigDataSource dataSource

  @Mock
  MaterialUpdateService materialUpdateService

  @Mock
  MaterialConfigConverter converter

  @BeforeEach
  void setUp() {
    initMocks(this)
    Policy directives = new Policy()
    directives.add(new Allow("administer", "config_repo", "repo-*"))
    RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)

    when(goConfigService.rolesForUser(any())).then({ InvocationOnMock invocation ->
      CaseInsensitiveString username = invocation.getArguments()[0]
      if (username == Username.ANONYMOUS.username) {
        return []
      }
      return [roleConfig]
    })
  }

  @Override
  ConfigReposInternalControllerV3 createControllerInstance() {
    new ConfigReposInternalControllerV3(new ApiAuthenticationHelper(securityService, goConfigService), service, dataSource, materialUpdateService, converter)
  }

  @Nested
  class IndexSecurity implements SecurityTestTrait, NormalUserSecurity {
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
  class Index {

    @BeforeEach
    void setup() {
      loginAsUser()
    }

    @Test
    void 'should list only those existing config repos, with associated parse results, for which the user has permission'() {
      Modification modification = new Modification()
      modification.setRevision("abc")

      PartialConfig partialConfig = new PartialConfig()
      PartialConfigParseResult result = PartialConfigParseResult.parseSuccess(modification, partialConfig);

      ConfigReposConfig repos = new ConfigReposConfig(repo(ID_1), repo(ID_2), repo("test-id"))
      when(service.getConfigRepos()).thenReturn(repos)
      when(dataSource.getLastParseResult(repos.get(0).getMaterialConfig())).thenReturn(null)
      when(dataSource.getLastParseResult(repos.get(1).getMaterialConfig())).thenReturn(result)

      getWithApiHeader(controller.controllerBasePath())

      assertThatResponse()
        .isOk()
        .hasJsonBody([
          _links   : [
            self: [href: "http://test.host/go$Routes.ConfigRepos.BASE".toString()]
          ],
          _embedded: [
            config_repos: [
              expectedRepoJson(ID_1, null, null, false),
              expectedRepoJson(ID_2, "abc", null, false)
            ]
          ]
        ])
    }

    @Test
    void 'should return empty list if the user does not have permission to view any config repos'() {
      ConfigReposConfig repos = new ConfigReposConfig(repo("test-id"), repo("another-id"))
      when(service.getConfigRepos()).thenReturn(repos)

      getWithApiHeader(controller.controllerBasePath())

      assertThatResponse()
        .isOk()
        .hasEtag("\"${new ConfigReposConfig().etag()}\"")
        .hasJsonBody([
          _links   : [
            self: [href: "http://test.host/go$Routes.ConfigRepos.BASE".toString()]
          ],
          _embedded: [
            config_repos: []
          ]
        ])
    }
  }

  static Map expectedRepoJson(String id, String revision, String error, boolean isInProgress) {
    return [
      _links                     : [
        self: [href: "http://test.host/go${Routes.ConfigRepos.id(id)}".toString()],
        doc : [href: Routes.ConfigRepos.DOC],
        find: [href: "http://test.host/go${Routes.ConfigRepos.find()}".toString()],
      ],

      id                         : id,
      plugin_id                  : TEST_PLUGIN_ID,
      material                   : [
        type      : "hg",
        attributes: [
          name       : null,
          url        : "${TEST_REPO_URL}/$id".toString(),
          auto_update: true
        ]
      ],
      configuration              : [],
      can_administer             : true,
      material_update_in_progress: isInProgress,
      parse_info                 : null == revision ? [:] : [
        error                     : error,
        good_modification         : [
          "username"     : null,
          "email_address": null,
          "revision"     : revision,
          "comment"      : null,
          "modified_time": null
        ],
        latest_parsed_modification: [
          "username"     : null,
          "email_address": null,
          "revision"     : revision,
          "comment"      : null,
          "modified_time": null
        ]
      ]
    ]
  }

  static ConfigRepoConfig repo(String id) {
    HgMaterialConfig materialConfig = hg("${TEST_REPO_URL}/$id", "")
    ConfigRepoConfig repo = new ConfigRepoConfig(materialConfig, TEST_PLUGIN_ID, id)

    return repo
  }
}
