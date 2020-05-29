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
package com.thoughtworks.go.apiv2.configrepos

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.RoleConfig
import com.thoughtworks.go.config.Users
import com.thoughtworks.go.config.materials.PasswordDeserializer
import com.thoughtworks.go.config.policy.Allow
import com.thoughtworks.go.config.policy.Deny
import com.thoughtworks.go.config.policy.Policy
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.ConfigReposConfig
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.ConfigRepoService
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.DeprecatedApiTrait
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
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks
import static org.mockito.internal.verification.VerificationModeFactory.times

class ConfigReposControllerV2Test implements SecurityServiceTrait, ControllerTrait<ConfigReposControllerV2>, DeprecatedApiTrait {
  private static final String TEST_PLUGIN_ID = "test.configrepo.plugin"
  private static final String TEST_REPO_URL = "https://fakeurl.com"
  private static final String ID_1 = "repo-01"
  private static final String ID_2 = "repo-02"

  @Mock
  private ConfigRepoService service

  @Mock
  private PasswordDeserializer pd

  @Mock
  private EntityHashingService entityHashingService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  ConfigReposControllerV2 createControllerInstance() {
    new ConfigReposControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), service, entityHashingService)
  }

  @Nested
  class Security {
    @BeforeEach
    void setUp() {
      Policy directives = new Policy()
      directives.add(new Allow("administer", "config_repo", "*"))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)

      when(goConfigService.rolesForUser(any())).then({ InvocationOnMock invocation ->
        CaseInsensitiveString username = invocation.getArguments()[0]
        if (username == Username.ANONYMOUS.username) {
          return []
        }
        return [roleConfig]
      })
    }

    @Nested
    class Index implements SecurityTestTrait, NormalUserSecurity {
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
    class Show implements SecurityTestTrait, NormalUserSecurity {
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
    class Create implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "createRepo"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerBasePath(), [id: 'repo-id'])
      }
    }

    @Nested
    class Update implements SecurityTestTrait, NormalUserSecurity {
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
    class Delete implements SecurityTestTrait, NormalUserSecurity {
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
      loginAsUser()
    }

    @Test
    void 'should list existing config repos for which user has permissions'() {
      Policy directives = new Policy()
      directives.add(new Allow("administer", "config_repo", "repo-01"))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)

      when(goConfigService.rolesForUser(any())).thenReturn([roleConfig])

      def repoConfig = repo(ID_1)
      def userSpecificRepos = new ConfigReposConfig(repoConfig)
      ConfigReposConfig repos = new ConfigReposConfig(repoConfig, repo(ID_2))
      when(service.getConfigRepos()).thenReturn(repos)

      getWithApiHeader(controller.controllerBasePath())

      assertThatResponse().
        isOk().
        hasHeader('ETag', "\"${userSpecificRepos.etag()}\"").
        hasJsonBody([
          _links   : [
            self: [href: "http://test.host/go$Routes.ConfigRepos.BASE".toString()]
          ],
          _embedded: [
            config_repos: [
              expectedRepoJson(ID_1)
            ]
          ]
        ])
    }

    @Test
    void 'should return no config repos when user does not have access to any config repo'() {
      Policy directives = new Policy()
      directives.add(new Allow("administer", "config_repo", "blah-*"))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)

      when(goConfigService.rolesForUser(any())).thenReturn([roleConfig])

      ConfigReposConfig repos = new ConfigReposConfig(repo(ID_1), repo(ID_2))
      when(service.getConfigRepos()).thenReturn(repos)

      getWithApiHeader(controller.controllerBasePath())

      assertThatResponse().
        isOk().
        hasHeader('ETag', "\"${new ConfigReposConfig().etag()}\"").
        hasJsonBody([
          _links   : [
            self: [href: "http://test.host/go$Routes.ConfigRepos.BASE".toString()]
          ],
          _embedded: [
            config_repos: []
          ]
        ])
    }
  }

  @Nested
  class Show {

    @BeforeEach
    void setup() {
      loginAsUser()
    }

    @Test
    void 'fetches a repo if the user has permission'() {
      Policy directives = new Policy()
      directives.add(new Allow("administer", "config_repo", "repo-*"))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)

      when(goConfigService.rolesForUser(any())).thenReturn([roleConfig])

      ConfigRepoConfig repo = repo(ID_1)
      when(service.getConfigRepo(ID_1)).thenReturn(repo)
      when(entityHashingService.hashForEntity(repo)).thenReturn('digest')

      getWithApiHeader(controller.controllerPath(ID_1))

      assertThatResponse().
        isOk().
        hasHeader('ETag', '"digest"'). // test etag does not change
        hasJsonBody(expectedRepoJson(ID_1))
    }

    @Test
    void 'returns 403 if the user does not have permission to view it'() {
      Policy directives = new Policy()
      directives.add(new Deny("view", "config_repo", "repo-*"))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)

      when(goConfigService.rolesForUser(any())).thenReturn([roleConfig])

      ConfigRepoConfig repo = repo(ID_1)
      when(service.getConfigRepo(ID_1)).thenReturn(repo)

      getWithApiHeader(controller.controllerPath(ID_1))

      assertThatResponse()
        .isForbidden()
        .hasJsonMessage("User '${currentUsername().getDisplayName()}' does not have permissions to view 'repo-01' config_repo(s).")
    }

    @Test
    void 'returns 404 if the repo does not exist'() {
      Policy directives = new Policy()
      directives.add(new Allow("administer", "config_repo", "repo-*"))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)

      when(goConfigService.rolesForUser(any())).thenReturn([roleConfig])

      getWithApiHeader(controller.controllerPath(ID_1))

      assertThatResponse().isNotFound()
    }
  }

  @Nested
  class Create {

    @BeforeEach
    void setup() {
      loginAsUser()
    }

    @Test
    void 'creates a new config repo if the user has access to create one'() {
      String id = "test-repo"

      Policy directives = new Policy()
      directives.add(new Allow("administer", "config_repo", id))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)

      when(goConfigService.rolesForUser(any())).thenReturn([roleConfig])

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

      Policy directives = new Policy()
      directives.add(new Allow("administer", "config_repo", id))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)

      when(goConfigService.rolesForUser(any())).thenReturn([roleConfig])

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
        errors       : [
          id: ["ConfigRepo ids should be unique. A ConfigRepo with the same id already exists."]
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

    @Test
    void 'should not allow to create a config repo if the user does not have permissions'() {
      String id = "test-repo"

      Policy directives = new Policy()
      directives.add(new Allow("view", "config_repo", id))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)

      when(goConfigService.rolesForUser(any())).thenReturn([roleConfig])

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

      assertThatResponse()
        .isForbidden()
        .hasJsonMessage("User '${currentUsername().getDisplayName()}' does not have permissions to administer 'test-repo' config_repo(s).")
    }
  }

  @Nested
  class Update {

    @BeforeEach
    void setup() {
      loginAsUser()
      Policy directives = new Policy()
      directives.add(new Allow("administer", "config_repo", "*repo*"))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)

      when(goConfigService.rolesForUser(any())).thenReturn([roleConfig])
    }

    @Test
    void 'updates an existing repo'() {
      String id = "test-repo"

      ConfigRepoConfig existing = ConfigRepoConfig.createConfigRepoConfig(hg("https://fakeurl.com", null), TEST_PLUGIN_ID, id)
      ConfigRepoConfig repoFromRequest = ConfigRepoConfig.createConfigRepoConfig(hg("https://newfakeurl.com", null), TEST_PLUGIN_ID, id)
      when(service.getConfigRepo(id)).thenReturn(existing)
      when(entityHashingService.hashForEntity(existing)).thenReturn('digest')
      when(entityHashingService.hashForEntity(repoFromRequest)).thenReturn('new_digest')

      putWithApiHeader(controller.controllerPath(id), ['If-Match': 'digest'], [
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
        updateConfigRepo(eq(id), eq(repoFromRequest), eq('digest'), any() as Username, any() as HttpLocalizedOperationResult)

      assertThatResponse().
        isOk().
        hasHeader('ETag', '"new_digest"'). // test etag should change
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
      when(service.getConfigRepo(id)).thenReturn(existing)
      when(entityHashingService.hashForEntity(existing)).thenReturn("unknown-etag")

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

    @Test
    void 'should throw 403 if the user does not have permission to update config-repo'() {
      String id = "test-id"

      putWithApiHeader(controller.controllerPath(id), ['If-Match': 'digest'], [
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

      assertThatResponse().
        isForbidden().
        hasJsonMessage("User '${currentUsername().getDisplayName()}' does not have permissions to administer 'test-id' config_repo(s).")
    }
  }

  @Nested
  class Delete {

    @BeforeEach
    void setup() {
      loginAsUser()
      Policy directives = new Policy()
      directives.add(new Allow("administer", "config_repo", ID_1))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)

      when(goConfigService.rolesForUser(any())).thenReturn([roleConfig])
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

    @Test
    void 'should throw 403 if the user does not have permission to delete'() {
      deleteWithApiHeader(controller.controllerPath(ID_2))

      assertThatResponse()
        .isForbidden()
        .hasJsonMessage("User '${currentUsername().getDisplayName()}' does not have permissions to administer 'repo-02' config_repo(s).")
    }
  }

  static Map expectedRepoJson(String id, String url = "${TEST_REPO_URL}/$id".toString()) {
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
    def configRepo = new ConfigRepoConfig() {
      @Override
      String etag() {
        return "etag-for-${id}"
      }
    }.setRepo(hg("${TEST_REPO_URL}/$id", ""))
      .setId(id)
      .setPluginId(TEST_PLUGIN_ID)
    return configRepo as ConfigRepoConfig
  }

}
