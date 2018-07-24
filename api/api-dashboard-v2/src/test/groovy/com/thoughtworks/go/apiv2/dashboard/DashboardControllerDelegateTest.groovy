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

package com.thoughtworks.go.apiv2.dashboard

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv2.dashboard.representers.DashboardFor
import com.thoughtworks.go.apiv2.dashboard.representers.PipelineGroupsRepresenter
import com.thoughtworks.go.config.security.Permissions
import com.thoughtworks.go.config.security.users.Everyone
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

import static com.thoughtworks.go.apiv2.dashboard.GoDashboardPipelineMother.dashboardPipeline
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class DashboardControllerDelegateTest implements SecurityServiceTrait, ControllerTrait<DashboardControllerDelegate> {

  @Mock
  private GoDashboardService goDashboardService

  @Mock
  private PipelineSelectionsService pipelineSelectionsService

  @BeforeEach
  void setup() {
    initMocks(this)

  }

  @Override
  DashboardControllerDelegate createControllerInstance() {
    new DashboardControllerDelegate(new ApiAuthenticationHelper(securityService, goConfigService), pipelineSelectionsService, goDashboardService)
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

        def pipelineSelections = PipelineSelections.ALL
        def pipelineGroup = new GoDashboardPipelineGroup('group1', new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE))
        pipelineGroup.addPipeline(dashboardPipeline('pipeline1'))
        pipelineGroup.addPipeline(dashboardPipeline('pipeline2'))
        when(pipelineSelectionsService.load((String) isNull(), any(Long.class))).thenReturn(pipelineSelections)
        when(goDashboardService.hasEverLoadedCurrentState()).thenReturn(true)
        when(goDashboardService.allPipelineGroupsForDashboard(eq(Filters.WILDCARD_FILTER), eq(currentUsername()))).thenReturn([pipelineGroup])

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(new DashboardFor([pipelineGroup], currentUsername()), PipelineGroupsRepresenter)
      }

      @Test
      void 'should render 304 if content matches'() {
        loginAsUser()

        def pipelineSelections = PipelineSelections.ALL
        def pipelineGroup = new GoDashboardPipelineGroup('group1', new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE))
        pipelineGroup.addPipeline(dashboardPipeline('pipeline1'))
        pipelineGroup.addPipeline(dashboardPipeline('pipeline2'))
        when(pipelineSelectionsService.load((String) isNull(), any(Long.class))).thenReturn(pipelineSelections)
        when(goDashboardService.hasEverLoadedCurrentState()).thenReturn(true)
        def pipelineGroups = [pipelineGroup]
        when(goDashboardService.allPipelineGroupsForDashboard(eq(Filters.WILDCARD_FILTER), eq(currentUsername()))).thenReturn(pipelineGroups)


        def etag = computeEtag(pipelineGroups)
        getWithApiHeader(controller.controllerBasePath(), ['if-none-match': etag])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
          .hasNoBody()
      }

      @Test
      void 'should get empty json when dashboard is empty'() {
        def noPipelineGroups = []
        def pipelineSelections = PipelineSelections.ALL
        when(pipelineSelectionsService.load((String) isNull(), any(Long.class))).thenReturn(pipelineSelections)
        when(goDashboardService.allPipelineGroupsForDashboard(eq(Filters.WILDCARD_FILTER), eq(currentUsername()))).thenReturn(noPipelineGroups)
        when(goDashboardService.hasEverLoadedCurrentState()).thenReturn(true)

        loginAsUser()
        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(new DashboardFor(noPipelineGroups, currentUsername()), PipelineGroupsRepresenter)
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
        def pipelineGroup = new GoDashboardPipelineGroup('group1', new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE))
        pipelineGroup.addPipeline(dashboardPipeline('pipeline1'))
        pipelineGroup.addPipeline(dashboardPipeline('pipeline2'))
        when(pipelineSelectionsService.load((String) isNull(), any(Long.class))).thenReturn(pipelineSelections)
        when(goDashboardService.hasEverLoadedCurrentState()).thenReturn(true)
        def pipelineGroups = [pipelineGroup]
        when(goDashboardService.allPipelineGroupsForDashboard(eq(Filters.WILDCARD_FILTER), eq(currentUsername()))).thenReturn(pipelineGroups)

        String etag = computeEtag(pipelineGroups)
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

  private String computeEtag(List<GoDashboardPipelineGroup> pipelineGroups) {
    def pipelineGroupsEtag = pipelineGroups.collect { it.etag() }.join("/")
    '"' + DigestUtils.md5Hex(currentUserLoginName().toString() + "/" + pipelineGroupsEtag) + '"'
  }
}
