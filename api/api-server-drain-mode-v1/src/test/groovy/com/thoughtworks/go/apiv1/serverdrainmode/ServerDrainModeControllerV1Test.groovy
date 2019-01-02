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

package com.thoughtworks.go.apiv1.serverdrainmode

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.serverdrainmode.representers.DrainModeInfoRepresenter
import com.thoughtworks.go.config.remote.FileConfigOrigin
import com.thoughtworks.go.domain.AgentInstance
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
import com.thoughtworks.go.server.domain.ServerDrainMode
import com.thoughtworks.go.server.service.AgentService
import com.thoughtworks.go.server.service.DrainModeService
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

class ServerDrainModeControllerV1Test implements SecurityServiceTrait, ControllerTrait<ServerDrainModeControllerV1> {
  @Mock
  AgentService agentService

  @Mock
  TimeStampBasedCounter timeStampBasedCounter

  @Mock
  DrainModeService drainModeService

  @Mock
  GoDashboardCache goDashboardCache

  TestingClock testingClock = new TestingClock()

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  ServerDrainModeControllerV1 createControllerInstance() {
    new ServerDrainModeControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), goDashboardCache, agentService, drainModeService, testingClock)
  }

  @Nested
  class EnableDrainModeState {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "enableDrainModeState"
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
      void 'enable server drain mode settings'() {
        def newDrainModeState = false
        def headers = [
          'accept'      : controller.mimeType,
          'content-type': 'application/json'
        ]

        when(drainModeService.get())
          .thenReturn(new ServerDrainMode(newDrainModeState, currentUserLoginName().toString(), testingClock.currentTime()))

        postWithApiHeader(controller.controllerPath('/enable'), headers)

        def captor = ArgumentCaptor.forClass(ServerDrainMode.class)
        verify(drainModeService).update(captor.capture())
        def drainModeStateBeingSaved = captor.getValue()
        assertThat(drainModeStateBeingSaved.isDrainMode()).isTrue()
        assertThat(drainModeStateBeingSaved.updatedBy()).isEqualTo(currentUserLoginName().toString())
        assertThat(drainModeStateBeingSaved.updatedOn()).isEqualTo(new Timestamp(testingClock.currentTimeMillis()))

        assertThatResponse()
          .hasNoContent()
      }

      @Test
      void 'should not enable server drain mode in case server is already in drain state'() {
        when(drainModeService.get()).thenReturn(new ServerDrainMode(true, currentUserLoginName().toString(), testingClock.currentTime()))

        def headers = [
          'accept'      : controller.mimeType,
          'content-type': 'application/json'
        ]

        postWithApiHeader(controller.controllerPath('/enable'), headers)

        assertThatResponse()
          .isConflict()
          .hasJsonMessage("Failed to enable server drain mode. Server is already in drain state.")
      }
    }
  }

  @Nested
  class DisableDrainModeState {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "disableDrainModeState"
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
      void 'disable server drain mode settings'() {
        def newDrainModeState = true
        def headers = [
          'accept'      : controller.mimeType,
          'content-type': 'application/json'
        ]

        when(drainModeService.get())
          .thenReturn(new ServerDrainMode(newDrainModeState, currentUserLoginName().toString(), testingClock.currentTime()))

        postWithApiHeader(controller.controllerPath('/disable'), headers)

        def captor = ArgumentCaptor.forClass(ServerDrainMode.class)
        verify(drainModeService).update(captor.capture())
        def drainModeStateBeingSaved = captor.getValue()
        assertThat(drainModeStateBeingSaved.isDrainMode()).isFalse()
        assertThat(drainModeStateBeingSaved.updatedBy()).isEqualTo(currentUserLoginName().toString())
        assertThat(drainModeStateBeingSaved.updatedOn()).isEqualTo(new Timestamp(testingClock.currentTimeMillis()))

        assertThatResponse()
          .hasNoContent()
      }

      @Test
      void 'should not disable server drain mode in case server is not in drain state'() {
        when(drainModeService.get()).thenReturn(new ServerDrainMode(false, currentUserLoginName().toString(), testingClock.currentTime()))

        def headers = [
          'accept'      : controller.mimeType,
          'content-type': 'application/json'
        ]

        postWithApiHeader(controller.controllerPath('/disable'), headers)

        assertThatResponse()
          .isConflict()
          .hasJsonMessage("Failed to disable server drain mode. Server is not in drain state.")
      }
    }
  }

  @Nested
  class Info {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "getDrainModeInfo"
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
      void 'get drain mode info'() {
        def runningMDUs = []
        def runningJobs = []

        when(drainModeService.get()).thenReturn(new ServerDrainMode(true, currentUserLoginName().toString(), testingClock.currentTime()))
        when(drainModeService.getRunningMDUs()).thenReturn(runningMDUs)
        when(goDashboardCache.allEntries()).thenReturn(new GoDashboardPipelines(new HashMap<>(), new TimeStampBasedCounter(testingClock)))
        when(agentService.agents()).thenReturn(new AgentInstances(null))

        getWithApiHeader(controller.controllerPath('/info'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({
          DrainModeInfoRepresenter.toJSON(it, drainModeService.get(), true, runningMDUs, runningJobs)
        }))
      }

      @Test
      void 'get drain mode info when jobs are running'() {
        def job1Identifier = new JobIdentifier("up42", 1, "up42", "stage1", "1", "job1")
        def job2Identifier = new JobIdentifier("up42", 1, "up42", "stage1", "1", "job2")

        def buildingAgent = AgentInstanceMother.building(job1Identifier.buildLocator())

        def dashboardPipelinesMap = new HashMap<>()
        dashboardPipelinesMap.put("up42", getRunningPipeline("up42"))

        def job1 = new JobInstance("job1")
        job1.setState(JobState.Building)
        job1.setScheduledDate(new Date(1))
        job1.setIdentifier(job1Identifier)
        job1.setAgentUuid(buildingAgent.getUuid())

        def job2 = new JobInstance("job2")
        job2.setState(JobState.Scheduled)
        job2.setScheduledDate(new Date(1))
        job2.setIdentifier(job2Identifier)

        def dashboardPipelines = new GoDashboardPipelines(dashboardPipelinesMap, new TimeStampBasedCounter(testingClock))

        def agentInstances = new AgentInstances(null)
        agentInstances.add(buildingAgent)

        def runningMDUs = []
        def runningJobs = [job1, job2]

        when(drainModeService.get()).thenReturn(new ServerDrainMode(true, currentUserLoginName().toString(), testingClock.currentTime()))
        when(drainModeService.getRunningMDUs()).thenReturn(runningMDUs)
        when(goDashboardCache.allEntries()).thenReturn(dashboardPipelines)
        when(agentService.agents()).thenReturn(agentInstances)

        getWithApiHeader(controller.controllerPath('/info'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({
          DrainModeInfoRepresenter.toJSON(it, drainModeService.get(), false, runningMDUs, runningJobs)
        }))
      }

      @Test
      void 'should not fetch running subsystems information when server is not in drain mode'() {
        when(drainModeService.get()).thenReturn(new ServerDrainMode(false, currentUserLoginName().toString(), testingClock.currentTime()))

        getWithApiHeader(controller.controllerPath('/info'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({
          DrainModeInfoRepresenter.toJSON(it, drainModeService.get(), false, null, null)
        }))

        verifyZeroInteractions(goDashboardCache)
        verifyZeroInteractions(agentService)
        verify(drainModeService, never()).getRunningMDUs()
      }

      GoDashboardPipeline getRunningPipeline(String pipelineName) {
        def pipelineModel = new PipelineModel(pipelineName, false, false, notPaused())
        def allStages = new StageInstanceModels()

        def allJobs = new JobHistory()
        allJobs.add(new JobHistoryItem("job1", JobState.Building, JobResult.Unknown, new Date(1)))
        allJobs.add(new JobHistoryItem("job2", JobState.Scheduled, JobResult.Unknown, new Date(1)))
        allJobs.add(new JobHistoryItem("job4", JobState.Completed, JobResult.Passed, new Date(1)))
        allStages.add(new StageInstanceModel("stage1", "1", allJobs))
        def pipelineInstanceModel = new PipelineInstanceModel(pipelineName, 1, pipelineName, null, allStages)
        pipelineModel.addPipelineInstance(pipelineInstanceModel)
        return new GoDashboardPipeline(pipelineModel, null, "group1", new TimeStampBasedCounter(testingClock), new FileConfigOrigin())
      }
    }
  }
}
