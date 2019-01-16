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

package com.thoughtworks.go.apiv2.environments

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv2.environments.representers.EnvironmentRepresenter
import com.thoughtworks.go.apiv2.environments.representers.EnvironmentsRepresenter
import com.thoughtworks.go.config.BasicEnvironmentConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.EnvironmentConfig
import com.thoughtworks.go.domain.ConfigElementForEdit
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.EnvironmentConfigService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.api.util.HaltApiMessages.notFoundMessage
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class EnvironmentsControllerV2Test implements SecurityServiceTrait, ControllerTrait<EnvironmentsControllerV2> {

  @Mock
  EnvironmentConfigService environmentConfigService

  @Mock
  EntityHashingService entityHashingService

  @Override
  EnvironmentsControllerV2 createControllerInstance() {
    new EnvironmentsControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), environmentConfigService, entityHashingService)

  }

  @BeforeEach
  void setup() {
    initMocks(this)
  }

  @Nested
  class Show {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'show'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath("env1"))
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
      void 'should return a environment with given name'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addAgent("agent1")
        env1.addAgent("agent2")
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        when(entityHashingService.md5ForEntity(env1)).thenReturn("md5-hash")
        when(environmentConfigService.getMergedEnvironmentforDisplay(eq("env1"), any(HttpLocalizedOperationResult.class))).thenReturn(new ConfigElementForEdit(env1, "md5-hash"))

        getWithApiHeader(controller.controllerPath("env1"))

        assertThatResponse()
          .isOk()
          .hasEtag('"md5-hash"')
          .hasBodyWithJsonObject(env1, EnvironmentRepresenter)
      }

      @Test
      void 'should return error when environment with specified name is not found'() {
        when(environmentConfigService.getMergedEnvironmentforDisplay(eq("env1"), any(HttpLocalizedOperationResult.class)))
          .then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.getArguments().last()
          result.badRequest("No such environment")
        })

        getWithApiHeader(controller.controllerPath("env1"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(notFoundMessage())
      }
    }
  }


  @Nested
  class Remove {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'remove'
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath("env1"))
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
      void 'should delete environment with specified name'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addAgent("agent1")
        env1.addAgent("agent2")
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        when(environmentConfigService.getMergedEnvironmentforDisplay(anyString(), any(HttpLocalizedOperationResult.class))).thenReturn(new ConfigElementForEdit(env1, "3123abcef"))
        when(environmentConfigService.deleteEnvironment(eq(env1), eq(currentUsername()), any(HttpLocalizedOperationResult))).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.getArguments().last()
            result.setMessage("Environment 'my_environment' was deleted successfully.")
        })

        deleteWithApiHeader(controller.controllerPath("env1"))

        assertThatResponse()
          .isOk()
          .hasJsonMessage("Environment 'my_environment' was deleted successfully.")
      }

      @Test
      void 'should error out if the environment does not exist'() {
        when(environmentConfigService.getMergedEnvironmentforDisplay(anyString(), any(HttpLocalizedOperationResult.class)))
          .then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.getArguments().last()
          result.badRequest("No such environment")
        })

        getWithApiHeader(controller.controllerPath("env1"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(notFoundMessage())
      }
    }
  }


  @Nested
  class Update {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'update'
      }

      @Override
      void makeHttpCall() {
        putWithApiHeader(controller.controllerPath("env1"), [])
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
      void 'should error out on update if environment rename is attempted'() {
        def existingConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        existingConfig.addAgent("agent1")
        existingConfig.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        existingConfig.addPipeline(new CaseInsensitiveString("Pipeline1"))

        def newConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env2"))
        newConfig.addAgent("agent1")
        newConfig.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        newConfig.addPipeline(new CaseInsensitiveString("Pipeline1"))

        when(entityHashingService.md5ForEntity(existingConfig)).thenReturn("ffff")
        when(environmentConfigService.getMergedEnvironmentforDisplay(
          eq("env1"),
          any(HttpLocalizedOperationResult))
        ).thenReturn(new ConfigElementForEdit<>(existingConfig, "ffff"))

        def json = toObjectString({ EnvironmentRepresenter.toJSON(it, newConfig) })

        putWithApiHeader(controller.controllerPath("env1"),['if-match': 'ffff'], json)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Renaming of environment is not supported by this API.")
      }

      @Test
      void 'should not update if etag does not match'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addAgent("agent1")
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))


        when(environmentConfigService.getMergedEnvironmentforDisplay(
          eq("env1"),
          any(HttpLocalizedOperationResult))
        ).thenReturn(new ConfigElementForEdit<>(env1, "ffff"))

        when(entityHashingService.md5ForEntity(env1)).thenReturn("wrong-md5")

        def json = toObjectString({ EnvironmentRepresenter.toJSON(it, env1) })

        putWithApiHeader(controller.controllerPath("env1"),['if-match': 'ffff'], json)

        assertThatResponse()
          .isPreconditionFailed()
          .hasJsonMessage("Someone has modified the configuration for environment 'env1'. Please update your copy of the config with the changes and try again.")
      }

      @Test
      void 'should update environment'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addAgent("agent1")
        env1.addAgent("agent2")
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        when(entityHashingService.md5ForEntity(env1)).thenReturn("ffff")

        when(
            environmentConfigService.getMergedEnvironmentforDisplay(eq("env1"),
            any(HttpLocalizedOperationResult))
        ).thenReturn(new ConfigElementForEdit<>(env1, "ffff"))

        when(environmentConfigService.updateEnvironment(eq("env1"), eq(env1), eq(currentUsername()), anyString(), any(HttpLocalizedOperationResult))).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.setMessage("Updated environment '\" + oldEnvironmentConfigName + \"'.")
        })

        def json = toObjectString({ EnvironmentRepresenter.toJSON(it, env1) })

        putWithApiHeader(controller.controllerPath("env1"),['if-match': 'ffff'], json)

        assertThatResponse()
          .isOk()
          .hasEtag('"ffff"')
          .hasBodyWithJsonObject(env1, EnvironmentRepresenter)
      }

      @Test
      void 'should error out if the environment does not exist'() {
        when(environmentConfigService.getMergedEnvironmentforDisplay(eq("env1"), any(HttpLocalizedOperationResult)))
        .then({
          InvocationOnMock invocation ->
            def result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.badRequest("The environment does not exist")
        })
        putWithApiHeader(controller.controllerPath("env1"), [name: "env1"])

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(notFoundMessage())
      }
    }
  }


  @Nested
  class PartialUpdate {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'partialUpdate'
      }

      @Override
      void makeHttpCall() {
        patchWithApiHeader(controller.controllerPath("env1"), [])
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
      void 'should update attributes of environment'() {
        def oldConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        oldConfig.addAgent("agent1")
        oldConfig.addAgent("agent2")
        oldConfig.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        oldConfig.addEnvironmentVariable("URL", "google.com")
        oldConfig.addPipeline(new CaseInsensitiveString("Pipeline1"))
        oldConfig.addPipeline(new CaseInsensitiveString("Pipeline2"))
        oldConfig.addPipeline(new CaseInsensitiveString("Pipeline3"))

        def updatedConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        updatedConfig.addAgent("agent1")
        updatedConfig.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        updatedConfig.addPipeline(new CaseInsensitiveString("Pipeline1"))
        updatedConfig.addPipeline(new CaseInsensitiveString("Pipeline2"))

        when(entityHashingService.md5ForEntity(updatedConfig)).thenReturn("md5-hash")
        when(environmentConfigService.getMergedEnvironmentforDisplay(eq("env1"), any(HttpLocalizedOperationResult)))
          .thenReturn(new ConfigElementForEdit<>(oldConfig, "old_md5_hash"))
        when(environmentConfigService.getMergedEnvironmentforDisplay(eq("env1"), any(HttpLocalizedOperationResult)))
          .thenReturn(new ConfigElementForEdit<>(updatedConfig, "new_md5_hash"))
        when(environmentConfigService.patchEnvironment(
          eq(oldConfig), anyList(), anyList(), anyList(), anyList(),anyList(), anyList(),eq(currentUsername()), any(HttpLocalizedOperationResult))
        ).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.setMessage("Updated environment '\" + oldEnvironmentConfigName + \"'.")
        })


        patchWithApiHeader(controller.controllerPath("env1"), [
          "pipelines": [
            "add": [
              "Pipeline1", "Pipeline2"
            ],
            "remove": [
              "Pipeline3"
            ]
          ],
          "agents": [
            "add": [
              "agent1"
            ],
            "remove": [
              "agent2"
            ]
          ],
          "environment_variables": [
            "add": [
              [
                "name": "JAVA_HOME",
                "value": "/bin/java"
              ]
            ],
            "remove": [
              "URL"
            ]
          ]
        ])

        assertThatResponse()
          .isOk()
          .hasEtag('"md5-hash"')
          .hasBodyWithJsonObject(updatedConfig, EnvironmentRepresenter)
      }

      @Test
      void 'should error out if the environment does not exist'() {
        when(environmentConfigService.getMergedEnvironmentforDisplay(eq("env1"), any(HttpLocalizedOperationResult)))
          .then({
          InvocationOnMock invocation ->
            def result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.badRequest("The environment does not exist")
        })
        patchWithApiHeader(controller.controllerPath("env1"), [name: "env1"])

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(notFoundMessage())
      }
    }
  }


  @Nested
  class Create {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'create'
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), [])
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
      void 'should create environment with specified parameters'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addAgent("agent1")
        env1.addAgent("agent2")
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        def json = toObjectString({ EnvironmentRepresenter.toJSON(it, env1) })

        when(entityHashingService.md5ForEntity(env1)).thenReturn("md5-hash")
        when(environmentConfigService.createEnvironment(eq(env1), eq(currentUsername()), any(HttpLocalizedOperationResult))).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.setMessage("Environment was created successfully")
        })

        postWithApiHeader(controller.controllerPath(), json)

        assertThatResponse()
          .isOk()
          .hasEtag('"md5-hash"')
          .hasJsonBody(json)
      }

      @Test
      void 'should error out if the environment already exists'() {
        when(environmentConfigService.getMergedEnvironmentforDisplay(anyString(), any(HttpLocalizedOperationResult.class)))
          .thenReturn(new ConfigElementForEdit<EnvironmentConfig>(new BasicEnvironmentConfig(), "md5string"))

        postWithApiHeader(controller.controllerPath(), [name: "env1"])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Failed to add environment 'env1'. Another environment with the same name already exists.")
      }
    }
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
        getWithApiHeader(controller.controllerPath())
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
      void 'should return all configured environments'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addAgent("agent1")
        env1.addAgent("agent2")
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        def env2 = new BasicEnvironmentConfig(new CaseInsensitiveString("env2"))

        def listOfEnvironmentConfigs = [env1, env2]

        when(environmentConfigService.getAllMergedEnvironments()).thenReturn(listOfEnvironmentConfigs)

        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse()
          .hasBodyWithJsonObject(listOfEnvironmentConfigs, EnvironmentsRepresenter)
      }


      @Test
      void 'should return empty environments when no environements are configured'() {
        def expectedResponse = [
          "_embedded": [
            "environments": []
          ],
          "_links"   : [
            "doc" : [
              "href": "https://api.go.cd/current/#environment-config"
            ],
            "self": [
              "href": "http://test.host/go/api/admin/environments"
            ]
          ]
        ]

        when(environmentConfigService.getAllMergedEnvironments()).thenReturn([])

        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse().hasJsonBody(expectedResponse)
      }
    }
  }
}
