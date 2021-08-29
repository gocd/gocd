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
package com.thoughtworks.go.apiv2.materials

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv2.materials.representers.MaterialConfigsRepresenter
import com.thoughtworks.go.config.materials.MaterialConfigs
import com.thoughtworks.go.config.materials.git.GitMaterialConfig
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig
import com.thoughtworks.go.server.materials.MaterialUpdateService
import com.thoughtworks.go.server.service.MaterialConfigService
import com.thoughtworks.go.server.service.MaterialService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class MaterialConfigControllerV2Test implements SecurityServiceTrait, ControllerTrait<MaterialConfigControllerV2> {


  @Mock
  private MaterialConfigService materialConfigService

  @Mock
  private MaterialService materialService

  @Mock
  private MaterialUpdateService materialUpdateService

  @Override
  MaterialConfigControllerV2 createControllerInstance() {
    return new MaterialConfigControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), materialConfigService
    )
  }

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

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
    class AsUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsUser()
      }

      @Test
      void 'should list all materials from config xml'() {
        def gitMaterialConfig = new GitMaterialConfig()
        gitMaterialConfig.url = "http://example.com/git-repo"
        gitMaterialConfig.branch = "master"
        gitMaterialConfig.shallowClone = true
        def hgMaterialConfig = new HgMaterialConfig()
        hgMaterialConfig.url = "http://example.com/hg-repo"
        hgMaterialConfig.branchAttribute = "test"
        MaterialConfigs materialConfigs = new MaterialConfigs(gitMaterialConfig, hgMaterialConfig)
        when(materialConfigService.getMaterialConfigs(currentUsernameString())).thenReturn(materialConfigs)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(MaterialConfigsRepresenter, materialConfigs)
      }
    }
  }
}
