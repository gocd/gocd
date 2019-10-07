/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.compare

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.compare.representers.ComparisonRepresenter
import com.thoughtworks.go.server.service.ChangesetService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.serverhealth.HealthStateScope
import com.thoughtworks.go.serverhealth.HealthStateType
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static java.util.Collections.emptyList
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class CompareControllerV1Test implements SecurityServiceTrait, ControllerTrait<CompareControllerV1> {

    @Mock
    private ChangesetService changesetService

    @BeforeEach
    void setUp() {
        initMocks(this)
    }

    @Override
    CompareControllerV1 createControllerInstance() {
        new CompareControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), changesetService)
    }

    @Nested
    class Index {

        @BeforeEach
        void setUp() {
            loginAsUser()
        }

        @Nested
        class Security implements SecurityTestTrait, NormalUserSecurity {

            @Override
            String getControllerMethodUnderTest() {
                return "index"
            }

            @Override
            void makeHttpCall() {
                getWithApiHeader(controller.controllerBasePath())
            }
        }

        @Nested
        class AsAuthorizedUser {
            @BeforeEach
            void setUp() {
                enableSecurity()
                loginAsAdmin()
            }

            @Test
            void 'should return list of changes for the pipeline and between the counters specified'() {
                getWithApiHeader(getApi('up42', 1, 4))

                assertThatResponse()
                        .isOk()
                        .hasBodyWithJson(toObjectString({
                    ComparisonRepresenter.toJSON(it, "up42", 1, 4, emptyList())
                }))
            }

            @Test
            void 'should return 404 if pipeline was not found'() {
                when(changesetService.revisionsBetween(anyString(), anyInt(), anyInt(), any(), any(), anyBoolean())).then({ InvocationOnMock invocation ->
                    HttpLocalizedOperationResult result = invocation.getArguments()[4]
                    result.notFound("not found message", HealthStateType.general(HealthStateScope.forPipeline("undefined")))
                })

                getWithApiHeader(getApi('undefined', 1, 1))

                assertThatResponse()
                        .isNotFound()
                        .hasContentType(controller.mimeType)
                        .hasJsonMessage("not found message")
            }

            @Test
            void 'should return forbidden if the user does not have access to view the pipeline'() {
                when(changesetService.revisionsBetween(anyString(), anyInt(), anyInt(), any(), any(), anyBoolean())).then({ InvocationOnMock invocation ->
                    HttpLocalizedOperationResult result = invocation.getArguments()[4]
                    result.forbidden("forbidden message", HealthStateType.general(HealthStateScope.forPipeline("undefined")))
                })

                getWithApiHeader(getApi('undefined', 1, 1))

                assertThatResponse()
                        .isForbidden()
                        .hasContentType(controller.mimeType)
                        .hasJsonMessage("forbidden message")
            }

            @Test
            void 'should return as bad request if fromCounter or toCounter is given as a negative number'() {
                when(changesetService.revisionsBetween(anyString(), anyInt(), anyInt(), any(), any(), anyBoolean())).then({ InvocationOnMock invocation ->
                    HttpLocalizedOperationResult result = invocation.getArguments()[4]
                    result.badRequest("bad request message")
                })

                getWithApiHeader(getApi('undefined', -1, 1))

                assertThatResponse()
                        .isBadRequest()
                        .hasContentType(controller.mimeType)
                        .hasJsonMessage("bad request message")
            }
        }
    }

    String getApi(String pipelineName, Integer fromCounter, Integer toCounter) {
        return "/api/compare/$pipelineName/$fromCounter/$toCounter".toString()
    }
}
