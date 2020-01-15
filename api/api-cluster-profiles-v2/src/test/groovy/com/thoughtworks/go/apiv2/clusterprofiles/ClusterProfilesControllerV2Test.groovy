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
package com.thoughtworks.go.apiv2.clusterprofiles

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv2.clusterprofiles.representers.ClusterProfileRepresenter
import com.thoughtworks.go.apiv2.clusterprofiles.representers.ClusterProfilesRepresenter
import com.thoughtworks.go.config.elastic.ClusterProfile
import com.thoughtworks.go.config.elastic.ClusterProfiles
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.ClusterProfilesService
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import groovy.json.JsonBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.util.HaltApiMessages.etagDoesNotMatch
import static com.thoughtworks.go.api.util.HaltApiMessages.renameOfEntityIsNotSupportedMessage
import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class ClusterProfilesControllerV2Test implements SecurityServiceTrait, ControllerTrait<ClusterProfilesControllerV2> {
  @Mock
  ClusterProfilesService clusterProfilesService

  @Mock
  EntityHashingService entityHashingService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  ClusterProfilesControllerV2 createControllerInstance() {
    new ClusterProfilesControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), clusterProfilesService, entityHashingService)
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
        getWithApiHeader(controller.controllerBasePath())
      }
    }

    @Nested
    class AsAdminUser {
      def clusterProfile

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

        clusterProfile = new ClusterProfile("docker", "cd.go.docker")

        when(clusterProfilesService.getPluginProfiles()).thenReturn(new ClusterProfiles(clusterProfile))
      }

      @Test
      void 'should render all clusters'() {
        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(ClusterProfilesRepresenter.class, new ClusterProfiles(clusterProfile), { id -> true })
      }
    }
  }

  @Nested
  class GetClusterProfile {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "getClusterProfile"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath("/docker"))
      }
    }

    @Nested
    class AsAdminUser {
      def clusterProfile

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

        clusterProfile = new ClusterProfile("docker", "cd.go.docker")

        when(clusterProfilesService.findProfile("docker")).thenReturn(clusterProfile)
        when(entityHashingService.md5ForEntity(clusterProfile)).thenReturn("md5")
      }

      @Test
      void 'should render cluster profile'() {
        getWithApiHeader(controller.controllerPath("/docker"))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasEtag('"md5"')
          .hasBodyWithJsonObject(ClusterProfileRepresenter.class, clusterProfile, true)
      }

      @Test
      void 'should render not modified when ETag matches'() {
        getWithApiHeader(controller.controllerPath("/docker"), ['if-none-match': 'md5'])

        assertThatResponse()
          .isNotModified()
      }


      @Test
      void 'should render not found exception for non existent cluster profile'() {
        getWithApiHeader(controller.controllerPath("/test"))

        assertThatResponse()
          .isNotFound()
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
        postWithApiHeader(controller.controllerPath(), [id: 'some_id'])
      }
    }

    @Nested
    class AsAdminUser {
      def clusterProfile

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

        clusterProfile = new ClusterProfile("docker", "cd.go.docker")
        when(entityHashingService.md5ForEntity(clusterProfile)).thenReturn("md5")
      }

      @Test
      void 'should create cluster profile'() {
        def jsonPayload = [
          id        : 'docker',
          plugin_id : 'cd.go.docker',
          properties: []]

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        assertThatResponse()
          .isOk()
          .hasEtag('"md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(ClusterProfileRepresenter, clusterProfile, true)
      }

      @Test
      void 'should not create cluster profile if one already exist with same id'() {
        def existingClusterProfile = new ClusterProfile("docker", "cd.go.docker", create("DockerURI", false, "http://foo"))

        def jsonPayload = [
          id        : 'docker',
          plugin_id : 'cd.go.docker',
          properties: [
            [
              "key"  : "DockerURI",
              "value": "http://foo"
            ]
          ]]

        when(entityHashingService.md5ForEntity(Mockito.any() as ClusterProfile)).thenReturn('some-md5')
        when(clusterProfilesService.findProfile("docker")).thenReturn(existingClusterProfile)

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        def expectedResponseBody = [
          message: "Failed to add Cluster Profile 'docker'. Another Cluster Profile with the same name already exists.",
          data   : [
            id            : "docker",
            plugin_id     : "cd.go.docker",
            can_administer: true,
            properties    : [[key: "DockerURI", value: "http://foo"]],
            errors        : [id: ["Cluster Profile ids should be unique. Cluster Profile with id 'docker' already exists."]]
          ]
        ]

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonBody(new JsonBuilder(expectedResponseBody).toString())
      }

      @Test
      void 'should render errors occurred while creating cluster profile'() {
        def jsonPayload = [
          id        : 'docker',
          plugin_id : 'cd.go.docker',
          properties: []
        ]

        when(clusterProfilesService.create(any() as ClusterProfile, any() as Username, any() as HttpLocalizedOperationResult)).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(2)
          result.unprocessableEntity("Boom!")
        })

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Boom!")
      }
    }
  }

  @Nested
  class DeleteClusterProfile {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "deleteClusterProfile"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath("/docker"))
      }
    }

    @Nested
    class AsAdminUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should delete cluster profile'() {
        def clusterProfile = new ClusterProfile("docker", "cd.go.docker")
        when(clusterProfilesService.findProfile("docker")).thenReturn(clusterProfile)

        deleteWithApiHeader(controller.controllerPath("/docker"))

        assertThatResponse()
          .isOk()
      }

      @Test
      void 'should render not found exception for non existent cluster profile'() {
        when(clusterProfilesService.getPluginProfiles()).thenReturn(new ClusterProfiles())

        deleteWithApiHeader(controller.controllerPath("/docker"))

        assertThatResponse()
          .isNotFound()
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
        putWithApiHeader(controller.controllerPath("/docker"), '{}')
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
      void 'should update cluster profile if etag matches'() {
        def existingCluster = new ClusterProfile("docker", "cd.go.docker", create("DockerURI", false, "http://foo"))
        def updatedCluster = new ClusterProfile("docker", "cd.go.docker", create("DockerURI", false, "http://new-uri"))

        def jsonPayload = [
          id        : 'docker',
          plugin_id : 'cd.go.docker',
          properties: [
            [
              "key"  : "DockerURI",
              "value": "http://new-uri"
            ]
          ]]

        when(entityHashingService.md5ForEntity(existingCluster)).thenReturn('some-md5')
        when(entityHashingService.md5ForEntity(updatedCluster)).thenReturn('new-md5')
        when(clusterProfilesService.findProfile("docker")).thenReturn(existingCluster)

        putWithApiHeader(controller.controllerPath("/docker"), ['if-match': 'some-md5'], jsonPayload)

        assertThatResponse()
          .isOk()
          .hasEtag('"new-md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(ClusterProfileRepresenter, updatedCluster, true)
      }

      @Test
      void 'should not update cluster profile if etag does not match'() {
        def existingProfile = new ClusterProfile("docker", "cd.go.docker", create("DockerURI", false, "http://foo"))

        def jsonPayload = [
          id        : 'docker',
          plugin_id : 'cd.go.docker',
          properties: [
            [
              "key"  : "DockerURI",
              "value": "http://new-uri"
            ]
          ]]

        when(entityHashingService.md5ForEntity(existingProfile)).thenReturn('some-md5')
        when(clusterProfilesService.findProfile("docker")).thenReturn(existingProfile)

        putWithApiHeader(controller.controllerPath("/docker"), ['if-match': 'wrong-md5'], jsonPayload)

        assertThatResponse()
          .isPreconditionFailed()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(etagDoesNotMatch("Cluster Profile", "docker"))
      }

      @Test
      void 'should return 404 if the profile does not exists'() {
        when(clusterProfilesService.findProfile("docker")).thenReturn(null)
        putWithApiHeader(controller.controllerPath("/docker"), ['if-match': 'wrong-md5'], [:])

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(controller.entityType.notFoundMessage("docker"))
      }

      @Test
      void 'should return 422 if attempted rename'() {
        def existingCluster = new ClusterProfile("docker", "cd.go.docker", create("DockerURI", false, "http://foo"))
        def jsonPayload = [
          id        : 'docker-new',
          plugin_id : 'cd.go.docker',
          properties: [
            [
              "key"  : "DockerURI",
              "value": "http://new-uri"
            ]
          ]]

        when(entityHashingService.md5ForEntity(existingCluster)).thenReturn('some-md5')
        when(clusterProfilesService.findProfile("docker")).thenReturn(existingCluster)

        putWithApiHeader(controller.controllerPath("/docker"), ['if-match': 'some-md5'], jsonPayload)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(renameOfEntityIsNotSupportedMessage('Cluster Profile'))
      }

      @Test
      void 'should return 422 for validation error'() {
        def existingCluster = new ClusterProfile("docker", "cd.go.docker", create("DockerURI", false, "http://foo"))
        def jsonPayload = [
          id        : 'docker',
          plugin_id : 'cd.go.docker',
          properties: [
            [
              "key"  : "DockerURI",
              "value": "http://foo"
            ]
          ]]

        when(entityHashingService.md5ForEntity(existingCluster)).thenReturn('some-md5')
        when(clusterProfilesService.findProfile("docker")).thenReturn(existingCluster)

        when(clusterProfilesService.update(Mockito.any() as ClusterProfile, Mockito.any() as Username, Mockito.any() as HttpLocalizedOperationResult)).then({ InvocationOnMock invocation ->
          ClusterProfile clusterProfile = invocation.getArguments()[0]
          clusterProfile.addError("plugin_id", "Plugin not installed.")
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.unprocessableEntity("validation failed")
        })

        putWithApiHeader(controller.controllerPath("/docker"), ['if-match': 'some-md5'], jsonPayload)

        def expectedResponseBody = [
          message: "validation failed",
          data   : [
            id            : "docker",
            plugin_id     : "cd.go.docker",
            can_administer: true,
            properties    : [[key: "DockerURI", value: "http://foo"]],
            errors        : [plugin_id: ["Plugin not installed."]]
          ]
        ]

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonBody(new JsonBuilder(expectedResponseBody).toString())
      }
    }
  }
}
