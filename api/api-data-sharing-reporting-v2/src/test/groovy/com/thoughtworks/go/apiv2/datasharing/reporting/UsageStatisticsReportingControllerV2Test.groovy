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
package com.thoughtworks.go.apiv2.datasharing.reporting

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv2.datasharing.reporting.representers.UsageStatisticsReportingRepresenter
import com.thoughtworks.go.domain.UsageStatisticsReporting
import com.thoughtworks.go.server.service.datasharing.DataSharingUsageStatisticsReportingService
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

class UsageStatisticsReportingControllerV2Test implements SecurityServiceTrait, ControllerTrait<UsageStatisticsReportingControllerV2> {
    @BeforeEach
    void setUp() {
        initMocks(this)
    }

    @Mock
    DataSharingUsageStatisticsReportingService service

    @Override
    UsageStatisticsReportingControllerV2 createControllerInstance() {
        new UsageStatisticsReportingControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), service, goCache)
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

                when(service.get()).thenReturn(usageStatisticsReporting)
                getWithApiHeader(controller.controllerPath('/info'))

                assertThatResponse()
                        .isOk()
                        .hasContentType(controller.mimeType)
                        .hasBodyWithJsonObject(usageStatisticsReporting, UsageStatisticsReportingRepresenter.class)
            }
        }
    }

    @Nested
    class startReporting {
        @Nested
        class Security implements SecurityTestTrait, NormalUserSecurity {

            @Override
            String getControllerMethodUnderTest() {
                return "startReporting"
            }

            @Override
            void makeHttpCall() {
                postWithApiHeader(controller.controllerPath('/start'), [:])
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
            void 'should start usage reporting'() {
                doNothing().when(service).startReporting(any())

                def headers = [
                        'accept'        : controller.mimeType,
                        'X-GoCD-Confirm': true
                ]

                postWithApiHeader(controller.controllerPath('/start'), headers)

                verify(service).startReporting(any())

                assertThatResponse()
                        .hasNoContent()
                        .hasContentType(controller.mimeType)
            }
        }
    }

    @Nested
    class completeReporting {
        @Nested
        class Security implements SecurityTestTrait, NormalUserSecurity {

            @Override
            String getControllerMethodUnderTest() {
                return "completeReporting"
            }

            @Override
            void makeHttpCall() {
                postWithApiHeader(controller.controllerPath('/complete'), [:])
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
            void 'should complete usage reporting'() {
                doNothing().when(service).completeReporting(any())

                def headers = [
                        'accept'        : controller.mimeType,
                        'X-GoCD-Confirm': true
                ]

                postWithApiHeader(controller.controllerPath('/complete'), headers)

                verify(service).completeReporting(any())

                assertThatResponse()
                        .hasNoContent()
                        .hasContentType(controller.mimeType)
            }
        }
    }
}
