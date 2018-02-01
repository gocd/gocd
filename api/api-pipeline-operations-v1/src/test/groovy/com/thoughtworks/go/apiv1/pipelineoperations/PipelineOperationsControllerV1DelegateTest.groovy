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

package com.thoughtworks.go.apiv1.pipelineoperations

import com.thoughtworks.go.api.ClearSingletonExtension
import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.i18n.LocalizedMessage
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.PipelinePauseService
import com.thoughtworks.go.server.service.PipelineUnlockApiService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.server.service.result.HttpOperationResult
import com.thoughtworks.go.serverhealth.HealthStateScope
import com.thoughtworks.go.serverhealth.HealthStateType
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.PipelineGroupOperateUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.doAnswer
import static org.mockito.MockitoAnnotations.initMocks

@ExtendWith(ClearSingletonExtension.class)
class PipelineOperationsControllerV1DelegateTest implements SecurityServiceTrait, ControllerTrait<PipelineOperationsControllerV1Delegate> {
  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Mock
  PipelinePauseService pipelinePauseService
  @Mock
  PipelineUnlockApiService pipelineUnlockApiService

  @Override
  PipelineOperationsControllerV1Delegate createControllerInstance() {
    new PipelineOperationsControllerV1Delegate(pipelinePauseService, pipelineUnlockApiService, new ApiAuthenticationHelper(securityService, goConfigService), localizer)
  }

  @Nested
  class Pause {
    private String pipelineName = "up42"
    @Nested
    class Security implements SecurityTestTrait, PipelineGroupOperateUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "pause"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(pipelineName, 'pause'), [:])
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
      void 'should pause a pipeline'() {

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(3)
          result.setMessage(LocalizedMessage.string("PIPELINE_PAUSE_SUCCESSFUL", pipelineName))
          return result
        }).when(pipelinePauseService).pause(any() as String, any() as String, any() as Username, any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'pause'), [:])

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("PIPELINE_PAUSE_SUCCESSFUL")
      }

      @Test
      void 'should show errors occurred while pausing a pipeline'() {

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(3)
          result.conflict(LocalizedMessage.string("PIPELINE_ALREADY_PAUSED", pipelineName))
          return result
        }).when(pipelinePauseService).pause(any() as String, any() as String, any() as Username, any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'pause'), [:])

        assertThatResponse()
          .isConflict()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("PIPELINE_ALREADY_PAUSED")
      }
    }

    @Nested
    class AsPipelineGroupOperateUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupOperateUser()
      }

      @Test
      void 'should pause a pipeline'() {

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(3)
          result.setMessage(LocalizedMessage.string("PIPELINE_PAUSE_SUCCESSFUL", pipelineName))
          return result
        }).when(pipelinePauseService).pause(any() as String, any() as String, any() as Username, any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'pause'), [:])

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("PIPELINE_PAUSE_SUCCESSFUL")
      }

      @Test
      void 'should show errors occurred while pausing a pipeline'() {

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(3)
          result.conflict(LocalizedMessage.string("PIPELINE_ALREADY_PAUSED", pipelineName))
          return result
        }).when(pipelinePauseService).pause(any() as String, any() as String, any() as Username, any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'pause'), [:])

        assertThatResponse()
          .isConflict()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("PIPELINE_ALREADY_PAUSED")
      }
    }
  }

  @Nested
  class Unpause {
    private String pipelineName = "up42"
    @Nested
    class Security implements SecurityTestTrait, PipelineGroupOperateUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "unpause"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(pipelineName, 'unpause'), [:])
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
      void 'should unpause a pipeline'() {

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(2)
          result.setMessage(LocalizedMessage.string("PIPELINE_UNPAUSE_SUCCESSFUL", pipelineName))
          return result
        }).when(pipelinePauseService).unpause(any() as String, any() as Username, any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'unpause'), [:])

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("PIPELINE_UNPAUSE_SUCCESSFUL")
      }

      @Test
      void 'should show errors occurred while pausing a pipeline'() {

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(2)
          result.conflict(LocalizedMessage.string("PIPELINE_ALREADY_UNPAUSED", pipelineName))
          return result
        }).when(pipelinePauseService).unpause(any() as String, any() as Username, any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'unpause'), [:])

        assertThatResponse()
          .isConflict()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("PIPELINE_ALREADY_UNPAUSED")
      }
    }

    @Nested
    class AsPipelineGroupOperateUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupOperateUser()
      }

      @Test
      void 'should pause a pipeline'() {

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(2)
          result.setMessage(LocalizedMessage.string("PIPELINE_UNPAUSE_SUCCESSFUL", pipelineName))
          return result
        }).when(pipelinePauseService).unpause(any() as String, any() as Username, any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'unpause'), [:])

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("PIPELINE_UNPAUSE_SUCCESSFUL")
      }

      @Test
      void 'should show errors occurred while pausing a pipeline'() {

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(2)
          result.conflict(LocalizedMessage.string("PIPELINE_ALREADY_UNPAUSED", pipelineName))
          return result
        }).when(pipelinePauseService).unpause(any() as String, any() as Username, any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'unpause'), [:])

        assertThatResponse()
          .isConflict()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("PIPELINE_ALREADY_UNPAUSED")
      }
    }
  }

  @Nested
  class Unlock {
    private String pipelineName = "up42"
    @Nested
    class Security implements SecurityTestTrait, PipelineGroupOperateUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "unlock"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(pipelineName, 'unlock'), [:])
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
      void 'should unlock a pipeline'() {
        doAnswer({ InvocationOnMock invocation ->
          HttpOperationResult result = invocation.getArgument(2)
          result.ok("unlocked!")
          return result
        }).when(pipelineUnlockApiService).unlock(any() as String, any() as Username, any() as HttpOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'unlock'), [:])

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("unlocked!")
      }

      @Test
      void 'should show errors occurred while pausing a pipeline'() {
        doAnswer({ InvocationOnMock invocation ->
          HttpOperationResult result = invocation.getArgument(2)
          result.conflict("pipeline is not locked", "pipeline is not locked", HealthStateType.general(HealthStateScope.forPipeline(pipelineName)))
          return result
        }).when(pipelineUnlockApiService).unlock(any() as String, any() as Username, any() as HttpOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'unlock'), [:])

        assertThatResponse()
          .isConflict()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("pipeline is not locked")
      }
    }

    @Nested
    class AsPipelineGroupOperateUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupOperateUser()
      }

      @Test
      void 'should unlock a pipeline'() {
        doAnswer({ InvocationOnMock invocation ->
          HttpOperationResult result = invocation.getArgument(2)
          result.ok("unlocked!")
          return result
        }).when(pipelineUnlockApiService).unlock(any() as String, any() as Username, any() as HttpOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'unlock'), [:])

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("unlocked!")
      }

      @Test
      void 'should show errors occurred while unlocking a pipeline'() {
        doAnswer({ InvocationOnMock invocation ->
          HttpOperationResult result = invocation.getArgument(2)
          result.conflict("pipeline is not locked", "pipeline is not locked", HealthStateType.general(HealthStateScope.forPipeline(pipelineName)))
          return result
        }).when(pipelineUnlockApiService).unlock(any() as String, any() as Username, any() as HttpOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'unlock'), [:])

        assertThatResponse()
          .isConflict()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("pipeline is not locked")
      }
    }
  }

}

