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
import com.thoughtworks.go.api.base.JsonOutputWriter
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.serverdrainmode.representers.DrainModeInfoRepresenter
import com.thoughtworks.go.server.domain.ServerDrainMode
import com.thoughtworks.go.server.service.DrainModeService
import com.thoughtworks.go.server.service.JobInstanceService
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService
import com.thoughtworks.go.server.service.support.toggle.Toggles
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
import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class ServerDrainModeControllerV1Test implements SecurityServiceTrait, ControllerTrait<ServerDrainModeControllerV1> {
  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Mock
  DrainModeService drainModeService

  @Mock
  FeatureToggleService featureToggleService

  @Mock
  JobInstanceService jobInstanceService

  TestingClock testingClock = new TestingClock()

  @Override
  ServerDrainModeControllerV1 createControllerInstance() {
    new ServerDrainModeControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), drainModeService, jobInstanceService, featureToggleService, testingClock)
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

        when(featureToggleService.isToggleOn(Toggles.SERVER_DRAIN_MODE_API_TOGGLE_KEY)).thenReturn(true)
      }

      @Test
      void 'should return not found when SERVER_DRAIN_MODE_API_TOGGLE_KEY is turned off'() {
        when(featureToggleService.isToggleOn(Toggles.SERVER_DRAIN_MODE_API_TOGGLE_KEY)).thenReturn(false)
        def headers = [
          'accept'      : controller.mimeType,
          'content-type': 'application/json'
        ]

        postWithApiHeader(controller.controllerPath('/enable'), headers, [:])

        assertThatResponse()
          .isNotFound()
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

        when(featureToggleService.isToggleOn(Toggles.SERVER_DRAIN_MODE_API_TOGGLE_KEY)).thenReturn(true)
      }

      @Test
      void 'should return not found when SERVER_DRAIN_MODE_API_TOGGLE_KEY is turned off'() {
        when(featureToggleService.isToggleOn(Toggles.SERVER_DRAIN_MODE_API_TOGGLE_KEY)).thenReturn(false)
        def headers = [
          'accept'      : controller.mimeType,
          'content-type': 'application/json'
        ]

        postWithApiHeader(controller.controllerPath('/disable'), headers, [:])

        assertThatResponse()
          .isNotFound()
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
        when(jobInstanceService.allBuildingJobs()).thenReturn(runningJobs)
        when(featureToggleService.isToggleOn(Toggles.SERVER_DRAIN_MODE_API_TOGGLE_KEY)).thenReturn(true)

        getWithApiHeader(controller.controllerPath('/info'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ DrainModeInfoRepresenter.toJSON(it, drainModeService.get(), true, runningMDUs, runningJobs) }))
      }

      @Test
      void 'should return not found when SERVER_DRAIN_MODE_API_TOGGLE_KEY is turned off'() {
        when(featureToggleService.isToggleOn(Toggles.SERVER_DRAIN_MODE_API_TOGGLE_KEY)).thenReturn(false)

        getWithApiHeader(controller.controllerPath('/info'))

        assertThatResponse()
          .isNotFound()
      }
    }
  }
}
