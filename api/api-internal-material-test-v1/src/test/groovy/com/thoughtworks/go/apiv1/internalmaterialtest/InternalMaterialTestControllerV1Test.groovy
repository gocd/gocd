/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.apiv1.internalmaterialtest

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthorizationHelper
import com.thoughtworks.go.config.ParamConfig
import com.thoughtworks.go.config.materials.PasswordDeserializer
import com.thoughtworks.go.config.materials.git.GitMaterial
import com.thoughtworks.go.domain.materials.MaterialConfig
import com.thoughtworks.go.domain.materials.ValidationBean
import com.thoughtworks.go.domain.materials.git.GitCommand
import com.thoughtworks.go.server.service.MaterialConfigConverter
import com.thoughtworks.go.server.service.SecretParamResolver
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.util.SystemEnvironment
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static com.thoughtworks.go.config.CaseInsensitiveString.cis
import static com.thoughtworks.go.helper.PipelineConfigMother.*
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

@MockitoSettings(strictness = Strictness.LENIENT)
class InternalMaterialTestControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalMaterialTestControllerV1> {

  @Mock PasswordDeserializer passwordDeserializer
  @Mock MaterialConfigConverter materialConfigConverter
  @Mock SystemEnvironment systemEnvironment
  @Mock GitCommand gitCommand
  @Mock SecretParamResolver secretParamResolver

  @Override
  InternalMaterialTestControllerV1 createControllerInstance() {
    new InternalMaterialTestControllerV1(new ApiAuthorizationHelper(securityService, goConfigService), goConfigService, passwordDeserializer, materialConfigConverter, systemEnvironment, secretParamResolver)
  }

  @Nested
  class TestConnection {

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {
      @Delegate SecurityServiceTrait s = InternalMaterialTestControllerV1Test.this
      @Delegate ControllerTrait<InternalMaterialTestControllerV1> c = InternalMaterialTestControllerV1Test.this

      @Override
      String getControllerMethodUnderTest() {
        return "testConnection"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(Map.of("group_name", getGroupName())), [:])
      }

      @Override
      PipelineSpecifier getPipelineSpecifier() {
        new PipelineSpecifier(groupName: 'some-group')
      }
    }

    @Nested
    class AsGroupAdmin {
      String groupName = "some-pipeline-group"

      @BeforeEach
      void setUp() {
        loginAsGroupAdmin(groupName: groupName)
      }

      @Test
      void 'should be forbidden without group_name or pipeline_name'() {
        postWithApiHeader(controller.controllerBasePath(), [
          "type"      : "git",
          "attributes": [
            "url": "some-url"
          ]
        ])

        assertThatResponse()
          .hasJsonMessage("You are not authorized to perform this action.")
          .isForbidden()
      }

      @Test
      void 'should render error when authorized via pipeline_name without group_name'() {
        String pipelineName = "some-pipeline-name"
        loginAsGroupAdmin(pipelineName: pipelineName)

        postWithApiHeader(controller.controllerPath(Map.of("pipeline_name", pipelineName)), [
          "type"      : "git",
          "attributes": [
            "url": "some-url"
          ]
        ])

        assertThatResponse()
          .hasJsonMessage("Request is missing parameter `group_name`")
          .isBadRequest()
      }

      @Test
      void 'should render error if material type is invalid'() {
        postWithApiHeader(controller.controllerPath(Map.of("group_name", groupName)), ["type": "some-random-type"])
        assertThatResponse()
          .hasJsonMessage("Your request could not be processed. Invalid material type 'some-random-type'. It has to be one of [git, hg, svn, p4, tfs, dependency, package, plugin].")
          .isUnprocessableEntity()
      }

      @Test
      void 'should render error if material type does not support check connection functionality'() {
        postWithApiHeader(controller.controllerPath(Map.of("group_name", groupName)), ["type": "dependency"])
        assertThatResponse()
          .hasJsonMessage("Your request could not be processed. The material of type 'dependency' does not support connection testing.")
          .isUnprocessableEntity()
      }

      @Test
      void 'should render error if material is not valid'() {
        postWithApiHeader(controller.controllerPath(Map.of("group_name", groupName)), [
          "type"      : "git",
          "attributes": [
            "url"               : "",
            "encrypted_password": "encrypted-password",
            "password"          : "password"
          ]
        ])

        def expectedJSON = [
          "message": "There was an error with the material configuration.\n- url: URL cannot be blank",
          "data"   : [
            "errors"    : [
              "url": ["URL cannot be blank"]
            ],
            "type"      : "git",
            "attributes": [
              "url"             : "",
              "destination"     : null,
              "filter"          : null,
              "invert_filter"   : false,
              "name"            : null,
              "auto_update"     : true,
              "branch"          : "master",
              "submodule_folder": null,
              "shallow_clone"   : false
            ]
          ]
        ]

        assertThatResponse()
          .hasJsonBody(expectedJSON)
          .isUnprocessableEntity()
      }

      @Test
      void 'should render error if cannot connect to material'() {
        def material = mock(GitMaterial.class)
        def validationBean = ValidationBean.notValid("some-error")
        when(material.checkConnection(any())).thenReturn(validationBean)
        when(materialConfigConverter.toMaterial(any(MaterialConfig.class))).thenReturn(material)

        postWithApiHeader(controller.controllerPath(Map.of("group_name", groupName)), [
          "type"      : "git",
          "attributes": [
            "url": "some-url"
          ]
        ])

        assertThatResponse()
          .hasJsonMessage("some-error")
          .isUnprocessableEntity()
      }

      @Test
      void 'should render success response if can connect to material'() {
        def material = mock(GitMaterial.class)
        def validationBean = ValidationBean.valid()
        when(material.checkConnection(any())).thenReturn(validationBean)
        when(materialConfigConverter.toMaterial(any(MaterialConfig.class))).thenReturn(material)

        postWithApiHeader(controller.controllerPath(Map.of("group_name", groupName)), [
          "type"      : "git",
          "attributes": [
            "url": "some-url"
          ]
        ])

        assertThatResponse()
          .hasJsonMessage("Connection OK.")
          .isOk()
      }

      @Test
      void 'should resolve secrets before connecting to scm material'() {
        def material = mock(GitMaterial.class)
        def validationBean = ValidationBean.valid()
        when(material.checkConnection(any())).thenReturn(validationBean)
        when(materialConfigConverter.toMaterial(any(MaterialConfig.class))).thenReturn(material)

        postWithApiHeader(controller.controllerPath(Map.of("group_name", groupName)), [
          "type"      : "git",
          "attributes": [
            "url": "some-url"
          ]
        ])

        assertThatResponse()
          .hasJsonMessage("Connection OK.")
          .isOk()

        def mockOrders = inOrder(secretParamResolver, material)

        mockOrders.verify(secretParamResolver).resolve(eq(material), eq(Optional.of("some-pipeline-group")))
        mockOrders.verify(material).checkConnection(any())
      }

      @Test
      void 'should not resolve params when pipeline_name is not provided'() {
        def material = mock(GitMaterial.class)
        def validationBean = ValidationBean.valid()
        when(material.checkConnection(any())).thenReturn(validationBean)
        when(materialConfigConverter.toMaterial(any(MaterialConfig.class))).thenReturn(material)

        postWithApiHeader(controller.controllerPath(Map.of("group_name", groupName)), [
          "type"         : "git",
          "attributes"   : [
            "url": "http://some-url/val/#{test-param}"
          ]
        ])

        assertThatResponse()
          .hasJsonBody(["message": "Connection OK."])
          .isOk()

        def captor = ArgumentCaptor.forClass(MaterialConfig.class)
        verify(materialConfigConverter).toMaterial(captor.capture())
        assertEquals("http://some-url/val/#{test-param}", captor.value.getUriForDisplay())
      }

      @Test
      void 'should resolve params when pipeline_name is also provided'() {
        def pipelineName = "some-pipeline-name"
        loginAsGroupAdmin(groupName: groupName, pipelineName: pipelineName) // need to mock access to both

        def material = mock(GitMaterial.class)
        def validationBean = ValidationBean.valid()
        when(material.checkConnection(any())).thenReturn(validationBean)
        when(materialConfigConverter.toMaterial(any(MaterialConfig.class))).thenReturn(material)

        def pipelineConfig = pipelineConfig(pipelineName)
        pipelineConfig.addParam(new ParamConfig("test-param", "param-value"))
        when(goConfigService.findGroupByPipelineOptional(cis(pipelineName))).thenReturn(Optional.of(createGroup(groupName, pipelineConfig)))

        postWithApiHeader(controller.controllerPath(Map.of("group_name", groupName, "pipeline_name", pipelineName)), [
          "type"         : "git",
          "attributes"   : [
            "url": "http://some-url/val/#{test-param}"
          ]
        ])

        assertThatResponse()
          .hasJsonBody(["message": "Connection OK."])
          .isOk()

        verify(goConfigService).findGroupByPipelineOptional(cis(pipelineName))

        def captor = ArgumentCaptor.forClass(MaterialConfig.class)
        verify(materialConfigConverter).toMaterial(captor.capture())
        assertEquals("http://some-url/val/param-value", captor.value.getUriForDisplay())
      }

      @Test
      void 'should render error if pipeline is not part of authorized group'() {
        disableSecurity() // testing conservative fallback logic if not already validated in before()

        def pipelineName = "other-random-pipeline"
        def material = mock(GitMaterial.class)
        def validationBean = ValidationBean.notValid("some-error")
        when(material.checkConnection(any())).thenReturn(validationBean)
        when(materialConfigConverter.toMaterial(any(MaterialConfig.class))).thenReturn(material)
        when(goConfigService.findGroupByPipelineOptional(any()))
          .thenReturn(Optional.of(createGroup("unauthorized-group", createPipelineConfig(pipelineName, "someStage", "someJob"))))

        postWithApiHeader(controller.controllerPath(Map.of("group_name", groupName, "pipeline_name", pipelineName)), [
          "type"      : "git",
          "attributes": [
            "url": "some-url"
          ]
        ])

        assertThatResponse()
          .hasJsonMessage("Either the resource you requested was not found, or you are not authorized to perform this action.")
          .isNotFound()
      }
    }
  }
}
