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
package com.thoughtworks.go.apiv3.configrepos

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.config.*
import com.thoughtworks.go.config.materials.PasswordDeserializer
import com.thoughtworks.go.config.policy.Allow
import com.thoughtworks.go.config.policy.Deny
import com.thoughtworks.go.config.policy.Policy
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.ConfigReposConfig
import com.thoughtworks.go.config.remote.PartialConfig
import com.thoughtworks.go.domain.PipelineGroups
import com.thoughtworks.go.domain.materials.Material
import com.thoughtworks.go.domain.materials.MaterialConfig
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.materials.MaterialUpdateService
import com.thoughtworks.go.server.service.ConfigRepoService
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.MaterialConfigConverter
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

import java.util.stream.Collectors

import static com.thoughtworks.go.helper.MaterialConfigsMother.hg
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks
import static org.mockito.internal.verification.VerificationModeFactory.times

class ConfigReposControllerV3Test implements SecurityServiceTrait, ControllerTrait<ConfigReposControllerV3>, DeprecatedApiTrait {
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

  @Mock
  private MaterialUpdateService materialUpdateService

  @Mock
  private MaterialConfigConverter converter

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  ConfigReposControllerV3 createControllerInstance() {
    new ConfigReposControllerV3(new ApiAuthenticationHelper(securityService, goConfigService), service, entityHashingService, materialUpdateService, converter)
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

    @Nested
    class Definitions implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "definedConfigs"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(ID_1, "definitions"), [:])
      }
    }

    @Nested
    class Trigger implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "triggerUpdate"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(ID_1, 'trigger_update'), [:])
      }
    }

    @Nested
    class Status implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "inProgress"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(ID_1, 'status'), [:])
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
          configuration: [],
          rules        : []
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

  @Nested
  class Definitions {
    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Test
    void 'serializes partial config'() {
      Policy directives = new Policy()
      directives.add(new Allow("administer", "config_repo", ID_1))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)

      when(goConfigService.rolesForUser(any())).thenReturn([roleConfig])

      ConfigRepoConfig config = mock(ConfigRepoConfig.class)
      EnvironmentsConfig envs = new EnvironmentsConfig()
      envs.addPipelinesToEnvironment("env-1", "pip-1", "pip-2")
      envs.addPipelinesToEnvironment("env-2", "pip-3", "pip-4")
      PipelineGroups groups = new PipelineGroups()
      groups.add(groupWithPipelines("group-1", "pip-5", "pip-6"))
      groups.add(groupWithPipelines("group-2", "pip-7", "pip-8"))

      when(service.getConfigRepo(ID_1)).thenReturn(config)
      when(service.partialConfigDefinedBy(config)).thenReturn(new PartialConfig(envs, groups))

      getWithApiHeader(controller.controllerPath(ID_1, "definitions"), [:])

      assertThatResponse().
          isOk().
          hasJsonBody([
              environments: [[name: "env-1"], [name: "env-2"]],
              groups      : [
                  [
                      name     : "group-1",
                      pipelines: [[name: "pip-5"], [name: "pip-6"]]
                  ],
                  [
                      name     : "group-2",
                      pipelines: [[name: "pip-7"], [name: "pip-8"]]
                  ]
              ]
          ])
    }

    @Test
    void 'should throw 403 if user does not have access to the specified config repo'() {
      getWithApiHeader(controller.controllerPath('test-id', 'definitions'), [:])

      assertThatResponse()
          .isForbidden()
          .hasJsonMessage("User '${currentUsername().getDisplayName()}' does not have permissions to view 'test-id' config_repo(s).")
    }

    private PipelineConfigs groupWithPipelines(String groupName, String... pipelineNames) {
      PipelineConfig[] pipelines = Arrays.stream(pipelineNames).map({ String s ->
        PipelineConfig pipelineConfig = mock(PipelineConfig.class)
        when(pipelineConfig.name()).thenReturn(new CaseInsensitiveString(s))
        pipelineConfig
      }).collect(Collectors.toList()).toArray([] as PipelineConfig[])

      return new BasicPipelineConfigs(groupName, null, pipelines)
    }
  }

  @Nested
  class Status {
    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Test
    void 'should confirm if update is in progress'() {
      Policy directives = new Policy()
      directives.add(new Allow("administer", "config_repo", "repo-*"))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)
      MaterialConfig config = mock(MaterialConfig.class)
      Material material = mock(Material.class)

      when(goConfigService.rolesForUser(any())).thenReturn([roleConfig])
      when(service.getConfigRepo(ID_1)).thenReturn(ConfigRepoConfig.createConfigRepoConfig(config, null, ID_1))
      when(converter.toMaterial(config)).thenReturn(material)
      when(materialUpdateService.isInProgress(material)).thenReturn(true)

      getWithApiHeader(controller.controllerPath(ID_1, 'status'), [:])
      verify(materialUpdateService).isInProgress(material)

      assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody([in_progress: true])
    }

    @Test
    void 'should return false if update is not in progress'() {
      Policy directives = new Policy()
      directives.add(new Allow("administer", "config_repo", "repo-*"))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)
      MaterialConfig config = mock(MaterialConfig.class)
      Material material = mock(Material.class)

      when(goConfigService.rolesForUser(any())).thenReturn([roleConfig])
      when(service.getConfigRepo(ID_1)).thenReturn(ConfigRepoConfig.createConfigRepoConfig(config, null, ID_1))
      when(converter.toMaterial(config)).thenReturn(material)
      when(materialUpdateService.isInProgress(material)).thenReturn(false)

      getWithApiHeader(controller.controllerPath(ID_1, 'status'), [:])
      verify(materialUpdateService).isInProgress(material)

      assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody([in_progress: false])
    }

    @Test
    void 'should throw 403 if user does not have access to the specified config repo'() {
      getWithApiHeader(controller.controllerPath('test-id', 'status'), [:])

      assertThatResponse()
          .isForbidden()
          .hasJsonMessage("User '${currentUsername().getDisplayName()}' does not have permissions to view 'test-id' config_repo(s).")
    }
  }

  @Nested
  class Trigger {
    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Test
    void 'should trigger material update for repo'() {
      Policy directives = new Policy()
      directives.add(new Allow("administer", "config_repo", "repo-*"))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)
      MaterialConfig config = mock(MaterialConfig.class)
      Material material = mock(Material.class)

      when(goConfigService.rolesForUser(any())).thenReturn([roleConfig])
      when(service.getConfigRepo(ID_1)).thenReturn(ConfigRepoConfig.createConfigRepoConfig(config, null, ID_1))
      when(converter.toMaterial(config)).thenReturn(material)
      when(materialUpdateService.updateMaterial(material)).thenReturn(true)

      postWithApiHeader(controller.controllerPath(ID_1, 'trigger_update'), [:])
      verify(materialUpdateService).updateMaterial(material)

      assertThatResponse()
          .isCreated()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("OK")
    }

    @Test
    void 'should not trigger update if update is already in progress'() {
      Policy directives = new Policy()
      directives.add(new Allow("administer", "config_repo", "repo-*"))
      RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)
      MaterialConfig config = mock(MaterialConfig.class)
      Material material = mock(Material.class)

      when(goConfigService.rolesForUser(any())).thenReturn([roleConfig])
      when(service.getConfigRepo(ID_1)).thenReturn(ConfigRepoConfig.createConfigRepoConfig(config, null, ID_1))
      when(converter.toMaterial(config)).thenReturn(material)
      when(materialUpdateService.updateMaterial(material)).thenReturn(false)

      postWithApiHeader(controller.controllerPath(ID_1, 'trigger_update'), [:])
      verify(materialUpdateService).updateMaterial(material)

      assertThatResponse()
          .isConflict()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Update already in progress.")
    }

    @Test
    void 'should throw 403 if user does not have access to the specified config repo'() {
      postWithApiHeader(controller.controllerPath('test-id', 'trigger_update'), [:])

      assertThatResponse()
          .isForbidden()
          .hasJsonMessage("User '${currentUsername().getDisplayName()}' does not have permissions to administer 'test-id' config_repo(s).")
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
        configuration: [],
        rules        : []
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
