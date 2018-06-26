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

package com.thoughtworks.go.apiv1.datasharing.reporting

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.datasharing.reporting.representers.UsageStatisticsReportingRepresenter
import com.thoughtworks.go.domain.UsageStatisticsReporting
import com.thoughtworks.go.server.service.DataSharingUsageStatisticsReportingService
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class UsageStatisticsReportingControllerV1DelegateTest implements SecurityServiceTrait, ControllerTrait<UsageStatisticsReportingControllerV1Delegate> {
    @BeforeEach
    void setUp() {
        initMocks(this)
    }

    @Mock
    DataSharingUsageStatisticsReportingService dataSharingService

    @Override
    UsageStatisticsReportingControllerV1Delegate createControllerInstance() {
        new UsageStatisticsReportingControllerV1Delegate(new ApiAuthenticationHelper(securityService, goConfigService), dataSharingService)
    }

    @Nested
    class getDataSharingReporting {
        @Nested
        class Security implements SecurityTestTrait, NormalUserSecurity {

            @Override
            String getControllerMethodUnderTest() {
                return "getUsageStatisticsReporting"
            }

            @Override
            void makeHttpCall() {
                getWithApiHeader(controller.controllerPath('/info'))
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
            void 'get usage statistics reporting'() {
                def usageStatisticsReporting = new UsageStatisticsReporting("server-id", new Date())

                when(dataSharingService.get()).thenReturn(usageStatisticsReporting)
                getWithApiHeader(controller.controllerPath('/info'))

                assertThatResponse()
                        .isOk()
                        .hasContentType(controller.mimeType)
                        .hasBodyWithJsonObject(usageStatisticsReporting, UsageStatisticsReportingRepresenter.class)
            }
        }
    }

    @Nested
    class postDataSharingReporting {
        @Nested
        class Security implements SecurityTestTrait, NormalUserSecurity {

            @Override
            String getControllerMethodUnderTest() {
                return "updateUsageStatisticsReportingLastReportedTime"
            }

            @Override
            void makeHttpCall() {
                postWithApiHeader(controller.controllerPath('/reported'), [:])
            }
        }

        @Nested
        class AsAuthorizedUser {

            @BeforeEach
            void setUp() {
                enableSecurity()
                loginAsUser()
            }

            @Test
            void 'should update usage statistics reporting time'() {
                def reportsSharedAt = new Date()
                UsageStatisticsReporting metricsReporting = new UsageStatisticsReporting("server-id", new Date())
                metricsReporting.setLastReportedAt(reportsSharedAt)

                doReturn(metricsReporting).when(dataSharingService).updateLastReportedTime()

                def headers = [
                  'accept'        : controller.mimeType,
                  'X-GoCD-Confirm': true
                ]

                postWithApiHeader(controller.controllerPath('/reported'), headers)

                assertThatResponse()
                        .isOk()
                        .hasContentType(controller.mimeType)
                        .hasBodyWithJsonObject(metricsReporting, UsageStatisticsReportingRepresenter.class)
            }

        }
    }
}
