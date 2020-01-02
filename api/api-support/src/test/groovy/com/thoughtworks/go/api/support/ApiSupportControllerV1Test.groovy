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
package com.thoughtworks.go.api.support


import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.server.service.support.ServerStatusService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.doAnswer
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class ApiSupportControllerV1Test implements SecurityServiceTrait, ControllerTrait<ApiSupportController> {
  @Mock
  private ServerStatusService serverStatusService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  ApiSupportController createControllerInstance() {
    new ApiSupportController(serverStatusService)
  }

  @Nested
  class Get {

    @Test
    void 'should return agent json'() {
      loginAsAdmin()
      def res = [foo: "bar"]
      when(serverStatusService.asJson(any() as Username, any() as HttpLocalizedOperationResult)).thenReturn(res)

      get(controller.controllerPath())

      assertThatResponse()
        .isOk()
        .hasJsonBody(res)
        .hasContentType("application/json")
    }

    @Test
    void 'should return error response'() {
      loginAsAdmin()
      def message = "Failed to get information"
      doAnswer({ InvocationOnMock invocation ->
        def result = invocation.getArgument(1) as HttpLocalizedOperationResult
        result.unprocessableEntity(message)
      }).when(serverStatusService).asJson(eq(currentUsername()), any() as HttpLocalizedOperationResult)

      get(controller.controllerPath())

      assertThatResponse()
        .isUnprocessableEntity()
        .hasJsonMessage(message)
        .hasContentType("application/json")
    }
  }

  @Nested
  class ProcessList {
    @Test
    void 'should return process list json'() {
      get(controller.controllerPath(Routes.Support.PROCESS_LIST))

      assertThatResponse()
        .isOk()
        .hasContentType("application/json")
    }
  }
}
