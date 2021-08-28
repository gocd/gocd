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
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.api.base.JsonUtils.toObjectWithoutLinks
import static com.thoughtworks.go.api.util.HaltApiMessages.*
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

@MockitoSettings(strictness = Strictness.LENIENT)
class ArtifactStoreConfigControllerTest implements ControllerTrait<ArtifactStoreConfigController>, SecurityServiceTrait {

  @Mock
  ArtifactStoreService artifactStoreService

  @Mock
  EntityHashingService entityHashingService

  @Override
  ArtifactStoreConfigController createControllerInstance() {
    def apiAuthenticationHelper = new ApiAuthenticationHelper(securityService, goConfigService)
    return new ArtifactStoreConfigController(apiAuthenticationHelper, artifactStoreService, entityHashingService)
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
          .hasBodyWithJsonObject(ArtifactStoresRepresenter, artifactStores)
      }
    }
  }

  @Nested
  class Show {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @BeforeEach
      void setUp() {
        when(artifactStoreService.findArtifactStore("test"))
          .thenReturn(new ArtifactStore("docker", "cd.go.artifact.docker",
          ConfigurationPropertyMother.create("RegistryURL", false, "http://foo")))
      }

      @Override
      String getControllerMethodUnderTest() {
        return "show"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath('/test'))
      }
    }

    @Nested
    class AsAdmin {
      HttpLocalizedOperationResult result

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
        this.result = new HttpLocalizedOperationResult()
      }

      @Test
      void 'should render artifact store of specified id'() {
        def artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))
        when(entityHashingService.hashForEntity(artifactStore)).thenReturn('digest')
        when(artifactStoreService.findArtifactStore('test')).thenReturn(artifactStore)

        getWithApiHeader(controller.controllerPath('/test'))

        assertThatResponse()
          .isOk()
          .hasEtag('"digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(ArtifactStoreRepresenter, artifactStore)
      }

      @Test
      void 'should return 404 if the artifact store does not exist'() {
        when(artifactStoreService.findArtifactStore('non-existent-store')).thenReturn(null)

        getWithApiHeader(controller.controllerPath('/non-existent-store'))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("non-existent-store"))
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should render 304 if etag matches'() {
        def artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker",
          ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))
        when(entityHashingService.hashForEntity(artifactStore)).thenReturn('digest')
        when(artifactStoreService.findArtifactStore('test')).thenReturn(artifactStore)

        getWithApiHeader(controller.controllerPath('/test'), ['if-none-match': '"digest"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should render 200 if etag does not match'() {
        def artifactStore = new ArtifactStore("docker", "cd.go.artifact.docker",
          ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))
        when(entityHashingService.hashForEntity(artifactStore)).thenReturn('digest')
        when(artifactStoreService.findArtifactStore('test')).thenReturn(artifactStore)

        getWithApiHeader(controller.controllerPath('/test'), ['if-none-match': '"junk"'])

        assertThatResponse()
          .isOk()
          .hasEtag('"digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(ArtifactStoreRepresenter, artifactStore)
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
        when(entityHashingService.hashForEntity(artifactStore)).thenReturn('some-digest')

        postWithApiHeader(controller.controllerPath(), toObjectString({
          ArtifactStoreRepresenter.toJSON(it, artifactStore)
        }))

        assertThatResponse()
          .isOk()
          .hasEtag('"some-digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(ArtifactStoreRepresenter, artifactStore)
      }

      @Test
      void 'should fail to create if there are validation errors'() {
        def artifactStore = new ArtifactStore("test", "cd.go.artifact.docker", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))
        when(artifactStoreService.create(Mockito.any() as Username, Mockito.any() as ArtifactStore, Mockito.any() as LocalizedOperationResult))
          .then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.unprocessableEntity("validation failed")
        })

        postWithApiHeader(controller.controllerPath(), toObjectString({
          ArtifactStoreRepresenter.toJSON(it, artifactStore)
        }))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("validation failed")
      }

      @Test
      void 'should check for artifact store with same id'() {
        def artifactStore = new ArtifactStore("test", "cd.go.artifact.docker", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))
        when(artifactStoreService.findArtifactStore("test")).thenReturn(artifactStore)
        def artifactStoreWithError = new ArtifactStore("test", "cd.go.artifact.docker", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))
        artifactStoreWithError.addError("id", "Artifact store ids should be unique. Artifact store with the same id exists.")

        postWithApiHeader(controller.controllerPath(), toObjectString({
          ArtifactStoreRepresenter.toJSON(it, artifactStore)
        }))

        verify(artifactStoreService, never()).create(Mockito.any() as Username, Mockito.any() as ArtifactStore, Mockito.any() as LocalizedOperationResult)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(entityAlreadyExistsMessage("artifactStore", "test"))
          .hasJsonAttribute("data", toObjectWithoutLinks({ ArtifactStoreRepresenter.toJSON(it, artifactStoreWithError) }))
      }

    }
  }

  @Nested
  class Update {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "update"
      }

      @Override
      void makeHttpCall() {
        putWithApiHeader(controller.controllerPath("/test"), '{}')
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
      void 'should return 404 if store_id is not found'() {
        def artifactStore = new ArtifactStore("test", "cd.go.artifact.docker", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))

        when(artifactStoreService.findArtifactStore('test')).thenReturn(null)
        when(entityHashingService.hashForEntity(artifactStore)).thenReturn('cached-digest')

        putWithApiHeader(controller.controllerPath('/test'), ['if-match': 'some-string'], toObjectString({
          ArtifactStoreRepresenter.toJSON(it, artifactStore)
        }))

        assertThatResponse().isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(controller.entityType.notFoundMessage("test"))
      }

      @Test
      void 'should fail update if etag does not match'() {
        def artifactStore = new ArtifactStore("test", "cd.go.artifact.docker", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))

        when(artifactStoreService.findArtifactStore('test')).thenReturn(artifactStore)
        when(entityHashingService.hashForEntity(artifactStore)).thenReturn('cached-digest')

        putWithApiHeader(controller.controllerPath('/test'), ['if-match': 'some-string'], toObjectString({
          ArtifactStoreRepresenter.toJSON(it, artifactStore)
        }))

        assertThatResponse().isPreconditionFailed()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(etagDoesNotMatch("artifactStore", "test"))
      }

      @Test
      void 'should proceed with update if etag matches'() {
        def artifactStore = new ArtifactStore("test", "cd.go.artifact.docker", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))
        def newArtifactStore = new ArtifactStore("test", "cd.go.artifact.artifactory", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))

        when(artifactStoreService.findArtifactStore('test')).thenReturn(artifactStore)
        when(entityHashingService.hashForEntity(artifactStore)).thenReturn('cached-digest')
        when(entityHashingService.hashForEntity(newArtifactStore)).thenReturn('new-digest')

        putWithApiHeader(controller.controllerPath('/test'), ['if-match': 'cached-digest'], toObjectString({
          ArtifactStoreRepresenter.toJSON(it, newArtifactStore)
        }))

        assertThatResponse()
          .isOk()
          .hasEtag('"new-digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(ArtifactStoreRepresenter, newArtifactStore)
      }

      @Test
      void 'should not allow rename of id'() {
        def artifactStore = new ArtifactStore("test", "cd.go.artifact.docker", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))
        def newArtifactStore = new ArtifactStore("test1", "cd.go.artifact.artifactory", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))

        when(artifactStoreService.findArtifactStore('test')).thenReturn(artifactStore)
        when(entityHashingService.hashForEntity(artifactStore)).thenReturn('cached-digest')
        when(entityHashingService.hashForEntity(newArtifactStore)).thenReturn('new-digest')

        putWithApiHeader(controller.controllerPath('/test'), ['if-match': 'cached-digest'], toObjectString({
          ArtifactStoreRepresenter.toJSON(it, newArtifactStore)
        }))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(renameOfEntityIsNotSupportedMessage('artifactStore'))
      }
    }
  }

  @Nested
  class Destroy {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "destroy"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath('/test'))
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
      void 'should raise an error if artifact store is not found'() {
        when(artifactStoreService.findArtifactStore('test')).thenReturn(null)
        deleteWithApiHeader(controller.controllerPath('/test'))
        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("test"))
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should render the success message on deleting an artifact store'() {
        def artifactStore = new ArtifactStore("test", "cd.go.artifact.docker", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))
        when(artifactStoreService.findArtifactStore('test')).thenReturn(artifactStore)

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.arguments.last()
          result.setMessage(LocalizedMessage.resourceDeleteSuccessful('artifactStore', artifactStore.getId()))
        }).when(artifactStoreService).delete(any() as Username, eq(artifactStore), any() as LocalizedOperationResult)

        deleteWithApiHeader(controller.controllerPath('/test'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(LocalizedMessage.resourceDeleteSuccessful('artifactStore', artifactStore.getId()))
      }

      @Test
      void 'should render the validation errors on failure to delete'() {
        def artifactStore = new ArtifactStore("test", "cd.go.artifact.docker", ConfigurationPropertyMother.create("RegistryURL", false, "http://foo"))

        when(artifactStoreService.findArtifactStore('test')).thenReturn(artifactStore)

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.arguments.last()
          result.unprocessableEntity("save failed")
        }).when(artifactStoreService).delete(any() as Username, eq(artifactStore), any() as LocalizedOperationResult)

        deleteWithApiHeader(controller.controllerPath('/test'))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage('save failed')
      }
    }
  }
}
