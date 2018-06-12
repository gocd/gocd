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

package com.thoughtworks.go.apiv1.datasharing

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.datasharing.usagedata.*
import com.thoughtworks.go.apiv1.datasharing.usagedata.representers.*
import com.thoughtworks.go.server.service.DataSharingService
import com.thoughtworks.go.server.domain.UsageStatistics
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class UsageStatisticsControllerV1DelegateTest implements SecurityServiceTrait, ControllerTrait<UsageStatisticsControllerV1Delegate> {
  @BeforeEach
  void setUp() {
    initMocks(this)
  }
  @Mock
  DataSharingService dataSharingService

  @Override
  UsageStatisticsControllerV1Delegate createControllerInstance() {
    new UsageStatisticsControllerV1Delegate(new ApiAuthenticationHelper(securityService, goConfigService), dataSharingService )
  }

  @Nested
  class getDataSharing {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "getUsageStatistics"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath())
      }
    }

    @Nested
    class AsNormalUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsUser()
      }

      @Test
      void 'get usage statistics'() {
        def metrics = new UsageStatistics(10l, 20l, 1527244129553)
        when(dataSharingService.getUsageStatistics()).thenReturn(metrics)

        getWithApiHeader(controller.controllerPath())
        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(metrics, UsageStatisticsRepresenter.class)
      }
    }
  }
}

