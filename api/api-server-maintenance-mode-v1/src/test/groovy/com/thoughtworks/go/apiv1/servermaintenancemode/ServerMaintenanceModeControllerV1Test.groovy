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
package com.thoughtworks.go.apiv1.servermaintenancemode

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.servermaintenancemode.representers.MaintenanceModeInfoRepresenter
import com.thoughtworks.go.config.remote.FileConfigOrigin
import com.thoughtworks.go.domain.JobIdentifier
import com.thoughtworks.go.domain.JobInstance
import com.thoughtworks.go.domain.JobResult
import com.thoughtworks.go.domain.JobState
import com.thoughtworks.go.helper.AgentInstanceMother
import com.thoughtworks.go.presentation.pipelinehistory.*
import com.thoughtworks.go.server.dashboard.GoDashboardCache
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline
import com.thoughtworks.go.server.dashboard.GoDashboardPipelines
import com.thoughtworks.go.server.dashboard.TimeStampBasedCounter
import com.thoughtworks.go.server.domain.AgentInstances
import com.thoughtworks.go.server.domain.ServerMaintenanceMode
import com.thoughtworks.go.server.service.AgentService
import com.thoughtworks.go.server.service.MaintenanceModeService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.util.TestingClock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock

import java.sql.Timestamp

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.domain.PipelinePauseInfo.notPaused
import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class ServerMaintenanceModeControllerV1Test implements SecurityServiceTrait, ControllerTrait<ServerMaintenanceModeControllerV1> {
  @Mock
  AgentService agentService

  @Mock
  TimeStampBasedCounter timeStampBasedCounter

  @Mock
  MaintenanceModeService maintenanceModeService

  @Mock
  GoDashboardCache goDashboardCache

  TestingClock testingClock = new TestingClock()

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  ServerMaintenanceModeControllerV1 createControllerInstance() {
    new ServerMaintenanceModeControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), goDashboardCache, agentService, maintenanceModeService, testingClock)
  }

  @Nested
  class EnableMaintenanceModeState {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "enableMaintenanceModeState"
      }

      @Override
      void makeHttpCall() {
        post(controller.controllerPath('/enable'), [:])
      }
    }

    @Nested
    class AsAdminUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

      }

      @Test
      void 'enable server maintenance mode settings'() {
        def newMaintenanceModeState = false
        def headers = [
          'accept'      : controller.mimeType,
          'content-type': 'application/json'
        ]

        when(maintenanceModeService.get())
          .thenReturn(new ServerMaintenanceMode(newMaintenanceModeState, currentUsernameString(), testingClock.currentTime()))

        postWithApiHeader(controller.controllerPath('/enable'), headers)

        def captor = ArgumentCaptor.forClass(ServerMaintenanceMode.class)
        verify(maintenanceModeService).update(captor.capture())
        def maintenanceModeStateBeingSaved = captor.getValue()
        assertThat(maintenanceModeStateBeingSaved.isMaintenanceMode()).isTrue()
        assertThat(maintenanceModeStateBeingSaved.updatedBy()).isEqualTo(currentUsernameString())
        assertThat(maintenanceModeStateBeingSaved.updatedOn()).isEqualTo(new Timestamp(testingClock.currentTimeMillis()))

        assertThatResponse()
          .hasNoContent()
      }

      @Test
      void 'should not enable server maintenance mode in case server is already in maintenance mode'() {
        when(maintenanceModeService.get()).thenReturn(new ServerMaintenanceMode(true, currentUsernameString(), testingClock.currentTime()))

        def headers = [
          'accept'      : controller.mimeType,
          'content-type': 'application/json'
        ]

        postWithApiHeader(controller.controllerPath('/enable'), headers)

        assertThatResponse()
          .isConflict()
          .hasJsonMessage("Failed to enable server maintenance mode. Server is already in maintenance mode.")
      }
    }
  }

  @Nested
  class DisableMaintenanceModeState {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "disableMaintenanceModeState"
      }

      @Override
      void makeHttpCall() {
        post(controller.controllerPath('/disable'), [:])
      }
    }

    @Nested
    class AsAdminUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

      }

      @Test
      void 'disable server maintenance mode settings'() {
        def newMaintenanceModeState = true
        def headers = [
          'accept'      : controller.mimeType,
          'content-type': 'application/json'
        ]

        when(maintenanceModeService.get())
          .thenReturn(new ServerMaintenanceMode(newMaintenanceModeState, currentUsernameString(), testingClock.currentTime()))

        postWithApiHeader(controller.controllerPath('/disable'), headers)

        def captor = ArgumentCaptor.forClass(ServerMaintenanceMode.class)
        verify(maintenanceModeService).update(captor.capture())
        def maintenanceModeStateBeingSaved = captor.getValue()
        assertThat(maintenanceModeStateBeingSaved.isMaintenanceMode()).isFalse()
        assertThat(maintenanceModeStateBeingSaved.updatedBy()).isEqualTo(currentUsernameString())
        assertThat(maintenanceModeStateBeingSaved.updatedOn()).isEqualTo(new Timestamp(testingClock.currentTimeMillis()))

        assertThatResponse()
          .hasNoContent()
      }

      @Test
      void 'should not disable server maintenance mode in case server is not in maintenance mode'() {
        when(maintenanceModeService.get()).thenReturn(new ServerMaintenanceMode(false, currentUsernameString(), testingClock.currentTime()))

        def headers = [
          'accept'      : controller.mimeType,
          'content-type': 'application/json'
        ]

        postWithApiHeader(controller.controllerPath('/disable'), headers)

        assertThatResponse()
          .isConflict()
          .hasJsonMessage("Failed to disable server maintenance mode. Server is not in maintenance mode.")
      }
    }
  }

  @Nested
  class Info {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "getMaintenanceModeInfo"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath('/info'))
      }
    }

    @Nested
    class AsAdminUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'get maintenance mode info'() {
        def runningMDUs = []
        def runningJobs = []

        when(maintenanceModeService.get()).thenReturn(new ServerMaintenanceMode(true, currentUsernameString(), testingClock.currentTime()))
        when(maintenanceModeService.getRunningMDUs()).thenReturn(runningMDUs)
        when(goDashboardCache.allEntries()).thenReturn(new GoDashboardPipelines(new HashMap<>(), new TimeStampBasedCounter(testingClock)))
        when(agentService.getAgentInstances()).thenReturn(new AgentInstances(null))

        getWithApiHeader(controller.controllerPath('/info'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({
          MaintenanceModeInfoRepresenter.toJSON(it, maintenanceModeService.get(), true, runningMDUs, runningJobs, [])
        }))
      }

      @Test
      void 'get maintenance mode info when jobs are running'() {
        def job1Identifier = new JobIdentifier("up42", 1, "up42", "stage1", "1", "job1")
        def job2Identifier = new JobIdentifier("up42", 1, "up42", "stage1", "1", "job2")
        def job3Identifier = new JobIdentifier("up42", 1, "up42", "stage1", "1", "job3")

        def buildingAgent1 = AgentInstanceMother.building(job1Identifier.buildLocator())
        def buildingAgent2 = AgentInstanceMother.building(job3Identifier.buildLocator())
        buildingAgent2.getAgent().setUuid("agent-2")

        def dashboardPipelinesMap = new HashMap<>()
        dashboardPipelinesMap.put("up42", getRunningPipeline("up42"))

        def job1 = new JobInstance("job1")
        job1.setState(JobState.Building)
        job1.setScheduledDate(new Date(1))
        job1.setIdentifier(job1Identifier)
        job1.setAgentUuid(buildingAgent1.getUuid())

        def job2 = new JobInstance("job2")
        job2.setState(JobState.Scheduled)
        job2.setScheduledDate(new Date(1))
        job2.setIdentifier(job2Identifier)

        def job3 = new JobInstance("job3")
        job3.setState(JobState.Building)
        job3.setScheduledDate(new Date(1))
        job3.setIdentifier(job3Identifier)
        job3.setAgentUuid(buildingAgent2.getUuid())

        def dashboardPipelines = new GoDashboardPipelines(dashboardPipelinesMap, new TimeStampBasedCounter(testingClock))

        def agentInstances = new AgentInstances(null)
        agentInstances.add(buildingAgent1)
        agentInstances.add(buildingAgent2)

        def runningMDUs = []
        def buildingJobs = [job1, job3]
        def scheduledJobs = [job2]

        when(maintenanceModeService.get()).thenReturn(new ServerMaintenanceMode(true, currentUsernameString(), testingClock.currentTime()))
        when(maintenanceModeService.getRunningMDUs()).thenReturn(runningMDUs)
        when(goDashboardCache.allEntries()).thenReturn(dashboardPipelines)
        when(agentService.getAgentInstances()).thenReturn(agentInstances)

        getWithApiHeader(controller.controllerPath('/info'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({
          MaintenanceModeInfoRepresenter.toJSON(it, maintenanceModeService.get(), false, runningMDUs, buildingJobs, scheduledJobs)
        }))
      }

      @Test
      void 'should not fetch running subsystems information when server is not in maintenance mode'() {
        when(maintenanceModeService.get()).thenReturn(new ServerMaintenanceMode(false, currentUsernameString(), testingClock.currentTime()))

        getWithApiHeader(controller.controllerPath('/info'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({
          MaintenanceModeInfoRepresenter.toJSON(it, maintenanceModeService.get(), false, null, null, null)
        }))

        verifyZeroInteractions(goDashboardCache)
        verifyZeroInteractions(agentService)
        verify(maintenanceModeService, never()).getRunningMDUs()
      }

      GoDashboardPipeline getRunningPipeline(String pipelineName) {
        def pipelineModel = new PipelineModel(pipelineName, false, false, notPaused())
        def allStages = new StageInstanceModels()

        def allJobs = new JobHistory()

        allJobs.add(new JobHistoryItem("job1", JobState.Building, JobResult.Unknown, new Date(1)))
        allJobs.add(new JobHistoryItem("job2", JobState.Scheduled, JobResult.Unknown, new Date(1)))
        allJobs.add(new JobHistoryItem("job3", JobState.Building, JobResult.Unknown, new Date(1)))
        allJobs.add(new JobHistoryItem("job4", JobState.Completed, JobResult.Passed, new Date(1)))

        allStages.add(new StageInstanceModel("stage1", "1", allJobs))
        def pipelineInstanceModel = new PipelineInstanceModel(pipelineName, 1, pipelineName, null, allStages)
        pipelineModel.addPipelineInstance(pipelineInstanceModel)
        return new GoDashboardPipeline(pipelineModel, null, "group1", null, new TimeStampBasedCounter(testingClock), new FileConfigOrigin(), 0)
      }
    }
  }
}
