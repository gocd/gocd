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
package com.thoughtworks.go.apiv3.dashboard

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv3.dashboard.representers.DashboardFor
import com.thoughtworks.go.apiv3.dashboard.representers.DashboardRepresenter
import com.thoughtworks.go.config.security.Permissions
import com.thoughtworks.go.config.security.permissions.EveryonePermission
import com.thoughtworks.go.config.security.users.Everyone
import com.thoughtworks.go.server.dashboard.GoDashboardEnvironment
import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup
import com.thoughtworks.go.server.domain.user.Filters
import com.thoughtworks.go.server.domain.user.PipelineSelections
import com.thoughtworks.go.server.service.GoDashboardService
import com.thoughtworks.go.server.service.PipelineSelectionsService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.apache.commons.codec.digest.DigestUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class DashboardControllerV3Test implements SecurityServiceTrait, ControllerTrait<DashboardControllerV3> {

  @Mock
  private GoDashboardService goDashboardService

  @Mock
  private PipelineSelectionsService pipelineSelectionsService

  @BeforeEach
  void setup() {
    initMocks(this)

  }

  @Override
  DashboardControllerV3 createControllerInstance() {
    new DashboardControllerV3(new ApiAuthenticationHelper(securityService, goConfigService), pipelineSelectionsService, goDashboardService)
  }

  @Nested
  class Dashboard {

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

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
    class AsAuthorizedUser {
      @Test
      void 'should get dashboard json'() {
        loginAsUser()

        def group = pipelineGroup('group1')
        def env = environment('env1')

        when(pipelineSelectionsService.load((String) isNull(), any(Long.class))).thenReturn(PipelineSelections.ALL)
        when(goDashboardService.hasEverLoadedCurrentState()).thenReturn(true)
        when(goDashboardService.allPipelineGroupsForDashboard(eq(Filters.WILDCARD_FILTER), eq(currentUsername()))).thenReturn([group])
        when(goDashboardService.allEnvironmentsForDashboard(eq(Filters.WILDCARD_FILTER), eq(currentUsername()))).thenReturn([env])

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(new DashboardFor([group], [env], currentUsername(), PipelineSelections.ALL.etag()), DashboardRepresenter)
      }

      @Test
      void 'should render 304 if content matches'() {
        loginAsUser()

        def group = pipelineGroup('group1')
        def env = environment('env1')

        when(pipelineSelectionsService.load((String) isNull(), any(Long.class))).thenReturn(PipelineSelections.ALL)
        when(goDashboardService.hasEverLoadedCurrentState()).thenReturn(true)
        when(goDashboardService.allPipelineGroupsForDashboard(eq(Filters.WILDCARD_FILTER), eq(currentUsername()))).thenReturn([group])
        when(goDashboardService.allEnvironmentsForDashboard(eq(Filters.WILDCARD_FILTER), eq(currentUsername()))).thenReturn([env])

        def etag = computeEtag([group], [env])
        getWithApiHeader(controller.controllerBasePath(), ['if-none-match': etag])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
          .hasNoBody()
      }

      @Test
      void 'should get empty json when dashboard is empty'() {
        def pipelineSelections = PipelineSelections.ALL
        when(pipelineSelectionsService.load((String) isNull(), any(Long.class))).thenReturn(pipelineSelections)
        when(goDashboardService.allPipelineGroupsForDashboard(eq(Filters.WILDCARD_FILTER), eq(currentUsername()))).thenReturn([])
        when(goDashboardService.allEnvironmentsForDashboard(eq(Filters.WILDCARD_FILTER), eq(currentUsername()))).thenReturn([])
        when(goDashboardService.hasEverLoadedCurrentState()).thenReturn(true)

        loginAsUser()
        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(new DashboardFor([], [], currentUsername(), pipelineSelections.etag()), DashboardRepresenter)
      }

      @Test
      void 'should return 202 no content when dashboard is not processed (on server start)'() {
        when(goDashboardService.hasEverLoadedCurrentState()).thenReturn(false)

        loginAsUser()
        getWithApiHeader(controller.controllerPath())

        verify(goDashboardService).hasEverLoadedCurrentState()
        verifyNoMoreInteractions(pipelineSelectionsService, goDashboardService)

        assertThatResponse()
          .isAccepted()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Dashboard is being processed, this may take a few seconds. Please check back later.")
      }
    }

    @Nested
    class Etag {
      @Test
      void 'should account for current user when computing etag'() {
        loginAsUser()

        def pipelineSelections = PipelineSelections.ALL
        def pipelineGroup = new GoDashboardPipelineGroup('group1', new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, EveryonePermission.INSTANCE), true)
        pipelineGroup.addPipeline(GoDashboardPipelineMother.dashboardPipeline('pipeline1'))
        pipelineGroup.addPipeline(GoDashboardPipelineMother.dashboardPipeline('pipeline2'))
        when(pipelineSelectionsService.load((String) isNull(), any(Long.class))).thenReturn(pipelineSelections)
        when(goDashboardService.hasEverLoadedCurrentState()).thenReturn(true)
        def pipelineGroups = [pipelineGroup]
        when(goDashboardService.allPipelineGroupsForDashboard(eq(Filters.WILDCARD_FILTER), eq(currentUsername()))).thenReturn(pipelineGroups)

        String etag = computeEtag(pipelineGroups, [])
        getWithApiHeader(controller.controllerBasePath(), ['if-none-match': etag])
        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
          .hasNoBody()

        loginAsPipelineViewUser()

        when(goDashboardService.allPipelineGroupsForDashboard(eq(Filters.WILDCARD_FILTER), eq(currentUsername()))).thenReturn(pipelineGroups)
        getWithApiHeader(controller.controllerBasePath(), ['if-none-match': etag])
        assertThatResponse()
          .isOk()

      }
    }
  }

  private static GoDashboardPipelineGroup pipelineGroup(String name) {
    GoDashboardPipelineGroup pipelineGroup = new GoDashboardPipelineGroup(name, permissions(), true)
    pipelineGroup.addPipeline(GoDashboardPipelineMother.dashboardPipeline('pipeline1'))
    pipelineGroup.addPipeline(GoDashboardPipelineMother.dashboardPipeline('pipeline2'))
    return pipelineGroup
  }

  private static GoDashboardEnvironment environment(String name) {
    GoDashboardEnvironment environment = new GoDashboardEnvironment(name, Everyone.INSTANCE, true)
    environment.addPipeline(GoDashboardPipelineMother.dashboardPipeline('pipeline1'))
    return environment
  }

  private static Permissions permissions() {
    new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, EveryonePermission.INSTANCE)
  }

  private String computeEtag(List<GoDashboardPipelineGroup> pipelineGroups, List<GoDashboardEnvironment> envs) {
    '"' + DigestUtils.md5Hex([
      currentUsernameString(),
      pipelineGroups.collect { it.etag() }.join("/"),
      envs.collect { it.etag() }.join("/")
    ].join('/')) + '"'
  }
}
