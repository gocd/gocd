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

package com.thoughtworks.go.apiv1.internalpipelinegroups

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.internalpipelinegroups.models.PipelineGroupsViewModel
import com.thoughtworks.go.apiv1.internalpipelinegroups.representers.InternalPipelineGroupsRepresenter
import com.thoughtworks.go.config.BasicCruiseConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.EnvironmentsConfig
import com.thoughtworks.go.config.PipelineConfigs
import com.thoughtworks.go.domain.PipelineGroups
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.server.service.EnvironmentConfigService
import com.thoughtworks.go.server.service.PipelineConfigService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.util.Node
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalPipelineGroupsControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalPipelineGroupsControllerV1> {

  @Mock
  PipelineConfigService pipelineConfigService
  @Mock
  EnvironmentConfigService environmentConfigService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalPipelineGroupsControllerV1 createControllerInstance() {
    new InternalPipelineGroupsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService),
      pipelineConfigService, environmentConfigService)
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

  @Nested
  class Index {
    private Hashtable<CaseInsensitiveString, Node> hashtable

    @BeforeEach
    void setUp() {
      loginAsUser()

      hashtable = new Hashtable<CaseInsensitiveString, Node>()
      def cruiseConfig = mock(BasicCruiseConfig.class)
      when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig)
      when(cruiseConfig.getDependencyTable()).thenReturn(hashtable)
    }

    @Test
    void 'should render list of all pipeline groups'() {
      PipelineConfigs group = PipelineConfigMother.createGroup("my-group",
        PipelineConfigMother.createPipelineConfig("my-pipeline", "my-stage", "my-job1", "my-job2"))

      when(pipelineConfigService.viewableGroupsForUserIncludingConfigRepos(currentUsername())).thenReturn(new PipelineGroups([group]))

      getWithApiHeader(controller.controllerBasePath())

      def pipelineGroupsViewModel = new PipelineGroupsViewModel(new PipelineGroups([group]), new EnvironmentsConfig())

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(InternalPipelineGroupsRepresenter.class, pipelineGroupsViewModel)
    }
  }
}
