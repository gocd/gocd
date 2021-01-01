/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv1.defaultjobtimeout

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.defaultjobtimeout.representers.DefaultJobTimeOutRepresenter
import com.thoughtworks.go.config.CruiseConfig
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.ServerConfigService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class DefaultJobTimeoutControllerV1Test implements SecurityServiceTrait, ControllerTrait<DefaultJobTimeoutControllerV1> {

  @Mock
  EntityHashingService entityHashingService

  @Mock
  ServerConfigService serverConfigService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  DefaultJobTimeoutControllerV1 createControllerInstance() {
    new DefaultJobTimeoutControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService, serverConfigService)
  }

  @Nested
  class Index {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath())
      }
    }

    @BeforeEach
    void setUp() {
      enableSecurity()
      loginAsAdmin()
    }

    @Test
    void 'should return default job timeout'() {
      def defaultJobTimeout = "10"
      when(serverConfigService.getDefaultJobTimeout()).thenReturn(defaultJobTimeout)
      getWithApiHeader(controller.controllerPath())

      assertThatResponse()
        .isOk()
        .hasBodyWithJson(toObjectString({ DefaultJobTimeOutRepresenter.toJSON(it, defaultJobTimeout) }))

      verify(serverConfigService, times(1)).getDefaultJobTimeout()
      verifyNoMoreInteractions(serverConfigService)
    }
  }

  @Nested
  class Create {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "createOrUpdate"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerBasePath(), [])
      }
    }

    @BeforeEach
    void setUp() {
      enableSecurity()
      loginAsAdmin()
    }

    @Test
    void 'should set default job timeout value'() {
      def defaultJobTimeout = "10"
      postWithApiHeader(controller.controllerPath(), ["default_job_timeout": defaultJobTimeout])

      assertThatResponse()
        .isOk()
        .hasBodyWithJson(toObjectString({ DefaultJobTimeOutRepresenter.toJSON(it, defaultJobTimeout) }))
    }

    @Test
    void 'should not set default job timeout value if it is not valid'() {
      def defaultJobTimeout = "foo"
      def cruiseConfig = mock(CruiseConfig.class)
      when(serverConfigService.createOrUpdateDefaultJobTimeout(defaultJobTimeout)).thenThrow(new GoConfigInvalidException(cruiseConfig, "Timeout should be a valid number as it represents number of minutes"))
      postWithApiHeader(controller.controllerPath(), ["default_job_timeout": defaultJobTimeout])

      assertThatResponse()
        .isUnprocessableEntity()
        .hasJsonMessage("Timeout should be a valid number as it represents number of minutes")
    }

    @Test
    void 'should not allow empty string as a default job timeout value'() {
      def defaultJobTimeout = ""
      def cruiseConfig = mock(CruiseConfig.class)
      when(serverConfigService.createOrUpdateDefaultJobTimeout(defaultJobTimeout)).thenThrow(new GoConfigInvalidException(cruiseConfig, "Timeout should be a valid number as it represents number of minutes"))
      postWithApiHeader(controller.controllerPath(), ["default_job_timeout": defaultJobTimeout])

      assertThatResponse()
        .isUnprocessableEntity()
        .hasJsonMessage("Timeout should be a valid number as it represents number of minutes")
    }
  }

  @Nested
  class Update {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "createOrUpdate"
      }

      @Override
      void makeHttpCall() {
        putWithApiHeader(controller.controllerBasePath(), [])
      }
    }

    @BeforeEach
    void setUp() {
      enableSecurity()
      loginAsAdmin()
    }

    @Test
    void 'should update default job timeout value'() {
      def defaultJobTimeout = "10"
      putWithApiHeader(controller.controllerPath(), ["default_job_timeout": defaultJobTimeout])

      assertThatResponse()
        .isOk()
        .hasBodyWithJson(toObjectString({ DefaultJobTimeOutRepresenter.toJSON(it, defaultJobTimeout) }))
    }

    @Test
    void 'should not update default job timeout value if it is not valid'() {
      def defaultJobTimeout = "foo"
      def cruiseConfig = mock(CruiseConfig.class)
      when(serverConfigService.createOrUpdateDefaultJobTimeout(defaultJobTimeout)).thenThrow(new GoConfigInvalidException(cruiseConfig, "Timeout should be a valid number as it represents number of minutes"))
      putWithApiHeader(controller.controllerPath(), ["default_job_timeout": defaultJobTimeout])

      assertThatResponse()
        .isUnprocessableEntity()
        .hasJsonMessage("Timeout should be a valid number as it represents number of minutes")
    }

    @Test
    void 'should not allow to update empty string as a default job timeout value'() {
      def defaultJobTimeout = ""
      def cruiseConfig = mock(CruiseConfig.class)
      when(serverConfigService.createOrUpdateDefaultJobTimeout(defaultJobTimeout)).thenThrow(new GoConfigInvalidException(cruiseConfig, "Timeout should be a valid number as it represents number of minutes"))
      putWithApiHeader(controller.controllerPath(), ["default_job_timeout": defaultJobTimeout])

      assertThatResponse()
        .isUnprocessableEntity()
        .hasJsonMessage("Timeout should be a valid number as it represents number of minutes")
    }
  }
}
