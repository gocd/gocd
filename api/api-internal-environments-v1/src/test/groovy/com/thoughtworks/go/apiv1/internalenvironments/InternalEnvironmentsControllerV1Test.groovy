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

package com.thoughtworks.go.apiv1.internalenvironments

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.internalenvironments.representers.MergedEnvironmentsRepresenter
import com.thoughtworks.go.config.exceptions.EntityType
import com.thoughtworks.go.config.exceptions.RecordNotFoundException
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig
import com.thoughtworks.go.helper.EnvironmentConfigMother
import com.thoughtworks.go.server.service.AgentService
import com.thoughtworks.go.server.service.EnvironmentConfigService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static java.util.Arrays.asList
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalEnvironmentsControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalEnvironmentsControllerV1> {

  @Mock
  EnvironmentConfigService environmentConfigService
  @Mock
  private AgentService agentService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalEnvironmentsControllerV1 createControllerInstance() {
    new InternalEnvironmentsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), environmentConfigService, agentService)
  }

  @Nested
  class Index {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'index'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath())
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        loginAsAdmin()
      }

      @Test
      void 'test should return environments fetched from environments config service'() {
        List<String> envNames = ['environment1', 'environment2']

        when(environmentConfigService.getEnvironmentNames()).thenReturn(envNames)

        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse()
          .isOk()
          .hasBodyWithJson('["environment1","environment2"]')
      }

      @Test
      void 'test should return empty environments list when no environment exists'() {
        when(environmentConfigService.getEnvironmentNames()).thenReturn([])

        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse()
          .isOk()
          .hasBody('[]')
      }
    }
  }

  @Nested
  class IndexMergedEnvironments {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'indexMergedEnvironments'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath('/merged'))
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        loginAsAdmin()
      }

      @Test
      void 'should represent basic environments'() {
        def environmentName1 = "env1"
        def environmentName2 = "env2"
        def env1 = EnvironmentConfigMother.environment(environmentName1)
        def env2 = EnvironmentConfigMother.environment(environmentName2)

        when(environmentConfigService.getAllMergedEnvironments()).thenReturn([env1, env2])

        getWithApiHeader(controller.controllerPath('/merged'))

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(toObjectString({ MergedEnvironmentsRepresenter.toJSON(it, [env1, env2]) }))
      }

      @Test
      void 'should represent merged environments'() {
        def environmentName = "env"
        def env = EnvironmentConfigMother.environment(environmentName)
        def remoteEnv = EnvironmentConfigMother.remote(environmentName)
        def mergeEnvironmentConfig = new MergeEnvironmentConfig(env, remoteEnv)

        when(environmentConfigService.getAllMergedEnvironments()).thenReturn([mergeEnvironmentConfig])

        getWithApiHeader(controller.controllerPath('/merged'))

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(toObjectString({ MergedEnvironmentsRepresenter.toJSON(it, [mergeEnvironmentConfig]) }))
      }

      @Test
      void 'should represent empty environments'() {
        when(environmentConfigService.getAllMergedEnvironments()).thenReturn([])

        getWithApiHeader(controller.controllerPath('/merged'))

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(toObjectString({ MergedEnvironmentsRepresenter.toJSON(it, []) }))
      }
    }
  }

  @Nested
  class UpdateAgentsAssociation {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'updateAgentAssociation'
      }

      @Override
      void makeHttpCall() {
        putWithApiHeader(controller.controllerPath('env'), [])
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        loginAsAdmin()
      }

      @Test
      void 'should allow to associate agents to a given env config name'() {
        def json = [
          uuids: ["agent1", "agent2"]
        ]

        def env = EnvironmentConfigMother.environment("env")
        when(environmentConfigService.getEnvironmentConfig("env")).thenReturn(env)

        putWithApiHeader(controller.controllerPath('env'), json)

        assertThatResponse()
          .isOk()
          .hasJsonMessage("Environment 'env' updated successfully!")

      }

      @Test
      void 'should throw RecordNotFound if env specified does not exist'() {
        def json = [
          uuids: ["agent1", "agent2"]
        ]

        doThrow(new RecordNotFoundException(EntityType.Environment, "unknown-env"))
          .when(environmentConfigService)
          .getEnvironmentConfig("unknown-env")

        putWithApiHeader(controller.controllerPath('unknown-env'), json)

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Environment with name 'unknown-env' was not found!")
      }

      @Test
      void 'should throw RecordNotFound if any agent specified does not exist'() {
        def json = [
          uuids: ["agent1", "agent2"]
        ]

        def env = EnvironmentConfigMother.environment("env")
        when(environmentConfigService.getEnvironmentConfig("env")).thenReturn(env)
        doThrow(new RecordNotFoundException(EntityType.Agent, "agent1"))
          .when(agentService)
          .updateAgentsAssociationOfEnvironment(env, asList("agent1", "agent2"))

        putWithApiHeader(controller.controllerPath('env'), json)

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Agent with uuid 'agent1' was not found!")
      }

      @Test
      void 'should error out if input does not contain property uuids'() {
        def json = [
          uuids1: ["agent1", "agent2"]
        ]

        putWithApiHeader(controller.controllerPath('env'), json)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage('Json `{\\"uuids1\\":[\\"agent1\\",\\"agent2\\"]}` does not contain property \'uuids\'.')
      }
    }

  }
}
