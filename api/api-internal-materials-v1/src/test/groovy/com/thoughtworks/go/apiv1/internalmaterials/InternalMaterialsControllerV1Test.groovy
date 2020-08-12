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

package com.thoughtworks.go.apiv1.internalmaterials

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.internalmaterials.models.MaterialInfo
import com.thoughtworks.go.apiv1.internalmaterials.representers.MaterialWithModificationsRepresenter
import com.thoughtworks.go.apiv1.internalmaterials.representers.UsagesRepresenter
import com.thoughtworks.go.config.materials.MaterialConfigs
import com.thoughtworks.go.helper.MaterialConfigsMother
import com.thoughtworks.go.helper.ModificationsMother
import com.thoughtworks.go.server.service.MaintenanceModeService
import com.thoughtworks.go.server.service.MaterialConfigService
import com.thoughtworks.go.server.service.MaterialService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static java.util.Collections.emptyMap
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalMaterialsControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalMaterialsControllerV1> {
  @Mock
  private MaterialConfigService materialConfigService
  @Mock
  private MaterialService materialService
  @Mock
  private MaintenanceModeService maintenanceModeService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalMaterialsControllerV1 createControllerInstance() {
    new InternalMaterialsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), materialConfigService, materialService, maintenanceModeService)
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

    @BeforeEach
    void setUp() {
      loginAsUser()
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
      MaterialConfigs materialConfigs = new MaterialConfigs()
      def git = MaterialConfigsMother.git("http://example.com")
      materialConfigs.add(git)
      def modifications = ModificationsMother.withModifiedFileWhoseNameLengthIsOneK()

      def map = new HashMap<>()
      map.put(git.getFingerprint(), modifications)

      when(materialConfigService.getMaterialConfigs(anyString())).thenReturn(materialConfigs)
      when(materialService.getLatestModificationForEachMaterial()).thenReturn(map)
      when(maintenanceModeService.getRunningMDUs()).thenReturn([])

      getWithApiHeader(controller.controllerBasePath())

      def resultMap = new HashMap<>()
      resultMap.put(git, new MaterialInfo(modifications, false))

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
      MaterialConfigs materialConfigs = new MaterialConfigs()
      def git = MaterialConfigsMother.git("http://example.com")
      materialConfigs.add(git)

      when(materialConfigService.getMaterialConfigs(anyString())).thenReturn(materialConfigs)
      when(materialService.getLatestModificationForEachMaterial()).thenReturn(emptyMap())
      when(maintenanceModeService.getRunningMDUs()).thenReturn([])

      getWithApiHeader(controller.controllerBasePath())

      def resultMap = new HashMap<>()
      resultMap.put(git, new MaterialInfo(null, false))

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(MaterialWithModificationsRepresenter.class, resultMap)
    }

    @Test
    void 'should not include dependency materials'() {
      MaterialConfigs materialConfigs = new MaterialConfigs()
      def dependencyConfig = MaterialConfigsMother.dependencyMaterialConfig("pipeline", "stage")
      def git = MaterialConfigsMother.git("http://example.com")
      materialConfigs.add(dependencyConfig)
      materialConfigs.add(git)
      def modifications = ModificationsMother.withModifiedFileWhoseNameLengthIsOneK()

      def map = new HashMap<>()
      map.put(git.getFingerprint(), modifications)
      map.put(dependencyConfig.getFingerprint(), modifications)

      when(materialConfigService.getMaterialConfigs(anyString())).thenReturn(materialConfigs)
      when(materialService.getLatestModificationForEachMaterial()).thenReturn(map)
      when(maintenanceModeService.getRunningMDUs()).thenReturn([])

      getWithApiHeader(controller.controllerBasePath())

      def resultMap = new HashMap<>()
      resultMap.put(git, new MaterialInfo(modifications, false))

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(MaterialWithModificationsRepresenter.class, resultMap)
    }
  }
}
