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
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
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
    @Mock
    EntityHashingService entityHashingService

    @Override
    UsageStatisticsReportingControllerV1Delegate createControllerInstance() {
        new UsageStatisticsReportingControllerV1Delegate(new ApiAuthenticationHelper(securityService, goConfigService), dataSharingService, entityHashingService)
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
            void 'get usage statistics reporting'() {
                def usageStatisticsReporting = new UsageStatisticsReporting("server-id", new Date())

                when(dataSharingService.get()).thenReturn(usageStatisticsReporting)
                def etag = "md5"
                when(entityHashingService.md5ForEntity(any() as UsageStatisticsReporting)).thenReturn(etag)
                getWithApiHeader(controller.controllerPath())

                assertThatResponse()
                        .isOk()
                        .hasEtag('"' + etag + '"')
                        .hasContentType(controller.mimeType)
                        .hasBodyWithJsonObject(usageStatisticsReporting, UsageStatisticsReportingRepresenter.class)
            }
        }
    }

    @Nested
    class patchDataSharingReporting {
        @Nested
        class Security implements SecurityTestTrait, NormalUserSecurity {

            @Override
            String getControllerMethodUnderTest() {
                return "updateUsageStatisticsReporting"
            }

            @Override
            void makeHttpCall() {
                patchWithApiHeader(controller.controllerBasePath(), [:])
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
                def data = [
                  last_reported_at: reportsSharedAt.getTime(),
                ]

                UsageStatisticsReporting metricsReporting = new UsageStatisticsReporting("server-id", new Date())
                metricsReporting.setLastReportedAt(reportsSharedAt)

                doNothing().when(dataSharingService).update(any() as UsageStatisticsReporting, any() as HttpLocalizedOperationResult)
                doReturn(metricsReporting).when(dataSharingService).get()

                when(entityHashingService.md5ForEntity(any() as UsageStatisticsReporting)).thenReturn("cached-md5")

                def headers = [
                  'accept'      : controller.mimeType,
                  'If-Match'    : 'cached-md5',
                  'content-type': 'application/json'
                ]

                patchWithApiHeader(controller.controllerBasePath(), headers, data)

                assertThatResponse()
                        .isOk()
                        .hasContentType(controller.mimeType)
                        .hasBodyWithJsonObject(metricsReporting, UsageStatisticsReportingRepresenter.class)
            }

            @Test
            void 'should reject if server_id is being updated in request'() {
                def reportsSharedAt = new Date()
                def data = [
                  server_id: "something-new",
                  last_reported_at: reportsSharedAt.getTime(),
                ]
                when(entityHashingService.md5ForEntity(any() as UsageStatisticsReporting)).thenReturn("cached-md5")
                def headers = [
                  'accept'      : controller.mimeType,
                  'If-Match'    : 'cached-md5',
                  'content-type': 'application/json'
                ]
                patchWithApiHeader(controller.controllerBasePath(), headers, data)

                assertThatResponse()
                  .isUnprocessableEntity()
                  .hasContentType(controller.mimeType)
                  .hasJsonMessage("Renaming of server_id is not supported by this API.")
            }

            @Test
            void 'should reject if data_sharing_server_url is being updated in request'() {
                def reportsSharedAt = new Date()
                def data = [
                  last_reported_at       : reportsSharedAt.getTime(),
                  data_sharing_server_url: "something-new"
                ]
                when(entityHashingService.md5ForEntity(any() as UsageStatisticsReporting)).thenReturn("cached-md5")
                def headers = [
                  'accept'      : controller.mimeType,
                  'If-Match'    : 'cached-md5',
                  'content-type': 'application/json'
                ]
                patchWithApiHeader(controller.controllerBasePath(), headers, data)

                assertThatResponse()
                  .isUnprocessableEntity()
                  .hasContentType(controller.mimeType)
                  .hasJsonMessage("Renaming of data_sharing_server_url is not supported by this API.")
            }

            @Test
            void 'should return error occurred validation fails'() {
                def errorMsg = "Please provide last_reported_at time."
                def data = [last_reported_at: null]
                UsageStatisticsReporting usageStatisticsReportingReturnedByServer
                doAnswer({ InvocationOnMock invocation ->
                    UsageStatisticsReporting reporting = invocation.arguments.first()
                    reporting.addError("lastReportedAt", "error message")
                    HttpLocalizedOperationResult result = invocation.arguments.last()
                    result.unprocessableEntity(errorMsg)
                    usageStatisticsReportingReturnedByServer = reporting
                }).when(dataSharingService).update(any() as UsageStatisticsReporting, any() as HttpLocalizedOperationResult)
                when(entityHashingService.md5ForEntity(any() as UsageStatisticsReporting)).thenReturn("cached-md5")

                def headers = [
                  'accept'      : controller.mimeType,
                  'If-Match'    : 'cached-md5',
                  'content-type': 'application/json'
                ]

                patchWithApiHeader(controller.controllerBasePath(), headers, data)

                def jsonDataWithErrors = toObjectString({ UsageStatisticsReportingRepresenter.toJSON(it, usageStatisticsReportingReturnedByServer) })
                assertThatResponse()
                        .isUnprocessableEntity()
                        .hasJsonMessage(errorMsg)
                        .hasJsonAttribute("data", jsonDataWithErrors)
            }

            @Test
            void 'should reject if etag does not match'() {
                def errorMsg = "Please provide last_reported_at time."
                def data = [last_reported_at: null]
                UsageStatisticsReporting usageStatisticsReportingReturnedByServer
                doAnswer({ InvocationOnMock invocation ->
                    UsageStatisticsReporting reporting = invocation.arguments.first()
                    reporting.addError("lastReportedAt", "error message")
                    HttpLocalizedOperationResult result = invocation.arguments.last()
                    result.unprocessableEntity(errorMsg)
                    usageStatisticsReportingReturnedByServer = reporting
                }).when(dataSharingService).update(any() as UsageStatisticsReporting, any() as HttpLocalizedOperationResult)
                when(entityHashingService.md5ForEntity(any() as UsageStatisticsReporting)).thenReturn("cached-md5")

                def headers = [
                  'accept'      : controller.mimeType,
                  'If-Match'    : 'old-md5',
                  'content-type': 'application/json'
                ]

                patchWithApiHeader(controller.controllerBasePath(), headers, data)

                assertThatResponse()
                        .isPreconditionFailed()
                        .hasJsonMessage("Someone has modified the entity. Please update your copy with the changes and try again.")
            }
        }
    }
}

