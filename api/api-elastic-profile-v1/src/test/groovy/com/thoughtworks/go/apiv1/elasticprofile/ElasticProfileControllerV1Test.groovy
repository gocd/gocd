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

package com.thoughtworks.go.apiv1.elasticprofile

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.api.util.HaltApiMessages
import com.thoughtworks.go.apiv1.elasticprofile.representers.ElasticProfileRepresenter
import com.thoughtworks.go.apiv1.elasticprofile.representers.ElasticProfilesRepresenter
import com.thoughtworks.go.config.elastic.ElasticProfile
import com.thoughtworks.go.config.elastic.ElasticProfiles
import com.thoughtworks.go.i18n.LocalizedMessage
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.ElasticProfileService
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.server.service.result.LocalizedOperationResult
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import groovy.json.JsonBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.util.HaltApiMessages.*
import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.doAnswer
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class ElasticProfileControllerV1Test implements SecurityServiceTrait, ControllerTrait<ElasticProfileControllerV1> {
  @Mock
  private ElasticProfileService elasticProfileService

  @Mock
  private EntityHashingService entityHashingService

  @BeforeEach
  void setup() {
    initMocks(this)
  }

  @Override
  ElasticProfileControllerV1 createControllerInstance() {
    return new ElasticProfileControllerV1(elasticProfileService, new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService)
  }

  @Nested
  class Index {

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'index'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath())
      }
    }

    @Nested
    class AsGroupAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupAdmin()
      }

      @Test
      void 'should list all elastic profiles'() {
        def elasticProfiles = new ElasticProfiles(
          new ElasticProfile("docker", "cd.go.docker", create("docker-uri", false, "unix:///var/run/docker")),
          new ElasticProfile("ecs", "cd.go.ecs", create("ACCESS_KEY", true, "encrypted-key")),
          new ElasticProfile("k8s", "cd.go.k8s", create("cluster-uri", false, "https://foo.bar"))
        )

        when(elasticProfileService.getPluginProfiles()).thenReturn(elasticProfiles)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(elasticProfiles, ElasticProfilesRepresenter)
      }
    }
  }

  @Nested
  class Show {

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'show'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath("/docker"))
      }
    }

    @Nested
    class AsGroupAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupAdmin()
      }

      @Test
      void 'should return elastic profile of specified id'() {
        def dockerElasticProfile = new ElasticProfile("docker", "cd.go.docker", create("docker-uri", false, "unix:///var/run/docker"))

        when(entityHashingService.md5ForEntity(dockerElasticProfile)).thenReturn('md5')
        when(elasticProfileService.findProfile('docker')).thenReturn(dockerElasticProfile)

        getWithApiHeader(controller.controllerPath('/docker'))

        assertThatResponse()
          .isOk()
          .hasEtag('"md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(dockerElasticProfile, ElasticProfileRepresenter)
      }

      @Test
      void 'should return 404 if elastic profile with id does not exist'() {
        getWithApiHeader(controller.controllerPath('/docker'))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(HaltApiMessages.notFoundMessage())
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 304 if elastic profile is not modified'() {
        def dockerElasticProfile = new ElasticProfile("docker", "cd.go.docker", create("docker-uri", false, "unix:///var/run/docker"))

        when(entityHashingService.md5ForEntity(dockerElasticProfile)).thenReturn('md5')
        when(elasticProfileService.findProfile('docker')).thenReturn(dockerElasticProfile)

        getWithApiHeader(controller.controllerPath('/docker'), ['if-none-match': '"md5"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 200 with elastic profile if etag does not match'() {
        def dockerElasticProfile = new ElasticProfile("docker", "cd.go.docker", create("docker-uri", false, "unix:///var/run/docker"))

        when(entityHashingService.md5ForEntity(dockerElasticProfile)).thenReturn('md5-new')
        when(elasticProfileService.findProfile('docker')).thenReturn(dockerElasticProfile)

        getWithApiHeader(controller.controllerPath('/docker'), ['if-none-match': '"md5"'])

        assertThatResponse()
          .isOk()
          .hasEtag('"md5-new"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(dockerElasticProfile, ElasticProfileRepresenter)
      }
    }
  }

  @Nested
  class Create {

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

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
    class AsGroupAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupAdmin()
      }

      @Test
      void 'should create elastic profile from given json payload'() {
        def jsonPayload = [
          id        : 'docker',
          plugin_id : 'cd.go.docker',
          properties: [
            [
              "key"  : "DockerURI",
              "value": "http://foo"
            ]
          ]]

        when(entityHashingService.md5ForEntity(Mockito.any() as ElasticProfile)).thenReturn('some-md5')

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        assertThatResponse()
          .isOk()
          .hasEtag('"some-md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(new ElasticProfile("docker", "cd.go.docker", create("DockerURI", false, "http://foo")), ElasticProfileRepresenter)
      }

      @Test
      void 'should not create elastic profile in case of validation error and return the profile with errors'() {
        def jsonPayload = [
          id        : 'docker',
          plugin_id : 'cd.go.docker',
          properties: [
            [
              "key"  : "DockerURI",
              "value": "http://foo"
            ]
          ]]

        when(elasticProfileService.create(Mockito.any() as Username, Mockito.any() as ElasticProfile, Mockito.any() as LocalizedOperationResult))
          .then({ InvocationOnMock invocation ->
          ElasticProfile elasticProfile = invocation.getArguments()[1]
          elasticProfile.addError("plugin_id", "Plugin not installed.")
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.unprocessableEntity("validation failed")
        })

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        def expectedResponseBody = [
          message: "validation failed",
          data   : [
            id        : "docker",
            plugin_id : "cd.go.docker",
            properties: [[key: "DockerURI", value: "http://foo"]],
            errors    : [plugin_id: ["Plugin not installed."]]
          ]
        ]

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonBody(new JsonBuilder(expectedResponseBody).toString())

      }

      @Test
      void 'should not create elastic profile if one already exist with same id'() {
        def existingElasticProfile = new ElasticProfile("docker", "cd.go.docker", create("DockerURI", false, "http://foo"))
        def jsonPayload = [
          id        : 'docker',
          plugin_id : 'cd.go.docker',
          properties: [
            [
              "key"  : "DockerURI",
              "value": "http://foo"
            ]
          ]]

        when(entityHashingService.md5ForEntity(Mockito.any() as ElasticProfile)).thenReturn('some-md5')
        when(elasticProfileService.findProfile("docker")).thenReturn(existingElasticProfile)

        postWithApiHeader(controller.controllerPath(), jsonPayload)


        def expectedResponseBody = [
          message: "Failed to add elasticProfile 'docker'. Another elasticProfile with the same name already exists.",
          data   : [
            id        : "docker",
            plugin_id : "cd.go.docker",
            properties: [[key: "DockerURI", value: "http://foo"]],
            errors    : [id: ["Elastic profile ids should be unique. Elastic profile with id 'docker' already exists."]]
          ]
        ]

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonBody(new JsonBuilder(expectedResponseBody).toString())
      }
    }
  }

  @Nested
  class Update {

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

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
    class AsGroupAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupAdmin()
      }

      @Test
      void 'should update elastic profile if etag matches'() {
        def existingProfile = new ElasticProfile("docker", "cd.go.docker", create("DockerURI", false, "http://foo"))
        def updatedProfile = new ElasticProfile("docker", "cd.go.docker", create("DockerURI", false, "http://new-uri"))
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
        when(entityHashingService.md5ForEntity(updatedProfile)).thenReturn('new-md5')
        when(elasticProfileService.findProfile("docker")).thenReturn(existingProfile)

        putWithApiHeader(controller.controllerPath("/docker"), ['if-match': 'some-md5'], jsonPayload)

        assertThatResponse()
          .isOk()
          .hasEtag('"new-md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(updatedProfile, ElasticProfileRepresenter)
      }

      @Test
      void 'should not update elastic profile if etag does not match'() {
        def existingProfile = new ElasticProfile("docker", "cd.go.docker", create("DockerURI", false, "http://foo"))
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
        when(elasticProfileService.findProfile("docker")).thenReturn(existingProfile)

        putWithApiHeader(controller.controllerPath("/docker"), ['if-match': 'wrong-md5'], jsonPayload)

        assertThatResponse()
          .isPreconditionFailed()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(etagDoesNotMatch("elasticProfile", "docker"))
      }

      @Test
      void 'should return 404 if the profile does not exists'() {
        when(elasticProfileService.findProfile("docker")).thenReturn(null)
        putWithApiHeader(controller.controllerPath("/docker"), ['if-match': 'wrong-md5'], [:])

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(notFoundMessage())
      }

      @Test
      void 'should return 422 if attempted rename'() {
        def existingProfile = new ElasticProfile("docker", "cd.go.docker", create("DockerURI", false, "http://foo"))
        def jsonPayload = [
          id        : 'docker-new',
          plugin_id : 'cd.go.docker',
          properties: [
            [
              "key"  : "DockerURI",
              "value": "http://new-uri"
            ]
          ]]

        when(entityHashingService.md5ForEntity(existingProfile)).thenReturn('some-md5')
        when(elasticProfileService.findProfile("docker")).thenReturn(existingProfile)

        putWithApiHeader(controller.controllerPath("/docker"), ['if-match': 'some-md5'], jsonPayload)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(renameOfEntityIsNotSupportedMessage('elasticProfile'))
      }

      @Test
      void 'should return 422 for validation error'() {
        def existingProfile = new ElasticProfile("docker", "cd.go.docker", create("DockerURI", false, "http://foo"))
        def jsonPayload = [
          id        : 'docker',
          plugin_id : 'cd.go.docker',
          properties: [
            [
              "key"  : "DockerURI",
              "value": "http://foo"
            ]
          ]]

        when(entityHashingService.md5ForEntity(existingProfile)).thenReturn('some-md5')
        when(elasticProfileService.findProfile("docker")).thenReturn(existingProfile)

        when(elasticProfileService.update(Mockito.any() as Username, Mockito.any() as String, Mockito.any() as ElasticProfile, Mockito.any() as LocalizedOperationResult))
          .then({ InvocationOnMock invocation ->
          ElasticProfile elasticProfile = invocation.getArguments()[2]
          elasticProfile.addError("plugin_id", "Plugin not installed.")
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.unprocessableEntity("validation failed")
        })

        putWithApiHeader(controller.controllerPath("/docker"), ['if-match': 'some-md5'], jsonPayload)

        def expectedResponseBody = [
          message: "validation failed",
          data   : [
            id        : "docker",
            plugin_id : "cd.go.docker",
            properties: [[key: "DockerURI", value: "http://foo"]],
            errors    : [plugin_id: ["Plugin not installed."]]
          ]
        ]

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonBody(new JsonBuilder(expectedResponseBody).toString())
      }
    }
  }

  @Nested
  class Destroy {
    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "destroy"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath("/docker"))
      }
    }

    @Nested
    class AsGroupAdmin {
      @Test
      void 'should delete elastic profile with given id'() {
        def elasticProfile = new ElasticProfile("docker", "cd.go.docker", create("DockerURI", false, "http://foo"))

        when(elasticProfileService.findProfile("docker")).thenReturn(elasticProfile)
        when(elasticProfileService.delete(Mockito.any() as Username, Mockito.any() as ElasticProfile, Mockito.any() as HttpLocalizedOperationResult)).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.arguments.last()
          result.setMessage(LocalizedMessage.resourceDeleteSuccessful('elastic profile', elasticProfile.getId()))
        })

        deleteWithApiHeader(controller.controllerPath('/docker'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(LocalizedMessage.resourceDeleteSuccessful('elastic profile', elasticProfile.getId()))
      }

      @Test
      void 'should return 404 if elastic profile with id does not exist'() {
        when(elasticProfileService.findProfile("docker")).thenReturn(null)

        deleteWithApiHeader(controller.controllerPath('/docker'))

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(notFoundMessage())
      }

      @Test
      void 'should return validation error on failure'() {
        def elasticProfile = new ElasticProfile("docker", "cd.go.docker", create("DockerURI", false, "http://foo"))

        when(elasticProfileService.findProfile('docker')).thenReturn(elasticProfile)
        doAnswer({ InvocationOnMock invocation ->
          ((HttpLocalizedOperationResult) invocation.arguments.last()).unprocessableEntity("save failed")
        }).when(elasticProfileService).delete(any() as Username, eq(elasticProfile), any() as LocalizedOperationResult)

        deleteWithApiHeader(controller.controllerPath('/docker'))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage('save failed')
      }
    }
  }
}
