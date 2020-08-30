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

package com.thoughtworks.go.apiv1.artifacts

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.server.service.ArtifactsService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyInt
import static org.mockito.Mockito.doAnswer
import static org.mockito.MockitoAnnotations.initMocks

class ArtifactsControllerV1Test implements SecurityServiceTrait, ControllerTrait<ArtifactsControllerV1> {
  @Mock
  ArtifactsService artifactsService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  ArtifactsControllerV1 createControllerInstance() {
    new ArtifactsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), artifactsService)
  }

  @Nested
  class Purge {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "purgeOldArtifacts"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath('purge'), [:])
      }
    }

    @Nested
    class AsAdmin {

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should show errors occurred while purging artifacts'() {

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(1)
          result.internalServerError("boo")
          return result
        }).when(artifactsService).purgeOldArtifacts(anyInt(), any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerPath('purge'), [purge_threshold: 2])

        assertThatResponse()
          .isInternalServerError()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("boo")
      }

      @Test
      void 'should purge artifacts'() {
        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(1)
          result.setMessage("eligible artifacts purged")
        }).when(artifactsService).purgeOldArtifacts(anyInt(), any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerPath('purge'), [purge_threshold: 2])

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("eligible artifacts purged")
      }
    }

  }
}
