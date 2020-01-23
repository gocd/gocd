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
package com.thoughtworks.go.apiv1.internalpipelinestructure

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.internalpipelinestructure.representers.InternalPipelineStructuresRepresenter
import com.thoughtworks.go.config.PipelineConfigs
import com.thoughtworks.go.config.TemplatesConfig
import com.thoughtworks.go.domain.PipelineGroups
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.helper.PipelineTemplateConfigMother
import com.thoughtworks.go.server.service.PipelineConfigService
import com.thoughtworks.go.server.service.TemplateConfigService
import com.thoughtworks.go.server.service.UserService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static java.util.Arrays.asList
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalPipelineStructureControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalPipelineStructureControllerV1> {

  @Mock
  PipelineConfigService pipelineConfigService
  @Mock
  TemplateConfigService templateConfigService
  @Mock
  private UserService userService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalPipelineStructureControllerV1 createControllerInstance() {
    new InternalPipelineStructureControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), pipelineConfigService, templateConfigService, userService)
  }

  @Nested
  class Index {
    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Test
    void 'test should render list of all pipeline groups'() {
      PipelineConfigs group = PipelineConfigMother.createGroup("my-group", PipelineConfigMother.createPipelineConfig("my-pipeline", "my-stage", "my-job1", "my-job2"))
      def template = PipelineTemplateConfigMother.createTemplate("my-template")
      when(pipelineConfigService.viewableGroupsForUserIncludingConfigRepos(currentUsername())).thenReturn(new PipelineGroups([group]))
      when(templateConfigService.templateConfigsThatCanBeViewedBy(currentUsername())).thenReturn(new TemplatesConfig(template))

      getWithApiHeader(controller.controllerBasePath())

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(InternalPipelineStructuresRepresenter.class, new PipelineGroups([group]), new TemplatesConfig(template))
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

    @Test
    void 'should render list of pipeline grps with roles and users'() {
      PipelineConfigs group = PipelineConfigMother.createGroup("my-group", PipelineConfigMother.createPipelineConfig("my-pipeline", "my-stage", "my-job1", "my-job2"))
      def template = PipelineTemplateConfigMother.createTemplate("my-template")
      def groups = new PipelineGroups([group])
      def templateConfigs = new TemplatesConfig(template)
      def users = Set.of('user1', 'user2')
      def roles = asList('role1', 'role2')

      when(pipelineConfigService.viewableGroupsForUserIncludingConfigRepos(currentUsername())).thenReturn(groups)
      when(templateConfigService.templateConfigsThatCanBeViewedBy(currentUsername())).thenReturn(templateConfigs)
      when(userService.allUsernames()).thenReturn(users)
      when(userService.allRoleNames()).thenReturn(roles)

      getWithApiHeader(controller.controllerBasePath() + '?with_additional_info=true')

      def expectedJSON = toObjectString({
        InternalPipelineStructuresRepresenter.toJSON(it, groups, templateConfigs, users, roles)
      })

      assertThatResponse()
        .isOk()
        .hasJsonBody(expectedJSON)
    }
  }
}
