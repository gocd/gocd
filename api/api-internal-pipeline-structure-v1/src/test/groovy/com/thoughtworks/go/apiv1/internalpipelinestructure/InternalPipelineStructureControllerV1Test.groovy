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

package com.thoughtworks.go.apiv1.internalpipelinestructure

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.internalpipelinestructure.representers.InternalPipelineStructuresRepresenter
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.server.service.PipelineConfigService
import com.thoughtworks.go.spark.AdminUserOnlyIfSecurityEnabled
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito

import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalPipelineStructureControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalPipelineStructureControllerV1> {

  @Mock
  PipelineConfigService pipelineConfigService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalPipelineStructureControllerV1 createControllerInstance() {
    new InternalPipelineStructureControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), pipelineConfigService)
  }

  @Nested
  class Index {
    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Test
    void 'test should render list of all pipeline groups'() {
      def group = PipelineConfigMother.createGroup("my-group", PipelineConfigMother.createPipelineConfig("my-pipeline", "my-stage", "my-job1", "my-job2"))
      when(pipelineConfigService.viewableGroupsFor(currentUsername())).thenReturn([group])

      getWithApiHeader(controller.controllerBasePath())

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonArray([group], InternalPipelineStructuresRepresenter.class)
    }

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
  }
}
