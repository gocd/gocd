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

package com.thoughtworks.go.apiv1.configrepooperations

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.api.util.HaltApiMessages
import com.thoughtworks.go.config.GoRepoConfigDataSource
import com.thoughtworks.go.config.PartialConfigParseResult
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.PartialConfig
import com.thoughtworks.go.domain.materials.Material
import com.thoughtworks.go.domain.materials.MaterialConfig
import com.thoughtworks.go.server.materials.MaterialUpdateService
import com.thoughtworks.go.server.service.ConfigRepoService
import com.thoughtworks.go.server.service.MaterialConfigConverter
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class ConfigRepoOperationsControllerV1Test implements SecurityServiceTrait, ControllerTrait<ConfigRepoOperationsControllerV1> {
  @Mock
  GoRepoConfigDataSource dataSource
  @Mock
  ConfigRepoService service
  @Mock
  MaterialUpdateService materialUpdateService
  @Mock
  MaterialConfigConverter converter

  private static final ID = "repo1"

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  ConfigRepoOperationsControllerV1 createControllerInstance() {
    return new ConfigRepoOperationsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), dataSource, service, materialUpdateService, converter)
  }

  @Nested
  class Security implements SecurityTestTrait, AdminUserSecurity {
    @Override
    String getControllerMethodUnderTest() {
      return "getLastParseResult"
    }

    @Override
    void makeHttpCall() {
      getWithApiHeader(controller.controllerPath(ID, 'last_parsed_result'), [:])
    }
  }

  @Nested
  class getLastParsedResult {
    @BeforeEach
    void setUp() {
      loginAsAdmin()
    }

    @Test
    void 'should get last parsed result for config repo'() {
      MaterialConfig material = mock(MaterialConfig.class)

      when(service.getConfigRepo(ID)).thenReturn(new ConfigRepoConfig(material, null, ID))
      when(dataSource.getLastParseResult(material)).thenReturn(new PartialConfigParseResult("1234", new PartialConfig()))

      getWithApiHeader(controller.controllerPath(ID, 'last_parsed_result'), [:])

      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasBodyWithJson("{\"revision\":\"1234\",\"success\":true}")
    }

    @Test
    void 'should see any errors from last parsed result'() {
      MaterialConfig material = mock(MaterialConfig.class)

      when(service.getConfigRepo(ID)).thenReturn(new ConfigRepoConfig(material, null, ID))
      when(dataSource.getLastParseResult(material)).thenReturn(new PartialConfigParseResult("1234", new RuntimeException("Boom!")))

      getWithApiHeader(controller.controllerPath(ID, 'last_parsed_result'), [:])

      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasBodyWithJson("{\"revision\":\"1234\",\"success\":false,\"error\":\"Boom!\"}")
    }

    @Test
    void 'should handle materials that have never been seen'() {
      MaterialConfig material = mock(MaterialConfig.class)

      when(service.getConfigRepo(ID)).thenReturn(new ConfigRepoConfig(material, null, ID))
      when(dataSource.getLastParseResult(material)).thenReturn(null)

      getWithApiHeader(controller.controllerPath(ID, 'last_parsed_result'), [:])

      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasBodyWithJson("{\"revision\":null}")
    }

    @Test
    void 'should handle non-existent config repo'() {
      when(service.getConfigRepo(ID)).thenReturn(null)

      getWithApiHeader(controller.controllerPath(ID, 'last_parsed_result'), [:])

      assertThatResponse()
        .isNotFound()
        .hasContentType(controller.mimeType)
        .hasJsonMessage(HaltApiMessages.notFoundMessage())
    }
  }

  @Nested
  class status {
    @BeforeEach
    void setUp() {
      loginAsAdmin()
    }

    @Test
    void 'should confirm if update is in progress'() {
      MaterialConfig config = mock(MaterialConfig.class)
      Material material = mock(Material.class)

      when(service.getConfigRepo(ID)).thenReturn(new ConfigRepoConfig(config, null, ID))
      when(converter.toMaterial(config)).thenReturn(material)
      when(materialUpdateService.isInProgress(material)).thenReturn(true)

      getWithApiHeader(controller.controllerPath(ID, 'status'), [:])
      verify(materialUpdateService).isInProgress(material)

      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasBodyWithJson("{\"inProgress\": true}")
    }

    @Test
    void 'should return false if update is not in progress'() {
      MaterialConfig config = mock(MaterialConfig.class)
      Material material = mock(Material.class)

      when(service.getConfigRepo(ID)).thenReturn(new ConfigRepoConfig(config, null, ID))
      when(converter.toMaterial(config)).thenReturn(material)
      when(materialUpdateService.isInProgress(material)).thenReturn(false)

      getWithApiHeader(controller.controllerPath(ID, 'status'), [:])
      verify(materialUpdateService).isInProgress(material)

      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasBodyWithJson("{\"inProgress\": false}")
    }
  }

  @Nested
  class triggerUpdate {
    @BeforeEach
    void setUp() {
      loginAsAdmin()
    }

    @Test
    void 'should trigger material update for repo'() {
      MaterialConfig config = mock(MaterialConfig.class)
      Material material = mock(Material.class)

      when(service.getConfigRepo(ID)).thenReturn(new ConfigRepoConfig(config, null, ID))
      when(converter.toMaterial(config)).thenReturn(material)
      when(materialUpdateService.updateMaterial(material)).thenReturn(true)

      postWithApiHeader(controller.controllerPath(ID, 'trigger_update'), [:])
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

      when(service.getConfigRepo(ID)).thenReturn(new ConfigRepoConfig(config, null, ID))
      when(converter.toMaterial(config)).thenReturn(material)
      when(materialUpdateService.updateMaterial(material)).thenReturn(false)

      postWithApiHeader(controller.controllerPath(ID, 'trigger_update'), [:])
      verify(materialUpdateService).updateMaterial(material)

      assertThatResponse()
        .isConflict()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Update already in progress")
    }
  }
}
