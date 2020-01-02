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
package com.thoughtworks.go.apiv2.materials

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.base.JsonUtils
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv2.materials.representers.ModificationsRepresenter
import com.thoughtworks.go.config.materials.git.GitMaterialConfig
import com.thoughtworks.go.domain.materials.Modification
import com.thoughtworks.go.domain.materials.Modifications
import com.thoughtworks.go.server.materials.MaterialUpdateService
import com.thoughtworks.go.server.service.MaterialConfigService
import com.thoughtworks.go.server.service.MaterialService
import com.thoughtworks.go.server.service.result.HttpOperationResult
import com.thoughtworks.go.server.util.Pagination
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class MaterialModificationsControllerV2Test implements SecurityServiceTrait, ControllerTrait<MaterialModificationsControllerV2> {

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Mock
  private MaterialConfigService materialConfigService

  @Mock
  private MaterialService materialService

  @Mock
  private MaterialUpdateService materialUpdateService

  @Override
  MaterialModificationsControllerV2 createControllerInstance() {
    return new MaterialModificationsControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), materialConfigService,
      materialService)
  }

  @Nested
  class MaterialModifications {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "modifications"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath())
      }
    }

    @Nested
    class AsUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsUser()
      }

      @Test
      void 'should list material modifications'() {
        def pagination = Pagination.pageStartingAt(0, 2, 10)
        def modification1 = new Modification("user1", "comment1", "email@ediblefrog", new Date(), "revision1")
        modification1.id = 1
        def modification2 = new Modification("user2", "comment2", "email@argentino", new Date(), "anotherRevision")
        modification2.id = 2
        def modifications = new Modifications(modification1, modification2)
        def materialConfig = new GitMaterialConfig()
        materialConfig.url = "http://example.git/"

        when(materialService.getTotalModificationsFor(materialConfig)).thenReturn(2l)
        when(materialConfigService.getMaterialConfig(eq(currentUsernameString()), eq("fingerprint"), any(HttpOperationResult.class)))
          .thenReturn(materialConfig)
        when(materialService.getModificationsFor(materialConfig, pagination)).thenReturn(modifications)

        getWithApiHeader(Routes.MaterialModifications.modification("fingerprint"))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJson(JsonUtils.toObjectString({
            ModificationsRepresenter.toJSON(it, modifications, pagination, "fingerprint")
          }))
      }
    }
  }
}
