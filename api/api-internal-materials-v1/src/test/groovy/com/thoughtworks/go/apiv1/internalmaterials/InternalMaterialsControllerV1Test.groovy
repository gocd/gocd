/*
 * Copyright 2021 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.internalmaterials

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.internalmaterials.models.MaterialInfo
import com.thoughtworks.go.apiv1.internalmaterials.representers.MaterialWithModificationsRepresenter
import com.thoughtworks.go.apiv1.internalmaterials.representers.UsagesRepresenter
import com.thoughtworks.go.config.exceptions.RecordNotFoundException
import com.thoughtworks.go.config.materials.MaterialConfigs
import com.thoughtworks.go.domain.materials.Material
import com.thoughtworks.go.domain.materials.MaterialConfig
import com.thoughtworks.go.helper.MaterialConfigsMother
import com.thoughtworks.go.helper.ModificationsMother
import com.thoughtworks.go.server.materials.MaterialUpdateService
import com.thoughtworks.go.server.service.MaintenanceModeService
import com.thoughtworks.go.server.service.MaterialConfigConverter
import com.thoughtworks.go.server.service.MaterialConfigService
import com.thoughtworks.go.server.service.MaterialService
import com.thoughtworks.go.serverhealth.ServerHealthService
import com.thoughtworks.go.serverhealth.ServerHealthStates
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static java.util.Collections.emptyMap
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class InternalMaterialsControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalMaterialsControllerV1> {
  @Mock
  private MaterialConfigService materialConfigService
  @Mock
  private MaterialService materialService
  @Mock
  private MaintenanceModeService maintenanceModeService
  @Mock
  private MaterialUpdateService materialUpdateService
  @Mock
  private MaterialConfigConverter materialConfigConverter
  @Mock
  private ServerHealthService serverHealthService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalMaterialsControllerV1 createControllerInstance() {
    new InternalMaterialsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), materialConfigService, materialService, maintenanceModeService, materialUpdateService, materialConfigConverter, serverHealthService)
  }

  @Nested
  class Usages {

    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "usages"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath() + "/some-fingerprint/usages")
      }
    }

    @Test
    void 'should return 200 with usages'() {
      def usages = ["pipeline1", "pipeline2"]
      when(materialConfigService.getUsagesForMaterial(anyString(), anyString())).thenReturn(usages)

      getWithApiHeader(controller.controllerBasePath() + "/some-fingerprint/usages")

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(UsagesRepresenter.class, "some-fingerprint", usages)
    }
  }

  @Nested
  class Index {
    def material = mock(Material.class)

    @BeforeEach
    void setUp() {
      loginAsUser()
      when(materialConfigConverter.toMaterial(any(MaterialConfig.class))).thenReturn(material)
      when(serverHealthService.logs()).thenReturn(new ServerHealthStates())
    }

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath())
      }
    }

    @Test
    void 'should return 200 with materials and their info: modifications and mdu progress'() {
      Map<MaterialConfig, Boolean> materialConfigs = new HashMap<>()
      def git = MaterialConfigsMother.git("http://example.com")
      materialConfigs.put(git, true)
      def modifications = ModificationsMother.withModifiedFileWhoseNameLengthIsOneK()

      def map = new HashMap<>()
      map.put(git.getFingerprint(), modifications)

      when(materialConfigService.getMaterialConfigsWithPermissions(anyString())).thenReturn(materialConfigs)
      when(materialService.getLatestModificationForEachMaterial()).thenReturn(map)
      when(maintenanceModeService.getRunningMDUs()).thenReturn([])

      getWithApiHeader(controller.controllerBasePath())

      def resultMap = new HashMap<>()
      resultMap.put(git, new MaterialInfo(modifications, true, false, null, []))

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(MaterialWithModificationsRepresenter.class, resultMap)
    }

    @Test
    void 'should return 304 when etag matches'() {
      when(materialConfigService.getMaterialConfigs(anyString())).thenReturn(new MaterialConfigs())
      when(materialService.getLatestModificationForEachMaterial()).thenReturn(emptyMap())

      getWithApiHeader(controller.controllerBasePath(), ['if-none-match': 'f329a259ce39701e259956818e1b15eecee59460159d9158a55a885feb612110'])

      assertThatResponse()
        .isNotModified()
    }

    @Test
    void 'should return 200 with empty materials'() {
      when(materialConfigService.getMaterialConfigs(anyString())).thenReturn(new MaterialConfigs())
      when(materialService.getLatestModificationForEachMaterial()).thenReturn(emptyMap())

      getWithApiHeader(controller.controllerBasePath())

      assertThatResponse()
        .isOk()
        .hasEtag("\"f329a259ce39701e259956818e1b15eecee59460159d9158a55a885feb612110\"")
        .hasBodyWithJsonObject(MaterialWithModificationsRepresenter.class, emptyMap())
    }

    @Test
    void 'should return 200 with materials with empty modifications'() {
      Map<MaterialConfig, Boolean> materialConfigs = new HashMap<>()
      def git = MaterialConfigsMother.git("http://example.com")
      materialConfigs.put(git, false)

      when(materialConfigService.getMaterialConfigsWithPermissions(anyString())).thenReturn(materialConfigs)
      when(materialService.getLatestModificationForEachMaterial()).thenReturn(emptyMap())
      when(maintenanceModeService.getRunningMDUs()).thenReturn([])

      getWithApiHeader(controller.controllerBasePath())

      def resultMap = new HashMap<>()
      resultMap.put(git, new MaterialInfo(null, false, false, null, []))

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(MaterialWithModificationsRepresenter.class, resultMap)
    }

    @Test
    void 'should not include dependency materials'() {
      Map<MaterialConfig, Boolean> materialConfigs = new HashMap<>()
      def dependencyConfig = MaterialConfigsMother.dependencyMaterialConfig("pipeline", "stage")
      def git = MaterialConfigsMother.git("http://example.com")
      materialConfigs.put(dependencyConfig, false)
      materialConfigs.put(git, false)
      def modifications = ModificationsMother.withModifiedFileWhoseNameLengthIsOneK()

      def map = new HashMap<>()
      map.put(git.getFingerprint(), modifications)
      map.put(dependencyConfig.getFingerprint(), modifications)

      when(materialConfigService.getMaterialConfigsWithPermissions(anyString())).thenReturn(materialConfigs)
      when(materialService.getLatestModificationForEachMaterial()).thenReturn(map)
      when(maintenanceModeService.getRunningMDUs()).thenReturn([])

      getWithApiHeader(controller.controllerBasePath())

      def resultMap = new HashMap<>()
      resultMap.put(git, new MaterialInfo(modifications, false, false, null, []))

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(MaterialWithModificationsRepresenter.class, resultMap)
    }
  }

  @Nested
  class TriggerUpdate {
    def material = mock(Material.class)
    def git = MaterialConfigsMother.git("http://example.com")

    @BeforeEach
    void setUp() {
      loginAsUser()
      when(materialConfigConverter.toMaterial(any(MaterialConfig.class))).thenReturn(material)
    }

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "triggerUpdate"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerBasePath() + "/some-fingerprint/trigger_update", [:])
      }
    }

    @Test
    void 'should return ok when trigger is successful'() {
      when(materialConfigService.getMaterialConfig(anyString(), anyString())).thenReturn(git)
      when(materialUpdateService.updateMaterial((Material) any())).thenReturn(true)

      postWithApiHeader("/api/internal/materials/abc123/trigger_update", [])

      verify(materialUpdateService).updateMaterial(material)
      assertThatResponse()
        .isCreated()
        .hasJsonMessage("OK")
    }

    @Test
    void 'should not trigger update if update is already in progress'() {
      when(materialConfigService.getMaterialConfig(anyString(), anyString())).thenReturn(git)
      when(materialUpdateService.updateMaterial((Material) any())).thenReturn(false)

      postWithApiHeader("/api/internal/materials/abc123/trigger_update", [])

      verify(materialUpdateService).updateMaterial(material)
      assertThatResponse()
        .isConflict()
        .hasJsonMessage("Update already in progress.")
    }

    @Test
    void 'should return 404 value if material does not exist'() {
      when(materialConfigService.getMaterialConfig(anyString(), anyString())).thenThrow(new RecordNotFoundException("Material not found!!"))

      postWithApiHeader("/api/internal/materials/abc123/trigger_update", [])

      verifyNoInteractions(materialUpdateService)

      assertThatResponse()
        .isNotFound()
        .hasJsonMessage("Material not found!!")
    }
  }
}
