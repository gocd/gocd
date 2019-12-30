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

package com.thoughtworks.go.apiv3.rolesconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv3.rolesconfig.models.RolesViewModel
import com.thoughtworks.go.apiv3.rolesconfig.representers.RolesViewModelRepresenter
import com.thoughtworks.go.config.PluginRoleConfig
import com.thoughtworks.go.config.RoleConfig
import com.thoughtworks.go.config.RolesConfig
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.ConfigReposConfig
import com.thoughtworks.go.server.service.ConfigRepoService
import com.thoughtworks.go.server.service.EnvironmentConfigService
import com.thoughtworks.go.server.service.RoleConfigService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helper.MaterialConfigsMother.git
import static java.util.Arrays.asList
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalRolesControllerV3Test implements SecurityServiceTrait, ControllerTrait<InternalRolesControllerV3> {
  @Mock
  private RoleConfigService roleConfigService
  @Mock
  private EnvironmentConfigService environmentConfigService
  @Mock
  private ConfigRepoService configRepoService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalRolesControllerV3 createControllerInstance() {
    new InternalRolesControllerV3(new ApiAuthenticationHelper(securityService, goConfigService), roleConfigService, environmentConfigService, configRepoService)
  }

  @Nested
  class Index {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath())
      }
    }

    @Nested
    class AsAdminUser {
      private RolesConfig expectedRoles
      private ConfigRepoConfig configRepo
      private List<String> envNames

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

        def gocdRoleConfig = new RoleConfig("gocd-role")
        def pluginRoleConfig = new PluginRoleConfig('foo', 'ldap')
        expectedRoles = new RolesConfig([gocdRoleConfig, pluginRoleConfig])
        configRepo = new ConfigRepoConfig(git("https://foo.git", "master"), "json-config-repo-plugin", "repo-1");
        envNames = asList("env1", "env2")

        when(environmentConfigService.getEnvironmentNames()).thenReturn(envNames)
        when(configRepoService.getConfigRepos()).thenReturn(new ConfigReposConfig(configRepo))
      }

      @Test
      void 'should list all roles along with auto completion values'() {
        def rolesViewModel = new RolesViewModel().setRolesConfig(expectedRoles)
        rolesViewModel.getAutoSuggestions().put("environment", envNames)
        rolesViewModel.getAutoSuggestions().put("config_repo", ["repo-1"])

        when(roleConfigService.getRoles()).thenReturn(expectedRoles)

        getWithApiHeader(controller.controllerPath())

        def expectedJson = toObjectString({ RolesViewModelRepresenter.toJSON(it, rolesViewModel) })

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody(expectedJson)
      }

      @Test
      void 'should list roles with the given type'() {
        def rolesViewModel = new RolesViewModel().setRolesConfig(expectedRoles.ofType("plugin"))
        rolesViewModel.getAutoSuggestions().put("environment", envNames)
        rolesViewModel.getAutoSuggestions().put("config_repo", ["repo-1"])

        when(roleConfigService.getRoles()).thenReturn(expectedRoles)

        getWithApiHeader(controller.controllerPath(type: 'plugin'))

        def expectedJson = toObjectString({ RolesViewModelRepresenter.toJSON(it, rolesViewModel) })

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody(expectedJson)
      }
    }
  }
}
