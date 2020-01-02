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
package com.thoughtworks.go.apiv1.configrepooperations

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.config.*
import com.thoughtworks.go.config.exceptions.EntityType
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException
import com.thoughtworks.go.config.exceptions.RecordNotFoundException
import com.thoughtworks.go.config.remote.EphemeralConfigOrigin
import com.thoughtworks.go.config.remote.PartialConfig
import com.thoughtworks.go.plugin.access.configrepo.InvalidPartialConfigException
import com.thoughtworks.go.server.service.ConfigRepoService
import com.thoughtworks.go.server.service.GoConfigService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.spark.Routes.ConfigRepos.PREFLIGHT_PATH
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class ConfigRepoOperationsControllerV1Test implements SecurityServiceTrait, ControllerTrait<ConfigRepoOperationsControllerV1> {
  private static final String PLUGIN_ID = "test.plugin"
  private static final String REPO_ID = "test-repo"

  @Mock
  GoConfigPluginService pluginService

  @Mock
  ConfigRepoService configRepoService

  @Mock
  GoConfigService goConfigService

  @Mock
  GoPartialConfig partialConfigService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  ConfigRepoOperationsControllerV1 createControllerInstance() {
    return new ConfigRepoOperationsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), pluginService, configRepoService, goConfigService, partialConfigService)
  }

  @Nested
  class Preflight {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "preflight"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(PREFLIGHT_PATH), [:])
      }
    }

    @Nested
    class Request {

      @BeforeEach
      void setUp() {
        mockMultipartContent("files[]", "foo.config", "content")
        loginAsAdmin()
      }

      @Test
      void "requires pluginId parameter"() {
        postWithApiHeader(controller.controllerPath(PREFLIGHT_PATH), [:])

        assertThatResponse().
          isBadRequest().
          hasJsonMessage("Request is missing parameter `pluginId`")
      }

      @Test
      void "returns NOT FOUND when plugin does not exist"() {
        def plugin = mock(ConfigRepoPlugin.class)
        when(plugin.parseContent(any() as Map<String, String>, any() as PartialConfigLoadContext)).thenThrow(new RecordNotFoundException("Not found"))
        when(pluginService.partialConfigProviderFor(PLUGIN_ID)).thenReturn(plugin)
        postWithApiHeader(controller.controllerPath("$PREFLIGHT_PATH?pluginId=$PLUGIN_ID"), [:])

        assertThatResponse().
          isNotFound().
          hasJsonMessage("Not found")
      }

      @Test
      void "returns NOT FOUND when repo does not exist"() {
        when(configRepoService.getConfigRepo(REPO_ID)).thenReturn(null)
        postWithApiHeader(controller.controllerPath("$PREFLIGHT_PATH?pluginId=$PLUGIN_ID&repoId=$REPO_ID"), [:])

        assertThatResponse().
          isNotFound().
          hasJsonMessage(EntityType.ConfigRepo.notFoundMessage(REPO_ID))
      }

      @Test
      void "sets ad-hoc config origin on resultant partial config"() {
        def plugin = mock(ConfigRepoPlugin.class)
        def partialConfig = mock(PartialConfig.class)

        when(plugin.parseContent(any() as Map<String, String>, any() as PartialConfigLoadContext)).thenReturn(partialConfig)
        when(pluginService.partialConfigProviderFor(PLUGIN_ID)).thenReturn(plugin)

        postWithApiHeader(controller.controllerPath("$PREFLIGHT_PATH?pluginId=$PLUGIN_ID"), [:])

        verify(partialConfig).setOrigins(any(EphemeralConfigOrigin))
      }

      @Test
      void "returns serialized PreflightResult for valid config"() {
        def plugin = mock(ConfigRepoPlugin.class)
        def partialConfig = mock(PartialConfig.class)
        def cruiseConfig = mock(CruiseConfig.class)
        when(plugin.parseContent(any() as Map<String, String>, any() as PartialConfigLoadContext)).thenReturn(partialConfig)
        when(pluginService.partialConfigProviderFor(PLUGIN_ID)).thenReturn(plugin)
        when(partialConfigService.merge(eq(partialConfig), any() as String, any() as CruiseConfig)).thenReturn(cruiseConfig)

        postWithApiHeader(controller.controllerPath("$PREFLIGHT_PATH?pluginId=$PLUGIN_ID"), [:])

        verify(goConfigService, times(1)).validateCruiseConfig(cruiseConfig)
        assertThatResponse().hasJsonBody([
          errors: [],
          valid : true
        ])
      }

      @Test
      void "returns serialized PreflightResult when config fails to parse"() {
        def plugin = mock(ConfigRepoPlugin.class)
        def partialConfig = mock(PartialConfig.class)
        when(plugin.parseContent(any() as Map<String, String>, any() as PartialConfigLoadContext)).thenThrow(new InvalidPartialConfigException(partialConfig, "bad content!"))
        when(pluginService.partialConfigProviderFor(PLUGIN_ID)).thenReturn(plugin)

        postWithApiHeader(controller.controllerPath("$PREFLIGHT_PATH?pluginId=$PLUGIN_ID"), [:])

        verify(partialConfigService, never()).merge(any() as PartialConfig, any() as String, any() as CruiseConfig)
        verify(goConfigService, never()).validateCruiseConfig(any() as CruiseConfig)

        assertThatResponse().hasJsonBody([
          errors: ["bad content!"],
          valid : false
        ])
      }

      @Test
      void "returns serialized PreflightResult when config fails validations"() {
        def plugin = mock(ConfigRepoPlugin.class)
        def partialConfig = mock(PartialConfig.class)
        def cruiseConfig = mock(CruiseConfig.class)
        when(plugin.parseContent(any() as Map<String, String>, any() as PartialConfigLoadContext)).thenReturn(partialConfig)
        when(pluginService.partialConfigProviderFor(PLUGIN_ID)).thenReturn(plugin)
        when(partialConfigService.merge(eq(partialConfig), any() as String, any() as CruiseConfig)).thenReturn(cruiseConfig)
        when(goConfigService.validateCruiseConfig(cruiseConfig)).thenThrow(new GoConfigInvalidException(cruiseConfig, "nope!"))

        postWithApiHeader(controller.controllerPath("$PREFLIGHT_PATH?pluginId=$PLUGIN_ID"), [:])

        verify(goConfigService, times(1)).validateCruiseConfig(cruiseConfig)
        assertThatResponse().hasJsonBody([
          errors: ["nope!"],
          valid : false
        ])
      }
    }
  }
}
