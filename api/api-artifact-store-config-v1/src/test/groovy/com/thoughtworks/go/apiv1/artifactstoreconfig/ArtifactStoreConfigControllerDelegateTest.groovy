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
import com.thoughtworks.go.apiv1.artifactstoreconfig.representers.ArtifactStoreRepresenter
import com.thoughtworks.go.apiv1.artifactstoreconfig.representers.ArtifactStoresRepresenter
import com.thoughtworks.go.config.ArtifactStore
import com.thoughtworks.go.config.ArtifactStores
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.i18n.LocalizedMessage
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.ArtifactStoreService
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.server.service.result.LocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.api.util.HaltApiMessages.entityAlreadyExistsMessage
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class ArtifactStoreConfigControllerDelegateTest implements ControllerTrait<ArtifactStoreConfigControllerDelegate>, SecurityServiceTrait {

  @Mock
  ArtifactStoreService artifactStoreService

  @Mock
  EntityHashingService entityHashingService

  @BeforeEach
  void setup() {
    initMocks(this)
  }

  @Override
  ArtifactStoreConfigControllerDelegate createControllerInstance() {
    def apiAuthenticationHelper = new ApiAuthenticationHelper(securityService, goConfigService)
    return new ArtifactStoreConfigControllerDelegate(apiAuthenticationHelper, artifactStoreService, entityHashingService, localizer)
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

  @Nested
  class Create {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "create"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), '{}')
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
      void 'should deserialize artifact store config from given payload'() {
        def artifactStore = new ArtifactStore("test", "cd.go.artifact.docker", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))
        when(entityHashingService.md5ForEntity(artifactStore)).thenReturn('some-md5')

        postWithApiHeader(controller.controllerPath(), toObjectString({
          ArtifactStoreRepresenter.toJSON(it, artifactStore)
        }))

        assertThatResponse()
          .isOk()
          .hasEtag('"some-md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(artifactStore, ArtifactStoreRepresenter)
      }

      @Test
      void 'should fail to create if there are validation errors'() {
        def artifactStore = new ArtifactStore("test", "cd.go.artifact.docker", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))
        when(artifactStoreService.create(Mockito.any() as Username, Mockito.any() as ArtifactStore, Mockito.any() as LocalizedOperationResult))
          .then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.unprocessableEntity(LocalizedMessage.string("ENTITY_CONFIG_VALIDATION_FAILED"))
        })

        postWithApiHeader(controller.controllerPath(), toObjectString({
          ArtifactStoreRepresenter.toJSON(it, artifactStore)
        }))

        assertThatResponse()
          .isUnprocessibleEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("ENTITY_CONFIG_VALIDATION_FAILED")
      }

      @Test
      void 'should check for artifact store with same id'() {
        def artifactStore = new ArtifactStore("test", "cd.go.artifact.docker", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))
        when(artifactStoreService.findArtifactStore("test")).thenReturn(artifactStore)

        postWithApiHeader(controller.controllerPath(), toObjectString({
          ArtifactStoreRepresenter.toJSON(it, artifactStore)
        }))

        verify(artifactStoreService, never()).create(Mockito.any() as Username, Mockito.any() as ArtifactStore, Mockito.any() as LocalizedOperationResult)

        assertThatResponse()
          .isUnprocessibleEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(entityAlreadyExistsMessage("artifactStore", "test"))
          .hasJsonAtrribute("data", toObject({ ArtifactStoreRepresenter.toJSON(it, artifactStore) }))
      }

    }
  }
}
