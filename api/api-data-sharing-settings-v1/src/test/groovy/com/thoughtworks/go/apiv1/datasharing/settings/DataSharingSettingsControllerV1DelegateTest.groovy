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

package com.thoughtworks.go.apiv1.datasharing.settings

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.datasharing.settings.representers.DataSharingSettingsRepresenter
import com.thoughtworks.go.domain.DataSharingSettings
import com.thoughtworks.go.server.service.DataSharingService
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.util.TimeProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class DataSharingSettingsControllerV1DelegateTest implements SecurityServiceTrait, ControllerTrait<DataSharingSettingsControllerV1Delegate> {
    @BeforeEach
    void setUp() {
        initMocks(this)
    }

    @Mock
    DataSharingService dataSharingService
    @Mock
    EntityHashingService entityHashingService
    @Mock
    TimeProvider timeProvider

    @Override
    DataSharingSettingsControllerV1Delegate createControllerInstance() {
        new DataSharingSettingsControllerV1Delegate(new ApiAuthenticationHelper(securityService, goConfigService), dataSharingService, entityHashingService, timeProvider)
    }

    @Nested
    class getDataSharingSettings {
        @Nested
        class Security implements SecurityTestTrait, NormalUserSecurity {

            @Override
            String getControllerMethodUnderTest() {
                return "getDataSharingSettings"
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
            void 'get data sharing settings'() {
                def dataSharingSettings = new DataSharingSettings(false, "Bob", new Date())

                when(dataSharingService.getDataSharingSettings()).thenReturn(dataSharingSettings)
                def etag = "md5"
                when(entityHashingService.md5ForEntity(any() as DataSharingSettings)).thenReturn(etag)

                getWithApiHeader(controller.controllerPath())

                assertThatResponse()
                  .isOk()
                  .hasContentType(controller.mimeType)
                  .hasEtag('"' + etag + '"')
                  .hasBodyWithJsonObject(dataSharingSettings, DataSharingSettingsRepresenter.class)
            }
        }
    }


    @Nested
    class updateDataSharingSettings {
        @Nested
        class Security implements SecurityTestTrait, AdminUserSecurity {

            @Override
            String getControllerMethodUnderTest() {
                return "patchDataSharingSettings"
            }

            @Override
            void makeHttpCall() {
                patch(controller.controllerPath(), [:])
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
            void 'update data sharing settings'() {
                def newConsent = false

                def data = [allow: newConsent]

                def settings = new DataSharingSettings()
                settings.setAllowSharing(newConsent)
                settings.setUpdatedBy("Default")
                def captor = ArgumentCaptor.forClass(DataSharingSettings.class)
                doNothing().when(dataSharingService).updateDataSharingSettings(any())
                doReturn(settings).when(dataSharingService).getDataSharingSettings()
                when(entityHashingService.md5ForEntity(settings)).thenReturn("cached-md5")

                def headers = [
                  'accept'      : controller.mimeType,
                  'If-Match'    : 'cached-md5',
                  'content-type': 'application/json'
                ]

                patchWithApiHeader(controller.controllerBasePath(), headers, data)

                assertThatResponse()
                        .isOk()
                        .hasContentType(controller.mimeType)
                        .hasBodyWithJsonObject(settings, DataSharingSettingsRepresenter.class)

                verify(dataSharingService).updateDataSharingSettings(captor.capture())
                def settingsBeingSaved = captor.getValue()
                assertEquals(settingsBeingSaved.allowSharing(), newConsent)
                assertEquals(settingsBeingSaved.updatedBy(), currentUsername().getUsername().toString())
            }

            @Test
            void 'should save with previous value of allow flag if the flag is not provided in the patch request'() {
                def data = [junk: ""]

                def settings = new DataSharingSettings()
                settings.setAllowSharing(false)
                settings.setUpdatedBy("user1")
                def captor = ArgumentCaptor.forClass(DataSharingSettings.class)
                doNothing().when(dataSharingService).updateDataSharingSettings(any())
                doReturn(settings).when(dataSharingService).getDataSharingSettings()
                when(entityHashingService.md5ForEntity(settings)).thenReturn("cached-md5")

                def headers = [
                  'accept'      : controller.mimeType,
                  'If-Match'    : 'cached-md5',
                  'content-type': 'application/json'
                ]

                patchWithApiHeader(controller.controllerBasePath(), headers, data)

                assertThatResponse()
                  .isOk()
                  .hasContentType(controller.mimeType)
                  .hasBodyWithJsonObject(settings, DataSharingSettingsRepresenter.class)

                verify(dataSharingService).updateDataSharingSettings(captor.capture())
                def settingsBeingSaved = captor.getValue()
                assertEquals(settingsBeingSaved.allowSharing(), settings.allowSharing())
                assertEquals(settingsBeingSaved.updatedBy(), currentUsername().getUsername().toString())
            }

            @Test
            void 'should reject update if old etag is provided'() {
                def data = [allow: true]
                def settings = new DataSharingSettings()
                doReturn(settings).when(dataSharingService).getDataSharingSettings()
                when(entityHashingService.md5ForEntity(settings)).thenReturn("new-md5")
                def headers = [
                  'accept'      : controller.mimeType,
                  'If-Match'    : 'old-md5',
                  'content-type': 'application/json'
                ]

                patchWithApiHeader(controller.controllerBasePath(), headers, data)
                assertThatResponse()
                  .isPreconditionFailed()
                  .hasJsonMessage("Someone has modified the entity. Please update your copy with the changes and try again.")
                verify(dataSharingService, never()).updateDataSharingSettings(any());
            }
        }
    }
}

