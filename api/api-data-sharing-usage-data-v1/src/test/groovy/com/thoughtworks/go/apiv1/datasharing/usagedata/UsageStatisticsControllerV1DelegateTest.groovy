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

package com.thoughtworks.go.apiv1.datasharing.usagedata

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.datasharing.usagedata.representers.UsageStatisticsRepresenter
import com.thoughtworks.go.server.domain.UsageStatistics
import com.thoughtworks.go.server.service.DataSharingUsageDataService
import com.thoughtworks.go.server.util.RSAEncryptionHelper
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.util.SystemEnvironment
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.skyscreamer.jsonassert.JSONAssert

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class UsageStatisticsControllerV1DelegateTest implements SecurityServiceTrait, ControllerTrait<UsageStatisticsControllerV1Delegate> {
  @BeforeEach
  void setUp() {
    initMocks(this)
  }
  @Mock
  DataSharingUsageDataService dataSharingService

  @Mock
  SystemEnvironment systemEnvironment

  @Override
  UsageStatisticsControllerV1Delegate createControllerInstance() {
    new UsageStatisticsControllerV1Delegate(new ApiAuthenticationHelper(securityService, goConfigService), dataSharingService, systemEnvironment)
  }

  @Nested
  class getDataSharing {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

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
    class AsAdminUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'get usage statistics'() {
        def metrics = new UsageStatistics(10l, 20l, 1527244129553)
        when(dataSharingService.getUsageStatistics()).thenReturn(metrics)

        getWithApiHeader(controller.controllerPath())
        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBody(toObjectString({ UsageStatisticsRepresenter.toJSON(it, metrics) }))
      }
    }
  }

  @Nested
  class getEncryptedDataSharing {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "getEncryptedUsageStatistics"
      }

      @Override
      void makeHttpCall() {
        controller.mimeType = "application/octet-stream"
        getWithApiHeader(controller.controllerPath('/encrypted'))
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
      void 'get encrypted usage statistics'() {
        def metrics = new UsageStatistics(10l, 20l, 1527244129553)
        File privateKeyFile = new File(getClass().getClassLoader().getResource("private_key.pem").getFile())
        File publicKeyFile = new File(getClass().getClassLoader().getResource("public_key.pem").getFile())

        when(systemEnvironment.getUpdateServerPublicKeyPath()).thenReturn(publicKeyFile.getAbsolutePath())
        when(dataSharingService.getUsageStatistics()).thenReturn(metrics)

        def expectedJson = toObjectString({ UsageStatisticsRepresenter.toJSON(it, metrics) })

        getWithApiHeader(controller.controllerPath('/encrypted'))

        def actualEncrypted = response.getContentAsString()
        JSONAssert.assertEquals(RSAEncryptionHelper.decrypt(actualEncrypted, privateKeyFile.getAbsolutePath()), expectedJson, true)
        assertThatResponse()
          .isOk()
          .hasContentType("application/octet-stream")
      }
    }
  }
}
