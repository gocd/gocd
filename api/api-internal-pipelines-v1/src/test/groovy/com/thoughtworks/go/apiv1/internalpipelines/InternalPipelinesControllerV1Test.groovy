/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.internalpipelines

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.internalpipelines.representers.PipelineConfigsWithMinimalAttributesRepresenter
import com.thoughtworks.go.domain.PipelineGroups
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.PipelineConfigService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import spark.Response

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalPipelinesControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalPipelinesControllerV1> {

  @Override
  InternalPipelinesControllerV1 createControllerInstance() {
    new InternalPipelinesControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), pipelineConfigService, entityHashingService)
  }

  @Mock
  PipelineConfigService pipelineConfigService

  @Mock
  EntityHashingService entityHashingService

  @Mock
  private Response response

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Nested
  class Security implements SecurityTestTrait, GroupAdminUserSecurity {

    @Override
    String getControllerMethodUnderTest() {
      return "index"
    }

    @Override
    void makeHttpCall() {
      get(controller.controllerPath())
    }
  }

  @Nested
  class Index {

    @BeforeEach
    void setUp() {
      enableSecurity()
      loginAsAdmin()
    }

    @Test
    void 'should fetch all the pipelines for the admin user'() {
      def pipelineConfigs = PipelineConfigMother.createGroups("group1", "group2")
      when(pipelineConfigService.viewableOrOperatableGroupsFor(any() as Username)).thenReturn(pipelineConfigs)
      when(entityHashingService.md5ForEntity(any() as PipelineGroups)).thenReturn("md5")

      getWithApiHeader(controller.controllerPath())

      assertThatResponse()
        .isOk()
        .hasEtag('"md5"')
        .hasBodyWithJsonObject(pipelineConfigs, PipelineConfigsWithMinimalAttributesRepresenter)
    }

    @Test
    void 'should return 304 if pipelines are not modified'() {
      def pipelineConfigs = PipelineConfigMother.createGroups("group1", "group2")
      when(pipelineConfigService.viewableOrOperatableGroupsFor(any() as Username)).thenReturn(pipelineConfigs)
      when(entityHashingService.md5ForEntity(any() as PipelineGroups)).thenReturn("md5")

      getWithApiHeader(controller.controllerPath(), ['if-none-match': '"md5"'])

      assertThatResponse()
        .isNotModified()
    }

  }
}
