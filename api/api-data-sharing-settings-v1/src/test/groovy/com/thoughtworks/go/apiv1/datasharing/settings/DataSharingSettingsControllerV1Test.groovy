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
package com.thoughtworks.go.apiv1.datasharing.settings

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.datasharing.settings.representers.DataSharingSettingsRepresenter
import com.thoughtworks.go.server.domain.DataSharingSettings
import com.thoughtworks.go.server.service.datasharing.DataSharingNotification
import com.thoughtworks.go.server.service.datasharing.DataSharingSettingsService
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

class DataSharingSettingsControllerV1Test implements SecurityServiceTrait, ControllerTrait<DataSharingSettingsControllerV1> {
    @BeforeEach
    void setUp() {
        initMocks(this)
    }

    @Mock
    DataSharingSettingsService dataSharingSettingsService
    @Mock
    EntityHashingService entityHashingService
    @Mock
    TimeProvider timeProvider
    @Mock
    DataSharingNotification dataSharingNotification

    @Override
    DataSharingSettingsControllerV1 createControllerInstance() {
        new DataSharingSettingsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService),
                dataSharingSettingsService, entityHashingService, timeProvider, dataSharingNotification)
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

                when(dataSharingSettingsService.get()).thenReturn(dataSharingSettings)
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
    class getDataSharingNotificationForCurrentUser {
        @Nested
        class Security implements SecurityTestTrait, NormalUserSecurity {

            @Override
            String getControllerMethodUnderTest() {
                return "getDataSharingNotificationForCurrentUser"
            }

            @Override
            void makeHttpCall() {
                getWithApiHeader(controller.controllerPath('/notification_auth'))
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
            void 'get data sharing settings notification'() {
                when(dataSharingNotification.allowNotificationFor(currentUsername())).thenReturn(true);
                getWithApiHeader(controller.controllerPath('/notification_auth'));

                assertThatResponse()
                  .isOk()
                  .hasContentType(controller.mimeType)
                  .hasJsonBody("{\"show_notification\" : true }");
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
                doNothing().when(dataSharingSettingsService).createOrUpdate(any())
                doReturn(settings).when(dataSharingSettingsService).get()

                def headers = [
                  'accept'      : controller.mimeType,
                  'content-type': 'application/json'
                ]

                patchWithApiHeader(controller.controllerBasePath(), headers, data)

                assertThatResponse()
                        .isOk()
                        .hasContentType(controller.mimeType)
                        .hasBodyWithJsonObject(settings, DataSharingSettingsRepresenter.class)

                verify(dataSharingSettingsService).createOrUpdate(captor.capture())
                def settingsBeingSaved = captor.getValue()
                assertEquals(settingsBeingSaved.allowSharing(), newConsent)
                assertEquals(settingsBeingSaved.updatedBy(), currentUsernameString())
            }

            @Test
            void 'should save with previous value of allow flag if the flag is not provided in the patch request'() {
                def data = [junk: ""]

                def settings = new DataSharingSettings()
                settings.setAllowSharing(false)
                settings.setUpdatedBy("user1")
                def captor = ArgumentCaptor.forClass(DataSharingSettings.class)
                doNothing().when(dataSharingSettingsService).createOrUpdate(any())
                doReturn(settings).when(dataSharingSettingsService).get()

                def headers = [
                  'accept'      : controller.mimeType,
                  'content-type': 'application/json'
                ]

                patchWithApiHeader(controller.controllerBasePath(), headers, data)

                assertThatResponse()
                  .isOk()
                  .hasContentType(controller.mimeType)
                  .hasBodyWithJsonObject(settings, DataSharingSettingsRepresenter.class)

                verify(dataSharingSettingsService).createOrUpdate(captor.capture())
                def settingsBeingSaved = captor.getValue()
                assertEquals(settingsBeingSaved.allowSharing(), settings.allowSharing())
                assertEquals(settingsBeingSaved.updatedBy(), currentUsernameString())
            }
        }
    }
}
