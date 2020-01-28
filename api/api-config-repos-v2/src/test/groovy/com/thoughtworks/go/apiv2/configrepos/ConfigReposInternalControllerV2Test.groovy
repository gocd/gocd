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
import com.thoughtworks.go.config.*
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig
import com.thoughtworks.go.config.policy.Allow
import com.thoughtworks.go.config.policy.Policy
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.ConfigReposConfig
import com.thoughtworks.go.config.remote.PartialConfig
import com.thoughtworks.go.domain.PipelineGroups
import com.thoughtworks.go.domain.materials.Material
import com.thoughtworks.go.domain.materials.MaterialConfig
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

import java.util.stream.Collectors

import static com.thoughtworks.go.helper.MaterialConfigsMother.hg
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class ConfigReposInternalControllerV2Test implements SecurityServiceTrait, ControllerTrait<ConfigReposInternalControllerV2> {
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
  ConfigReposInternalControllerV2 createControllerInstance() {
    new ConfigReposInternalControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), service, dataSource, materialUpdateService, converter)
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
  class GetSecurity implements SecurityTestTrait, NormalUserSecurity {
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
      when(dataSource.getLastParseResult(repos.get(0).getRepo())).thenReturn(null)
      when(dataSource.getLastParseResult(repos.get(1).getRepo())).thenReturn(result)

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

  @Nested
  class Show {

    @BeforeEach
    void setup() {
      loginAsUser()
    }

    @Test
    void 'should show an individual repo with its parse result'() {
      Modification modification = new Modification()
      modification.setRevision("abc")

      PartialConfig partialConfig = new PartialConfig()
      PartialConfigParseResult result = PartialConfigParseResult.parseSuccess(modification, partialConfig);

      ConfigRepoConfig repo = repo(ID_1)
      when(service.getConfigRepo(ID_1)).thenReturn(repo)
      when(dataSource.getLastParseResult(repo.getRepo())).thenReturn(result)

      getWithApiHeader(controller.controllerPath(ID_1))

      assertThatResponse().
        isOk().
        hasJsonBody(expectedRepoJson(ID_1, "abc", null, false))
    }

    @Test
    void 'should return 404 if the repo does not exist'() {
      getWithApiHeader(controller.controllerPath(ID_1))

      verify(dataSource, never()).getLastParseResult(any() as MaterialConfig)
      assertThatResponse().isNotFound()
    }

    @Test
    void 'should throw 403 if user does not have access to the specified config repo'() {
      getWithApiHeader(controller.controllerPath('test-id'))

      assertThatResponse()
        .isForbidden()
        .hasJsonMessage("User '${currentUsername().getDisplayName()}' does not have permissions to view 'test-id' config_repo(s).")
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
    ConfigRepoConfig repo = ConfigRepoConfig.createConfigRepoConfig(materialConfig, TEST_PLUGIN_ID, id)

    return repo
  }

  @Nested
  class Definitions {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
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
    class Requests {
      @BeforeEach
      void setUp() {
        loginAsUser()
      }

      @Test
      void 'serializes partial config'() {
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
          PipelineConfig p = mock(PipelineConfig.class)
          when(p.name()).thenReturn(new CaseInsensitiveString(s))
          p
        }).collect(Collectors.toList()).toArray([] as PipelineConfig[])

        return new BasicPipelineConfigs(groupName, null, pipelines)
      }
    }
  }

  @Nested
  class Status {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "inProgress"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(ID_1, 'status'), [:])
      }
    }

    @Nested
    class Requests {
      @BeforeEach
      void setUp() {
        loginAsUser()
      }

      @Test
      void 'should confirm if update is in progress'() {
        MaterialConfig config = mock(MaterialConfig.class)
        Material material = mock(Material.class)

        when(service.getConfigRepo(ID_1)).thenReturn(ConfigRepoConfig.createConfigRepoConfig(config, null, ID_1))
        when(converter.toMaterial(config)).thenReturn(material)
        when(materialUpdateService.isInProgress(material)).thenReturn(true)

        getWithApiHeader(controller.controllerPath(ID_1, 'status'), [:])
        verify(materialUpdateService).isInProgress(material)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody([inProgress: true])
      }

      @Test
      void 'should return false if update is not in progress'() {
        MaterialConfig config = mock(MaterialConfig.class)
        Material material = mock(Material.class)

        when(service.getConfigRepo(ID_1)).thenReturn(ConfigRepoConfig.createConfigRepoConfig(config, null, ID_1))
        when(converter.toMaterial(config)).thenReturn(material)
        when(materialUpdateService.isInProgress(material)).thenReturn(false)

        getWithApiHeader(controller.controllerPath(ID_1, 'status'), [:])
        verify(materialUpdateService).isInProgress(material)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody([inProgress: false])
      }

      @Test
      void 'should throw 403 if user does not have access to the specified config repo'() {
        getWithApiHeader(controller.controllerPath('test-id', 'status'), [:])

        assertThatResponse()
          .isForbidden()
          .hasJsonMessage("User '${currentUsername().getDisplayName()}' does not have permissions to view 'test-id' config_repo(s).")
      }
    }
  }

  @Nested
  class Trigger {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
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
    class Requests {
      @BeforeEach
      void setUp() {
        loginAsUser()
      }

      @Test
      void 'should trigger material update for repo'() {
        MaterialConfig config = mock(MaterialConfig.class)
        Material material = mock(Material.class)

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
        MaterialConfig config = mock(MaterialConfig.class)
        Material material = mock(Material.class)

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
  }
}
