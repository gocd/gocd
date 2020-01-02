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
package com.thoughtworks.go.spark.spa

import com.thoughtworks.go.config.Authorization
import com.thoughtworks.go.config.BasicPipelineConfigs
import com.thoughtworks.go.config.PipelineConfigs
import com.thoughtworks.go.domain.PipelineGroups
import com.thoughtworks.go.server.service.PipelineConfigService
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService
import com.thoughtworks.go.server.service.support.toggle.Toggles
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class NewDashboardControllerTest implements ControllerTrait<NewDashboardController>, SecurityServiceTrait {
  PipelineConfigService pipelineConfigService = mock(PipelineConfigService.class)

  @Override
  NewDashboardController createControllerInstance() {
    return new NewDashboardController(new SPAAuthenticationHelper(securityService, goConfigService), templateEngine, securityService, systemEnvironment, pipelineConfigService)
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
        get(controller.controllerPath())
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
        Toggles.initializeWith(mock(FeatureToggleService.class))
      }

      @Test
      void shouldRedirectToPipelineCreationIfNoPipelinesExist() {
        when(pipelineConfigService.viewableGroupsFor(currentUsername())).thenReturn(new PipelineGroups())
        when(securityService.canCreatePipelines(currentUsername())).thenReturn(true)

        get(controller.controllerPath())

        assertThatResponse()
          .redirectsTo("/go/admin/pipelines/create?group=defaultGroup")
      }

      @Test
      void shouldNotRedirectToPipelineCreationIfUserDoesNotHavePermissions() {
        when(pipelineConfigService.viewableGroupsFor(currentUsername())).thenReturn(new PipelineGroups())
        when(securityService.canCreatePipelines(currentUsername())).thenReturn(false)

        get(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasNoRedirectUrlSet()
      }

      @Test
      void shouldNotRedirectToPipelineCreationIfPipelineGroupsArePresent() {
        def pipelineConfigs = new PipelineGroups()
        pipelineConfigs.add(new BasicPipelineConfigs("group1", new Authorization()))

        when(pipelineConfigService.viewableGroupsFor(currentUsername())).thenReturn(pipelineConfigs)

        get(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasNoRedirectUrlSet()
      }
    }
  }

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

}
