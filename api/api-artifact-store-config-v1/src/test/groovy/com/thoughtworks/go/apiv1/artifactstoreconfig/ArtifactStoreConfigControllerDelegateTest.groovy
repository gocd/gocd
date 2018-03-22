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

package com.thoughtworks.go.apiv1.artifactstoreconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.artifactstoreconfig.representers.ArtifactStoresRepresenter
import com.thoughtworks.go.config.ArtifactStore
import com.thoughtworks.go.config.ArtifactStores
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.server.service.ArtifactStoreService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class ArtifactStoreConfigControllerDelegateTest implements ControllerTrait<ArtifactStoreConfigControllerDelegate>, SecurityServiceTrait {

  @Mock
  ArtifactStoreService artifactStoreService

  @BeforeEach
  void setup() {
    initMocks(this)
  }

  @Override
  ArtifactStoreConfigControllerDelegate createControllerInstance() {
    return new ArtifactStoreConfigControllerDelegate(new ApiAuthenticationHelper(securityService, goConfigService), artifactStoreService)
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
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should get artifact store configs'() {
        def artifactStores = new ArtifactStores(new ArtifactStore("docker", "cd.go.artifact.docker",
          ConfigurationPropertyMother.create("RegistryURL", false, "http://foo")))
        when(artifactStoreService.getPluginProfiles()).thenReturn(artifactStores)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(artifactStores, ArtifactStoresRepresenter)
      }
    }
  }
}
