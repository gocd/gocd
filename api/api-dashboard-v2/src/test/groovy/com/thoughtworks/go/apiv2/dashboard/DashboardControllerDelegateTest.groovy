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

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv2.dashboard.representers.PipelineGroupsRepresenter
import com.thoughtworks.go.config.security.Permissions
import com.thoughtworks.go.config.security.users.Everyone
import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup
import com.thoughtworks.go.server.domain.user.PipelineSelections
import com.thoughtworks.go.server.service.GoDashboardService
import com.thoughtworks.go.server.service.PipelineSelectionsService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.mocks.TestRequestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.apiv2.dashboard.GoDashboardPipelineMother.dashboardPipeline
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.when
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
    new DashboardControllerDelegate(pipelineSelectionsService, goDashboardService)
  }

  @Nested
  class Dashboard {

    @Test
    void 'should get dashboard json'() {
      loginAsUser()

      def pipelineSelections = PipelineSelections.ALL
      def pipelineGroup = new GoDashboardPipelineGroup('group1', new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE))
      pipelineGroup.addPipeline(dashboardPipeline('pipeline1'))
      pipelineGroup.addPipeline(dashboardPipeline('pipeline2'))
      when(pipelineSelectionsService.getPersistedSelectedPipelines(any(), any())).thenReturn(pipelineSelections)
      when(goDashboardService.allPipelineGroupsForDashboard(eq(pipelineSelections), eq(currentUsername()))).thenReturn([pipelineGroup])

      getWithApiHeader(controller.controllerPath())

      def expectedJsonBody = PipelineGroupsRepresenter.toJSON([pipelineGroup], new TestRequestContext(), currentUsername())

      assertThatResponse()
        .isOk()
        .hasJsonBody(GsonTransformer.instance.render(expectedJsonBody))
    }

    @Test
    void 'should get empty json when dashboard is empty'() {
      def noPipelineGroups = []
      def pipelineSelections = PipelineSelections.ALL
      when(pipelineSelectionsService.getPersistedSelectedPipelines(any(), any())).thenReturn(pipelineSelections)
      when(goDashboardService.allPipelineGroupsForDashboard(eq(pipelineSelections), eq(currentUsername()))).thenReturn(noPipelineGroups)

      loginAsUser()
      getWithApiHeader(controller.controllerPath())

      def expectedJsonBody = PipelineGroupsRepresenter.toJSON(noPipelineGroups, new TestRequestContext(), currentUsername())

      assertThatResponse()
        .isOk()
        .hasJsonBody(GsonTransformer.instance.render(expectedJsonBody))
    }
  }
}
