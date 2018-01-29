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

package com.thoughtworks.go.apiv1.admin.artifactstore

import com.thoughtworks.go.api.ClearSingletonExtension
import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.api.util.HaltApiMessages
import com.thoughtworks.go.apiv1.admin.artifactstore.representers.ArtifactStoreRepresenter
import com.thoughtworks.go.apiv1.admin.artifactstore.representers.ArtifactStoresRepresenter
import com.thoughtworks.go.config.ArtifactStore
import com.thoughtworks.go.config.ArtifactStores
import com.thoughtworks.go.i18n.LocalizedMessage
import com.thoughtworks.go.server.service.ArtifactStoreService
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.util.HaltApiMessages.entityAlreadyExistsMessage
import static com.thoughtworks.go.api.util.HaltApiMessages.etagDoesNotMatch
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

@ExtendWith(ClearSingletonExtension.class)
class ArtifactStoresControllerV1DelegateTest implements SecurityServiceTrait, ControllerTrait<ArtifactStoresControllerV1Delegate> {

  @Mock
  ArtifactStoreService artifactStoreService
  @Mock
  EntityHashingService entityHashingService

  @BeforeEach
  void setup() {
    initMocks(this)
  }

  @Override
  ArtifactStoresControllerV1Delegate createControllerInstance() {
    return new ArtifactStoresControllerV1Delegate(artifactStoreService, entityHashingService, new ApiAuthenticationHelper(securityService, goConfigService), localizer)
  }

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait {

      @Test
      void 'should allow all with security disabled'() {
        disableSecurity()

        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void "should disallow anonymous users, with security enabled"() {
        enableSecurity()
        loginAsAnonymous()

        makeHttpCall()

        assertRequestNotAuthorized()
      }

      @Test
      void 'should disallow normal users, with security enabled'() {
        enableSecurity()
        loginAsUser()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

      @Test
      void 'should allow admin, with security enabled'() {
        enableSecurity()
        loginAsAdmin()

        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void 'should disallow pipeline group admin users, with security enabled'() {
        enableSecurity()
        loginAsGroupAdmin()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

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
      void 'should list all artifact stores'() {
        def expectedArtifactStores = new ArtifactStores(new ArtifactStore("docker", "cd.go.docker"))
        when(entityHashingService.md5ForEntity(expectedArtifactStores)).thenReturn("some-etag")
        when(artifactStoreService.getPluginProfiles()).thenReturn(expectedArtifactStores)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasEtag('"some-etag"')
          .hasContentType(controller.mimeType)
          .hasJsonBodySerializedWith(expectedArtifactStores, ArtifactStoresRepresenter)
      }

      @Test
      void 'should render 304 if etag matches'() {
        def expectedArtifactStores = new ArtifactStores(new ArtifactStore("docker", "cd.go.docker"))

        when(entityHashingService.md5ForEntity(expectedArtifactStores)).thenReturn("some-etag")
        when(artifactStoreService.getPluginProfiles()).thenReturn(expectedArtifactStores)

        getWithApiHeader(controller.controllerPath(), ['if-none-match': '"some-etag"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }
    }
  }

  @Nested
  class Show {
    @Nested
    class Security implements SecurityTestTrait {
      @BeforeEach
      void setUp() {
        when(artifactStoreService.findArtifactStore("foo")).thenReturn(new ArtifactStore("foo", "bar"))
      }

      @Test
      void 'should allow all with security disabled'() {
        disableSecurity()

        makeHttpCall()
        assertRequestAuthorized()
      }

      def 'should disallow anonymous users, with security enabled'() {
        enableSecurity()
        loginAsAnonymous()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

      def 'should disallow normal users, with security enabled'() {
        enableSecurity()
        loginAsUser()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

      def 'should allow admin, with security enabled'() {
        enableSecurity()
        loginAsAdmin()

        makeHttpCall()
        assertRequestAuthorized()
      }

      def 'should disallow pipeline group admin users, with security enabled'() {
        enableSecurity()
        loginAsGroupAdmin()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

      @Override
      String getControllerMethodUnderTest() {
        return "show"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath('/foo'))
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
      void 'should render the artifact store of specified id'() {
        def artifactStore = new ArtifactStore('docker', 'cd.go.docker')
        when(entityHashingService.md5ForEntity(artifactStore)).thenReturn('md5')
        when(artifactStoreService.findArtifactStore('docker')).thenReturn(artifactStore)

        getWithApiHeader(controller.controllerPath('/docker'))

        assertThatResponse()
          .isOk()
          .hasEtag('"md5"')
          .hasContentType(controller.mimeType)
          .hasJsonBodySerializedWith(artifactStore, ArtifactStoreRepresenter)
      }

      @Test
      void 'should return 404 if the artifact store does not exist'() {
        when(artifactStoreService.findArtifactStore('non-existent-artifact-store')).thenReturn(null)

        getWithApiHeader(controller.controllerPath('/non-existent-artifact-store'))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(HaltApiMessages.notFoundMessage())
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should render 304 if etag matches'() {
        def artifactStore = new ArtifactStore('docker', 'cd.go.docker')
        when(entityHashingService.md5ForEntity(artifactStore)).thenReturn('md5')
        when(artifactStoreService.findArtifactStore('docker')).thenReturn(artifactStore)
        getWithApiHeader(controller.controllerPath('/docker'), ['if-none-match': '"md5"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should render 200 if etag does not match'() {
        def artifactStore = new ArtifactStore('docker', 'cd.go.docker')
        when(entityHashingService.md5ForEntity(artifactStore)).thenReturn('md5')
        when(artifactStoreService.findArtifactStore('docker')).thenReturn(artifactStore)
        getWithApiHeader(controller.controllerPath('/docker'), ['if-none-match': '"junk"'])

        assertThatResponse()
          .isOk()
          .hasEtag('"md5"')
          .hasContentType(controller.mimeType)
          .hasJsonBodySerializedWith(artifactStore, ArtifactStoreRepresenter)
      }
    }
  }

  @Nested
  class Create {

    @Nested
    class Security implements SecurityTestTrait {

      @Test
      void 'should allow all with security disabled'() {
        disableSecurity()

        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      def 'should disallow anonymous users, with security enabled'() {
        enableSecurity()
        loginAsAnonymous()

        makeHttpCall()

        assertRequestNotAuthorized()
      }

      @Test
      def 'should disallow normal users, with security enabled'() {
        enableSecurity()
        loginAsUser()

        makeHttpCall()

        assertRequestNotAuthorized()
      }

      @Test
      def 'should allow admin, with security enabled'() {
        enableSecurity()
        loginAsAdmin()

        makeHttpCall()

        assertRequestAuthorized()
      }

      @Test
      def 'should disallow pipeline group admin users, with security enabled'() {
        enableSecurity()
        loginAsGroupAdmin()

        makeHttpCall()

        assertRequestNotAuthorized()
      }

      @Override
      String getControllerMethodUnderTest() { return "create" }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), "{}")
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
      void 'should deserialize artifact store from given parameters'() {
        ArtifactStore artifactStore = new ArtifactStore('docker', 'docker')
        when(entityHashingService.md5ForEntity(artifactStore)).thenReturn('some-md5')
        when(artifactStoreService.findArtifactStore('docker')).thenReturn(null)
        doNothing().when(artifactStoreService).create(any(), any(), any())

        postWithApiHeader(controller.controllerPath(), ArtifactStoreRepresenter.toJSON(artifactStore, requestContext))

        assertThatResponse()
          .isOk()
          .hasEtag('"some-md5"')
          .hasContentType(controller.mimeType)
          .hasJsonBodySerializedWith(artifactStore, ArtifactStoreRepresenter)
      }

      @Test
      void 'should fail to save if there are validation errors'() {
        ArtifactStore artifactStore = new ArtifactStore('docker', 'docker')

        when(artifactStoreService.create(any(), any(), any())).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.unprocessableEntity(LocalizedMessage.string("ENTITY_CONFIG_VALIDATION_FAILED"))
        })

        postWithApiHeader(controller.controllerPath(), ArtifactStoreRepresenter.toJSON(artifactStore, requestContext))

        assertThatResponse()
          .isUnprocessibleEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("ENTITY_CONFIG_VALIDATION_FAILED")
      }

      @Test
      void 'should check for existence of artifact store with same id'() {
        ArtifactStore artifactStore = new ArtifactStore('docker', 'skunkworks')

        ArtifactStore expectedArtifactStore = new ArtifactStore('docker', "foo")
        expectedArtifactStore.addError('id', 'Artifact store ids should be unique. Artifact store with the same id exists.')

        when(artifactStoreService.findArtifactStore('docker')).thenReturn(artifactStore)
        postWithApiHeader(controller.controllerPath(), ArtifactStoreRepresenter.toJSON(expectedArtifactStore, requestContext))

        verify(artifactStoreService, never()).create(any(), any(), any())

        assertThatResponse()
          .isUnprocessibleEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(entityAlreadyExistsMessage("artifactStore", "docker"))
          .hasJsonAtrribute('data', ArtifactStoreRepresenter.toJSON(expectedArtifactStore, requestContext))
      }
    }
  }

  @Nested
  class Update {
    @Nested
    class Security implements SecurityTestTrait {
      ArtifactStore artifactStore = new ArtifactStore('docker', 'skunkworks')

      @BeforeEach
      void setUp() {
        when(artifactStoreService.findArtifactStore(artifactStore.getId())).thenReturn(artifactStore)
        when(entityHashingService.md5ForEntity(artifactStore)).thenReturn('cached-md5')
      }

      @Override
      String getControllerMethodUnderTest() {
        return "update"
      }

      @Override
      void makeHttpCall() {
        sendRequest('put', controller.controllerPath('/docker'), [
          'accept'      : controller.mimeType,
          'If-Match'    : 'cached-md5',
          'content-type': 'application/json'
        ], ArtifactStoreRepresenter.toJSON(this.artifactStore, requestContext))
      }

      @Test
      void 'should allow all with security disabled'() {
        disableSecurity()
        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void 'should disallow anonymous users, with security enabled'() {
        enableSecurity()
        loginAsAnonymous()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

      @Test
      void 'should disallow normal users, with security enabled'() {
        enableSecurity()
        loginAsUser()
        makeHttpCall()
        assertRequestNotAuthorized()
      }

      @Test
      void 'should allow admin, with security enabled'() {
        enableSecurity()
        loginAsAdmin()
        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void 'should disallow pipeline group admin users, with security enabled'() {
        enableSecurity()
        loginAsGroupAdmin()
        makeHttpCall()
        assertRequestNotAuthorized()
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
      void 'should not allow rename of artifact store id'() {
        ArtifactStore artifactStore = new ArtifactStore('foo', 'cd.go.docker')

        when(artifactStoreService.findArtifactStore('foo')).thenReturn(artifactStore)
        when(entityHashingService.md5ForEntity(artifactStore)).thenReturn("cached-md5")

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'cached-md5',
          'content-type': 'application/json'
        ]
        def body = [
          id: 'docker',
        ]

        putWithApiHeader(controller.controllerPath('/foo'), headers, body)

        assertThatResponse()
          .isUnprocessibleEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(HaltApiMessages.renameOfEntityIsNotSupportedMessage("artifactStore"))
      }

      @Test
      void 'should fail update if etag does not match'() {
        ArtifactStore artifactStore = new ArtifactStore('docker', 'cd.go.docker')

        when(artifactStoreService.findArtifactStore('docker')).thenReturn(artifactStore)
        when(entityHashingService.md5ForEntity(artifactStore)).thenReturn('cached-md5')

        putWithApiHeader(controller.controllerPath('/docker'), ['if-match': 'some-string'], ArtifactStoreRepresenter.toJSON(artifactStore, requestContext))

        assertThatResponse()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(etagDoesNotMatch("artifactStore", "docker"))
      }

      @Test
      void 'should proceed with update if etag matches'() {
        ArtifactStore artifactStore = new ArtifactStore('docker', 'cd.go.docker')
        ArtifactStore newRole = new ArtifactStore('docker', 'docker')

        when(artifactStoreService.findArtifactStore('docker')).thenReturn(artifactStore)
        when(entityHashingService.md5ForEntity(artifactStore)).thenReturn('cached-md5')
        when(entityHashingService.md5ForEntity(newRole)).thenReturn('new-md5')

        putWithApiHeader(controller.controllerPath('/docker'), ['if-match': 'cached-md5'], ArtifactStoreRepresenter.toJSON((ArtifactStore) newRole, requestContext))

        assertThatResponse()
          .isOk()
          .hasEtag('"new-md5"')
          .hasContentType(controller.mimeType)
          .hasJsonBodySerializedWith(newRole, ArtifactStoreRepresenter)
      }
    }
  }

  @Nested
  class Destroy {
    @Nested
    class Security implements SecurityTestTrait {
      ArtifactStore artifactStore = new ArtifactStore('docker', 'skunkworks')

      @BeforeEach
      void setUp() {
        when(artifactStoreService.findArtifactStore('docker')).thenReturn(artifactStore)
      }

      @Override
      String getControllerMethodUnderTest() {
        return "destroy"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath('/docker'))
      }

      @Test
      void 'should allow all with security disabled'() {
        disableSecurity()
        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void 'should disallow anonymous users, with security enabled'() {
        enableSecurity()
        loginAsAnonymous()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

      @Test
      void 'should disallow normal users, with security enabled'() {
        enableSecurity()
        loginAsUser()
        makeHttpCall()
        assertRequestNotAuthorized()
      }

      @Test
      void 'should allow admin, with security enabled'() {
        enableSecurity()
        loginAsAdmin()
        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void 'should disallow pipeline group admin users, with security enabled'() {
        enableSecurity()
        loginAsGroupAdmin()
        makeHttpCall()
        assertRequestNotAuthorized()
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
      void 'should raise an error if artifactStore is not found'() {
        when(artifactStoreService.findArtifactStore('docker')).thenReturn(null)
        deleteWithApiHeader(controller.controllerPath('/docker'))
        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(HaltApiMessages.notFoundMessage())
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should render the success message on deleting a artifactStore'() {
        ArtifactStore artifactStore = new ArtifactStore('docker', 'cd.go.docker')
        when(artifactStoreService.findArtifactStore('docker')).thenReturn(artifactStore)

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.arguments.last()
          result.setMessage(LocalizedMessage.string("RESOURCE_DELETE_SUCCESSFUL", 'artifactStore', artifactStore.getId()))
        }).when(artifactStoreService).delete(any(), eq(artifactStore), any())

        deleteWithApiHeader(controller.controllerPath('/docker'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage('RESOURCE_DELETE_SUCCESSFUL')
      }

      @Test
      void 'should render the validation errors on failure to delete'() {
        ArtifactStore artifactStore = new ArtifactStore('docker', 'cd.go.docker')

        when(artifactStoreService.findArtifactStore('docker')).thenReturn(artifactStore)

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.arguments.last()
          result.unprocessableEntity(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", 'validation error'))
        }).when(artifactStoreService).delete(any(), eq(artifactStore), any())

        deleteWithApiHeader(controller.controllerPath('/docker'))

        assertThatResponse()
          .isUnprocessibleEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage('SAVE_FAILED_WITH_REASON')
      }
    }
  }
}