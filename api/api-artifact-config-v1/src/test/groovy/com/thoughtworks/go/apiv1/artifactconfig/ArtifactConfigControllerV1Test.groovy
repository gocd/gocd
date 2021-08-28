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
package com.thoughtworks.go.apiv1.artifactconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.config.*
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException
import com.thoughtworks.go.domain.ConfigErrors
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.ServerConfigService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.apiv1.artifactconfig.represernter.ArtifactConfigRepresenter.toJSON
import static org.mockito.Mockito.*

@MockitoSettings(strictness = Strictness.LENIENT)
class ArtifactConfigControllerV1Test implements SecurityServiceTrait, ControllerTrait<ArtifactConfigControllerV1> {

    @Mock
    ServerConfigService serverConfigService

    @Mock
    EntityHashingService entityHashingService

    @Override
    ArtifactConfigControllerV1 createControllerInstance() {
        new ArtifactConfigControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService, serverConfigService)
    }

    @Nested
    class Show {
        @Nested
        class Security implements SecurityTestTrait, AdminUserSecurity {

            @Override
            String getControllerMethodUnderTest() {
                return "show"
            }

            @Override
            void makeHttpCall() {
                getWithApiHeader(controller.controllerBasePath())
            }
        }

        @Nested
        class AsAdmin {

            ArtifactConfig artifactConfig

            @BeforeEach
            void setUp() {
                enableSecurity()
                loginAsAdmin()

                artifactConfig = new ArtifactConfig()
                def purgeSettings = new PurgeSettings()
                purgeSettings.setPurgeStart(new PurgeStart(20.0))
                purgeSettings.setPurgeUpto(new PurgeUpto(50.0))
                artifactConfig.setArtifactsDir(new ArtifactDirectory("artifacts-dir"))
                artifactConfig.setPurgeSettings(purgeSettings)
            }

            @Test
            void 'should get artifact configs'() {
                when(serverConfigService.getArtifactsConfig()).thenReturn(artifactConfig)

                getWithApiHeader(controller.controllerBasePath())

                assertThatResponse()
                        .isOk()
                        .hasBodyWithJson(toObjectString({ toJSON(it, artifactConfig) }))

                verify(serverConfigService, times(1)).getArtifactsConfig()
                verifyNoMoreInteractions(serverConfigService)

            }
        }
    }

    @Nested
    class Update {
        @Nested
        class Security implements SecurityTestTrait, AdminUserSecurity {

            @Override
            String getControllerMethodUnderTest() {
                return "update"
            }

            @Override
            void makeHttpCall() {
                putWithApiHeader(controller.controllerBasePath(), '{}')
            }
        }

        @Nested
        class AsAdmin {

            ArtifactConfig artifactConfig

            @BeforeEach
            void setUp() {
                enableSecurity()
                loginAsAdmin()

                artifactConfig = new ArtifactConfig()
                def purgeSettings = new PurgeSettings()
                purgeSettings.setPurgeStart(new PurgeStart(20.0))
                purgeSettings.setPurgeUpto(new PurgeUpto(50.0))
                artifactConfig.setArtifactsDir(new ArtifactDirectory("artifacts-dir"))
                artifactConfig.setPurgeSettings(purgeSettings)
            }

            @Test
            void 'should update artifacts config if etag matches'() {
                def updatedArtifactConfig = new ArtifactConfig()
                updatedArtifactConfig.setArtifactsDir(new ArtifactDirectory("updated-dir"))
                def purgeSettings = new PurgeSettings()
                purgeSettings.setPurgeStart(new PurgeStart(10.0))
                purgeSettings.setPurgeUpto(new PurgeUpto(20.0))
                updatedArtifactConfig.setPurgeSettings(purgeSettings)

                when(serverConfigService.getArtifactsConfig()).thenReturn(artifactConfig, updatedArtifactConfig)
                when(entityHashingService.hashForEntity(artifactConfig)).thenReturn('some-digest')
                when(entityHashingService.hashForEntity(updatedArtifactConfig)).thenReturn('some-another-digest')

                def jsonPayload = [
                        "artifacts_dir" : "updated-dir",
                        "purge_settings": [
                                "purge_start_disk_space": 10.0,
                                "purge_upto_disk_space" : 20.0
                        ]
                ]
                putWithApiHeader(controller.controllerBasePath(), ['if-match': 'some-digest'], jsonPayload)

                assertThatResponse()
                        .isOk()
                        .hasEtag('"some-another-digest"')
                        .hasBodyWithJson(toObjectString({ toJSON(it, updatedArtifactConfig) }))
            }

            @Test
            void 'should not update artifacts config if etag does not matches'() {
                def updatedArtifactConfig = new ArtifactConfig()
                updatedArtifactConfig.setArtifactsDir(new ArtifactDirectory("updated-dir"))

                when(serverConfigService.getArtifactsConfig()).thenReturn(artifactConfig)
                when(entityHashingService.hashForEntity(artifactConfig)).thenReturn('some-digest')

                def jsonPayload = [
                        "artifacts_dir" : "updated-dir",
                        "purge_settings": [
                                "purge_start_disk_space": 10.0,
                                "purge_upto_disk_space" : 20.0
                        ]
                ]
                putWithApiHeader(controller.controllerBasePath(), ['if-match': 'some-another-digest'], jsonPayload)

                assertThatResponse()
                        .isPreconditionFailed()
                        .hasContentType(controller.mimeType)
                        .hasJsonMessage("Someone has modified the entity. Please update your copy with the changes and try again.")
            }

            @Test
            void 'should return 422 for validation errors'() {
                when(serverConfigService.getArtifactsConfig()).thenReturn(artifactConfig)
                when(entityHashingService.hashForEntity(artifactConfig)).thenReturn('some-digest')

                def jsonPayload = [
                        "artifacts_dir" : "",
                        "purge_settings": [
                                "purge_start_disk_space": 10.0,
                                "purge_upto_disk_space" : 20.0
                        ]
                ]

                def cruiseConfig = mock(BasicCruiseConfig.class)
                def e = new GoConfigInvalidException(cruiseConfig, "Validation failed")
                ConfigErrors errors = new ConfigErrors()
                errors.add("artifactDir", "it should not be blank.")
                def errorMsg = "Validations failed for artifacts. Error(s): [it should not be blank.]. Please correct and resubmit."

                when(cruiseConfig.getAllErrors()).thenReturn(Arrays.asList(errors))
                doThrow(e).when(serverConfigService).updateArtifactConfig(Mockito.any() as ArtifactConfig)

                putWithApiHeader(controller.controllerBasePath(), ['if-match': 'some-digest'], jsonPayload)

                assertThatResponse()
                        .isUnprocessableEntity()
                        .hasContentType(controller.mimeType)
                        .hasJsonMessage(errorMsg)
            }
        }
    }
}
