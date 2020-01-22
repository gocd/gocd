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
package com.thoughtworks.go.apiv3.elasticprofile

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv3.elasticprofile.representers.ElasticProfileRepresenter
import com.thoughtworks.go.apiv3.elasticprofile.representers.ElasticProfilesRepresenter
import com.thoughtworks.go.config.elastic.ClusterProfile
import com.thoughtworks.go.config.elastic.ElasticProfile
import com.thoughtworks.go.config.elastic.ElasticProfiles
import com.thoughtworks.go.i18n.LocalizedMessage
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.ClusterProfilesService
import com.thoughtworks.go.server.service.ElasticProfileService
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.server.service.result.LocalizedOperationResult
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
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.doAnswer
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class ElasticProfileControllerV3Test implements SecurityServiceTrait, ControllerTrait<ElasticProfileControllerV3> {
  @Mock
  private ElasticProfileService elasticProfileService

  @Mock
  private EntityHashingService entityHashingService

  @Mock
  private ClusterProfilesService clusterProfileService

  @BeforeEach
  void setup() {
    initMocks(this)
  }

  @Override
  ElasticProfileControllerV3 createControllerInstance() {
    return new ElasticProfileControllerV3(elasticProfileService, new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService, clusterProfileService)
  }

  @Nested
  class Index {

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
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
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should list all elastic profiles'() {
        def elasticProfiles = new ElasticProfiles(
          new ElasticProfile("docker", "prod-cluster", create("docker-uri", false, "unix:///var/run/docker")),
          new ElasticProfile("ecs", "prod-cluster", create("ACCESS_KEY", true, "encrypted-key")),
          new ElasticProfile("k8s", "prod-cluster", create("cluster-uri", false, "https://foo.bar"))
        )

        when(elasticProfileService.getPluginProfiles()).thenReturn(elasticProfiles)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(ElasticProfilesRepresenter, elasticProfiles, { id, clusterId -> true })
      }
    }
  }

  @Nested
  class Show {
    @BeforeEach
    void setUp() {
      when(elasticProfileService.findProfile('docker')).thenReturn(new ElasticProfile("docker", "prod-cluster"))
    }

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
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
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should return elastic profile of specified id'() {
        def dockerElasticProfile = new ElasticProfile("docker", "prod-cluster", create("docker-uri", false, "unix:///var/run/docker"))

        when(entityHashingService.md5ForEntity(dockerElasticProfile)).thenReturn('md5')
        when(elasticProfileService.findProfile('docker')).thenReturn(dockerElasticProfile)

        getWithApiHeader(controller.controllerPath('/docker'))

        assertThatResponse()
          .isOk()
          .hasEtag('"md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(ElasticProfileRepresenter, dockerElasticProfile, true)
      }

      @Test
      void 'should return 404 if elastic profile with id does not exist'() {
        when(elasticProfileService.findProfile('docker')).thenReturn(null)

        getWithApiHeader(controller.controllerPath('/docker'))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("docker"))
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 304 if elastic profile is not modified'() {
        def dockerElasticProfile = new ElasticProfile("docker", "prod-cluster", create("docker-uri", false, "unix:///var/run/docker"))

        when(entityHashingService.md5ForEntity(dockerElasticProfile)).thenReturn('md5')
        when(elasticProfileService.findProfile('docker')).thenReturn(dockerElasticProfile)

        getWithApiHeader(controller.controllerPath('/docker'), ['if-none-match': '"md5"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 200 with elastic profile if etag does not match'() {
        def dockerElasticProfile = new ElasticProfile("docker", "prod-cluster", create("docker-uri", false, "unix:///var/run/docker"))

        when(entityHashingService.md5ForEntity(dockerElasticProfile)).thenReturn('md5-new')
        when(elasticProfileService.findProfile('docker')).thenReturn(dockerElasticProfile)

        getWithApiHeader(controller.controllerPath('/docker'), ['if-none-match': '"md5"'])

        assertThatResponse()
          .isOk()
          .hasEtag('"md5-new"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(ElasticProfileRepresenter, dockerElasticProfile, true)
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
        postWithApiHeader(controller.controllerPath(), [id: 'some-id', 'cluster_profile_id': 'clusteer-id'])
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
      void 'should create elastic profile from given json payload'() {
        def jsonPayload = [
          id                : 'docker',
          cluster_profile_id: "prod-cluster",
          properties        : [
            [
              "key"  : "DockerURI",
              "value": "http://foo"
            ]
          ]]

        when(clusterProfileService.findProfile("prod-cluster")).thenReturn(new ClusterProfile("prod-cluster", "cd.go.docker"))
        when(entityHashingService.md5ForEntity(Mockito.any() as ElasticProfile)).thenReturn('some-md5')

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        assertThatResponse()
          .isOk()
          .hasEtag('"some-md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(ElasticProfileRepresenter, new ElasticProfile("docker", "prod-cluster", create("DockerURI", false, "http://foo")), true)
      }

      @Test
      void 'should not create elastic profile from given json payload when specified cluster profile does not exists'() {
        def jsonPayload = [
          id                : 'docker',
          cluster_profile_id: "prod-cluster",
          properties        : [
            [
              "key"  : "DockerURI",
              "value": "http://foo"
            ]
          ]]

        when(entityHashingService.md5ForEntity(Mockito.any() as ElasticProfile)).thenReturn('some-md5')

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("No Cluster Profile exists with the specified cluster_profile_id 'prod-cluster'.")
      }

      @Test
      void 'should not create elastic profile in case of validation error and return the profile with errors'() {
        def jsonPayload = [
          id                : 'docker',
          cluster_profile_id: "prod-cluster",
          properties        : [
            [
              "key"  : "DockerURI",
              "value": "http://foo"
            ]
          ]]

        when(clusterProfileService.findProfile("prod-cluster")).thenReturn(new ClusterProfile("prod-cluster", "cd.go.docker"))
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
            id                : "docker",
            cluster_profile_id: "prod-cluster",
            can_administer    : true,
            properties        : [[key: "DockerURI", value: "http://foo"]],
            errors            : [plugin_id: ["Plugin not installed."]]
          ]
        ]

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonBody(new JsonBuilder(expectedResponseBody).toString())
      }

      @Test
      void 'should not create elastic profile if one already exist with same id'() {
        def existingElasticProfile = new ElasticProfile("docker", 'foo', create("DockerURI", false, "http://foo"))
        def jsonPayload = [
          id                : 'docker',
          cluster_profile_id: "prod-cluster",
          properties        : [
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
            id                : "docker",
            cluster_profile_id: "prod-cluster",
            can_administer    : true,
            properties        : [[key: "DockerURI", value: "http://foo"]],
            errors            : [id: ["Elastic profile ids should be unique. Elastic profile with id 'docker' already exists."]]
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
    @BeforeEach
    void setUp() {
      when(elasticProfileService.findProfile('docker')).thenReturn(new ElasticProfile("docker", "prod-cluster"))
    }

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
      void 'should update elastic profile if etag matches'() {
        def existingProfile = new ElasticProfile("docker", "prod-cluster", create("DockerURI", false, "http://foo"))
        def updatedProfile = new ElasticProfile("docker", "prod-cluster", create("DockerURI", false, "http://new-uri"))
        def jsonPayload = [
          id                : 'docker',
          cluster_profile_id: "prod-cluster",
          properties        : [
            [
              "key"  : "DockerURI",
              "value": "http://new-uri"
            ]
          ]]

        when(clusterProfileService.findProfile("prod-cluster")).thenReturn(new ClusterProfile("prod-cluster", "cd.go.docker"))
        when(entityHashingService.md5ForEntity(existingProfile)).thenReturn('some-md5')
        when(entityHashingService.md5ForEntity(updatedProfile)).thenReturn('new-md5')
        when(elasticProfileService.findProfile("docker")).thenReturn(existingProfile)

        putWithApiHeader(controller.controllerPath("/docker"), ['if-match': 'some-md5'], jsonPayload)

        assertThatResponse()
          .isOk()
          .hasEtag('"new-md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(ElasticProfileRepresenter, updatedProfile, true)
      }

      @Test
      void 'should not update elastic profile when specified cluster profile does not exists'() {
        def existingProfile = new ElasticProfile("docker", "prod-cluster", create("DockerURI", false, "http://foo"))
        def updatedProfile = new ElasticProfile("docker", "prod-cluster", create("DockerURI", false, "http://new-uri"))
        def jsonPayload = [
          id                : 'docker',
          cluster_profile_id: "prod-cluster",
          properties        : [
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
          .isUnprocessableEntity()
          .hasJsonMessage("No Cluster Profile exists with the specified cluster_profile_id 'prod-cluster'.")
      }

      @Test
      void 'should not update elastic profile if etag does not match'() {
        def existingProfile = new ElasticProfile("docker", "prod-cluster", create("DockerURI", false, "http://foo"))
        def jsonPayload = [
          id                : 'docker',
          cluster_profile_id: "prod-cluster",
          properties        : [
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
          .hasJsonMessage(controller.entityType.notFoundMessage("docker"))
      }

      @Test
      void 'should return 422 if attempted rename'() {
        def existingProfile = new ElasticProfile("docker", "prod-cluster", create("DockerURI", false, "http://foo"))
        def jsonPayload = [
          id                : 'docker-new',
          cluster_profile_id: "prod-cluster",
          properties        : [
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
        def existingProfile = new ElasticProfile("docker", "prod-cluster", create("DockerURI", false, "http://foo"))
        def jsonPayload = [
          id                : 'docker',
          cluster_profile_id: "prod-cluster",
          properties        : [
            [
              "key"  : "DockerURI",
              "value": "http://foo"
            ]
          ]]

        when(clusterProfileService.findProfile("prod-cluster")).thenReturn(new ClusterProfile("prod-cluster", "cd.go.docker"))
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
            id                : "docker",
            cluster_profile_id: "prod-cluster",
            can_administer    : true,
            properties        : [[key: "DockerURI", value: "http://foo"]],
            errors            : [plugin_id: ["Plugin not installed."]]
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
    @BeforeEach
    void setUp() {
      when(elasticProfileService.findProfile('docker')).thenReturn(new ElasticProfile("docker", "prod-cluster"))
    }

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "destroy"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath("/docker"), [id: 'some-id'])
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
      void 'should delete elastic profile with given id'() {
        def elasticProfile = new ElasticProfile("docker", "prod-cluster", create("DockerURI", false, "http://foo"))

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
          .hasJsonMessage(controller.entityType.notFoundMessage("docker"))
      }

      @Test
      void 'should return validation error on failure'() {
        def elasticProfile = new ElasticProfile("docker", "prod-cluster", create("DockerURI", false, "http://foo"))

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
