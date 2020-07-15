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
import com.thoughtworks.go.apiv1.internalmaterials.representers.UsagesRepresenter
import com.thoughtworks.go.server.service.MaterialConfigService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalMaterialsControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalMaterialsControllerV1> {
  @Mock
  private MaterialConfigService materialConfigService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalMaterialsControllerV1 createControllerInstance() {
    new InternalMaterialsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), materialConfigService)
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
      def usages = new HashMap<>()
      usages.put("grp", ["pipeline1", "pipeline2"])
      when(materialConfigService.getUsagesForMaterial(anyString(), anyString())).thenReturn(usages)

      getWithApiHeader(controller.controllerBasePath() + "/some-fingerprint/usages")

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(UsagesRepresenter.class, "some-fingerprint", usages)
    }
  }
}
