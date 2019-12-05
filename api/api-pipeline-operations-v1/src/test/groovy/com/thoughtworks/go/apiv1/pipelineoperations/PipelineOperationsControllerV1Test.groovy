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

package com.thoughtworks.go.apiv1.pipelineoperations

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.pipelineoperations.representers.TriggerOptions
import com.thoughtworks.go.apiv1.pipelineoperations.representers.TriggerWithOptionsViewRepresenter
import com.thoughtworks.go.config.EnvironmentVariableConfig
import com.thoughtworks.go.config.EnvironmentVariablesConfig
import com.thoughtworks.go.config.exceptions.EntityType
import com.thoughtworks.go.config.exceptions.RecordNotFoundException
import com.thoughtworks.go.domain.JobResult
import com.thoughtworks.go.domain.JobState
import com.thoughtworks.go.domain.MaterialRevisions
import com.thoughtworks.go.domain.PipelinePauseInfo
import com.thoughtworks.go.domain.buildcause.BuildCause
import com.thoughtworks.go.helper.EnvironmentVariablesConfigMother
import com.thoughtworks.go.helper.MaterialConfigsMother
import com.thoughtworks.go.helper.MaterialsMother
import com.thoughtworks.go.helper.ModificationsMother
import com.thoughtworks.go.presentation.PipelineStatusModel
import com.thoughtworks.go.presentation.pipelinehistory.JobHistory
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels
import com.thoughtworks.go.security.GoCipher
import com.thoughtworks.go.server.domain.MaterialForScheduling
import com.thoughtworks.go.server.domain.PipelineScheduleOptions
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.PipelineHistoryService
import com.thoughtworks.go.server.service.PipelinePauseService
import com.thoughtworks.go.server.service.PipelineTriggerService
import com.thoughtworks.go.server.service.PipelineUnlockApiService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.server.service.result.HttpOperationResult
import com.thoughtworks.go.serverhealth.HealthStateScope
import com.thoughtworks.go.serverhealth.HealthStateType
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.PipelineGroupOperateUserSecurity
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.doAnswer
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class PipelineOperationsControllerV1Test implements SecurityServiceTrait, ControllerTrait<PipelineOperationsControllerV1> {
  @Mock
  PipelineHistoryService pipelineHistoryService
  @Mock
  PipelinePauseService pipelinePauseService
  @Mock
  PipelineUnlockApiService pipelineUnlockApiService
  @Mock
  PipelineTriggerService pipelineTriggerService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  PipelineOperationsControllerV1 createControllerInstance() {
    new PipelineOperationsControllerV1(pipelinePauseService, pipelineUnlockApiService, pipelineTriggerService, new ApiAuthenticationHelper(securityService, goConfigService), goConfigService, pipelineHistoryService)
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

      @Override
      String getPipelineName() {
        return Pause.this.pipelineName
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
      void 'should pause a pipeline'() {

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(3)
          result.setMessage("success!")
          return result
        }).when(pipelinePauseService).pause(any() as String, any() as String, any() as Username, any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'pause'), [:])

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("success!")
      }

      @Test
      void 'should pause a pipeline when no pause cause is given'() {

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(3)
          result.setMessage("success!")
          return result
        }).when(pipelinePauseService).pause(any() as String, any() as String, any() as Username, any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'pause'), ['x-gocd-confirm': 'blah'], null)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("success!")
      }

      @Test
      void 'should show errors occurred while pausing a pipeline'() {

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(3)
          result.conflict("already paused")
          return result
        }).when(pipelinePauseService).pause(any() as String, any() as String, any() as Username, any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'pause'), [:])

        assertThatResponse()
          .isConflict()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("already paused")
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

      @Override
      String getPipelineName() {
        return Unpause.this.pipelineName
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
      void 'should unpause a pipeline'() {

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(2)
          result.setMessage("success!")
          return result
        }).when(pipelinePauseService).unpause(any() as String, any() as Username, any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'unpause'), [:])

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("success!")
      }

      @Test
      void 'should show errors occurred while pausing a pipeline'() {

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(2)
          result.conflict("already unpaused!")
          return result
        }).when(pipelinePauseService).unpause(any() as String, any() as Username, any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'unpause'), [:])

        assertThatResponse()
          .isConflict()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("already unpaused!")
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

      @Override
      String getPipelineName() {
        return Unlock.this.pipelineName
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
          .hasJsonMessage("pipeline is not locked { pipeline is not locked }")
      }
    }
  }

  @Nested
  class TriggerOptionsTest {

    @Nested
    class Security implements PipelineGroupOperateUserSecurity, SecurityTestTrait {

      @Override
      String getControllerMethodUnderTest() {
        return "triggerOptions"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(Routes.Pipeline.triggerOptions('build-linux'))
      }

      @Override
      String getPipelineName() {
        return 'build-linux'
      }
    }

    @Nested
    class AsAuthorizedUser {
      @Test
      void 'should render trigger options'() {
        MaterialRevisions revisions = ModificationsMother.modifyOneFile(MaterialsMother.defaultSvnMaterialsWithUrl("http://example.com/svn/project"), "revision")

        StageInstanceModels stages = new StageInstanceModels()
        stages.addStage("unit1", JobHistory.withJob("test", JobState.Completed, JobResult.Passed, new Date()))
        stages.addFutureStage("unit2", false)

        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createWithModifications(revisions, "bob"), stages)
        model.setMaterialConfigs(MaterialConfigsMother.defaultSvnMaterialConfigsWithUrl("http://example.com/svn/project"))

        EnvironmentVariablesConfig variables = EnvironmentVariablesConfigMother.environmentVariables()

        when(goConfigService.variablesFor("build-linux")).thenReturn(variables)
        when(pipelineHistoryService.latest("build-linux", currentUsername())).thenReturn(model)

        getWithApiHeader(Routes.Pipeline.triggerOptions('build-linux'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(new TriggerOptions(variables, model), TriggerWithOptionsViewRepresenter.class)
      }

      @Test
      void 'should render 404 if bad pipeline is provided'() {
        when(goConfigService.variablesFor("pipeline-that-is-not-present")).thenThrow(new RecordNotFoundException(EntityType.Pipeline, "pipeline-that-is-not-present"))

        getWithApiHeader(Routes.Pipeline.triggerOptions('pipeline-that-is-not-present'))

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(EntityType.Pipeline.notFoundMessage('pipeline-that-is-not-present'))
      }
    }
  }

  @Nested
  class Schedule {
    private String pipelineName = "up42"
    @Nested
    class Security implements SecurityTestTrait, PipelineGroupOperateUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "schedule"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(pipelineName, 'schedule'), [:])
      }

      @Override
      String getPipelineName() {
        return Schedule.this.pipelineName
      }
    }

    @Nested
    class AsAuthorizedUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupOperateUser(pipelineName)
      }

      @Test
      void 'should schedule a pipeline'() {
        doAnswer({ InvocationOnMock invocation ->
          HttpOperationResult result = invocation.getArgument(3)
          result.ok("scheduled!")
          return result
        }).when(pipelineTriggerService).schedule(eq(pipelineName), any() as PipelineScheduleOptions, any() as Username, any() as HttpOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'schedule'), [:])

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("scheduled!")
      }

      @Test
      void 'should schedule a pipeline with provided options'() {
        def pipelineScheduleOptions = new PipelineScheduleOptions()
        pipelineScheduleOptions.shouldPerformMDUBeforeScheduling(true);
        def goCipher = new GoCipher()
        pipelineScheduleOptions.getAllEnvironmentVariables().add(new EnvironmentVariableConfig(goCipher, "VAR1", "overridden_value", false))
        pipelineScheduleOptions.getAllEnvironmentVariables().add(new EnvironmentVariableConfig(goCipher, "SEC_VAR1", "overridden_secure_value", true))
        pipelineScheduleOptions.getMaterials().add(new MaterialForScheduling("repo1", "revision1"))
        pipelineScheduleOptions.getMaterials().add(new MaterialForScheduling("repo2", "revision2"))

        doAnswer({ InvocationOnMock invocation ->
          HttpOperationResult result = invocation.getArgument(3)
          result.ok("scheduled!")
          return result
        }).when(pipelineTriggerService).schedule(eq(pipelineName), eq(pipelineScheduleOptions), any() as Username, any() as HttpOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'schedule'), convertPipelineScheduleOptionsToJSON(pipelineScheduleOptions))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("scheduled!")
      }

      @Test
      void 'should schedule a pipeline with encrypted variable'() {
        def pipelineScheduleOptions = new PipelineScheduleOptions()
        pipelineScheduleOptions.shouldPerformMDUBeforeScheduling(false);
        pipelineScheduleOptions.getAllEnvironmentVariables().add(new EnvironmentVariableConfig(new GoCipher(), "SEC_VAR1", new GoCipher().encrypt("ENCRYPTED")))

        doAnswer({ InvocationOnMock invocation ->
          HttpOperationResult result = invocation.getArgument(3)
          result.ok("scheduled!")
          return result
        }).when(pipelineTriggerService).schedule(eq(pipelineName), eq(pipelineScheduleOptions), any() as Username, any() as HttpOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'schedule'), convertPipelineScheduleOptionsToJSON(pipelineScheduleOptions))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("scheduled!")
      }

      @Test
      void 'should show errors occurred while schedule a pipeline'() {
        doAnswer({ InvocationOnMock invocation ->
          HttpOperationResult result = invocation.getArgument(3)
          result.conflict("pipeline is not scheduled", "reason", HealthStateType.general(HealthStateScope.forPipeline(pipelineName)))
          return result
        }).when(pipelineTriggerService).schedule(eq(pipelineName), any() as PipelineScheduleOptions, any() as Username, any() as HttpOperationResult)

        postWithApiHeader(controller.controllerPath(pipelineName, 'schedule'), [:])

        assertThatResponse()
          .isConflict()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("pipeline is not scheduled { reason }")
      }

    }

    private Map convertPipelineScheduleOptionsToJSON(PipelineScheduleOptions scheduleOptions) {

      def scheduleOptionsJson = new HashMap<>()
      def materials = new ArrayList<>()
      def environmentVariables = new ArrayList<>()
      for (MaterialForScheduling materialForScheduling : scheduleOptions.getMaterials()) {
        def materialOptions = new HashMap()
        materialOptions.put("fingerprint", materialForScheduling.getFingerprint())
        materialOptions.put("revision", materialForScheduling.getRevision())
        materials.add(materialOptions)
      }
      for (EnvironmentVariableConfig environmentVariable : scheduleOptions.getAllEnvironmentVariables()) {
        def envVar = new HashMap<>()
        envVar.put("name", environmentVariable.getName())
        envVar.put("value", environmentVariable.getValue())
        try {
          envVar.put("encrypted_value", environmentVariable.getEncryptedValue())
        } catch (Exception) {
        }
        envVar.put("secure", environmentVariable.isSecure())
        environmentVariables.add(envVar)
      }
      scheduleOptionsJson.put("update_materials_before_scheduling", scheduleOptions.shouldPerformMDUBeforeScheduling())
      scheduleOptionsJson.put("materials", materials)
      scheduleOptionsJson.put("environment_variables", environmentVariables)
      return scheduleOptionsJson;
    }

  }

  @Nested
  class Status {
    private String pipelineName = "up42"
    @Nested
    class Security implements SecurityTestTrait, PipelineGroupOperateUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "getStatusInfo"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(pipelineName, 'status'), [:])
      }

      @Override
      String getPipelineName() {
        return Status.this.pipelineName
      }
    }

    @Nested
    class AsAuthorizedUser {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupOperateUser(pipelineName)
      }

      @Test
      void 'should return the pipeline status'() {
        def pipelineStatusModel = new PipelineStatusModel(false, true, new PipelinePauseInfo(true, "some pause cause", "admin"))

        when(pipelineHistoryService.getPipelineStatus(eq(pipelineName), anyString(), any(HttpOperationResult.class))).thenReturn(pipelineStatusModel)

        getWithApiHeader(controller.controllerPath(pipelineName, 'status'), [:])

        def expectedJson = [
          "paused"      : true,
          "paused_cause": "some pause cause",
          "paused_by"   : "admin",
          "locked"      : false,
          "schedulable" : true
        ]

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody(expectedJson)
      }


    }
  }
}
