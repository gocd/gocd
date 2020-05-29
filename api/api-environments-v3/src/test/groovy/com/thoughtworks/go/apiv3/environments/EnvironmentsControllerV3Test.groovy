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
package com.thoughtworks.go.apiv3.environments

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv3.environments.representers.EnvironmentRepresenter
import com.thoughtworks.go.apiv3.environments.representers.EnvironmentsRepresenter
import com.thoughtworks.go.config.*
import com.thoughtworks.go.config.policy.Allow
import com.thoughtworks.go.config.policy.Policy
import com.thoughtworks.go.domain.ConfigElementForEdit
import com.thoughtworks.go.security.GoCipher
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.EnvironmentConfigService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class EnvironmentsControllerV3Test implements SecurityServiceTrait, ControllerTrait<EnvironmentsControllerV3> {

  @Mock
  EnvironmentConfigService environmentConfigService

  @Mock
  EntityHashingService entityHashingService

  @Override
  EnvironmentsControllerV3 createControllerInstance() {
    new EnvironmentsControllerV3(new ApiAuthenticationHelper(securityService, goConfigService), environmentConfigService, entityHashingService)

  }

  @BeforeEach
  void setup() {
    initMocks(this)
  }

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
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
      void 'should return sorted list of environments by name'() {
        def prodEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("prod"))
        def devEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("dev"))
        def qaEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("qa"))

        prodEnv.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        prodEnv.addPipeline(new CaseInsensitiveString("Pipeline1"))
        prodEnv.addPipeline(new CaseInsensitiveString("Pipeline2"))

        def envConfigSet = new HashSet([qaEnv, devEnv, prodEnv])
        when(environmentConfigService.getEnvironments()).thenReturn(envConfigSet)

        getWithApiHeader(controller.controllerBasePath())

        def sortedEnvConfigList = [devEnv, prodEnv, qaEnv]
        assertThatResponse().hasBodyWithJsonObject(sortedEnvConfigList, EnvironmentsRepresenter)
      }

      @Test
      void 'should return no environments when user does not have access to any environment'() {
        loginAsUser()

        Policy directives = new Policy()
        directives.add(new Allow("administer", "environment", "blah_*"))
        RoleConfig role = new RoleConfig(new CaseInsensitiveString("read-only-environments"), new Users(), directives)

        when(goConfigService.rolesForUser(any(CaseInsensitiveString.class))).thenReturn([role])

        def prodEnv1 = new BasicEnvironmentConfig(new CaseInsensitiveString("prod_env1"))
        def devEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("dev_env1"))
        def prodEnv2 = new BasicEnvironmentConfig(new CaseInsensitiveString("prod_env2"))

        prodEnv1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        prodEnv1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        prodEnv1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        def envConfigSet = new HashSet([prodEnv2, devEnv, prodEnv1])
        when(environmentConfigService.getEnvironments()).thenReturn(envConfigSet)

        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse().hasBodyWithJsonObject([], EnvironmentsRepresenter)
      }

      @Test
      void 'should return sorted list of environments which user has access to'() {
        loginAsUser()

        Policy directives = new Policy()
        directives.add(new Allow("administer", "environment", "prod_*"))
        RoleConfig role = new RoleConfig(new CaseInsensitiveString("read-only-environments"), new Users(), directives)

        when(goConfigService.rolesForUser(any(CaseInsensitiveString.class))).thenReturn([role])

        def prodEnv1 = new BasicEnvironmentConfig(new CaseInsensitiveString("prod_env1"))
        def devEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("dev_env1"))
        def prodEnv2 = new BasicEnvironmentConfig(new CaseInsensitiveString("prod_env2"))

        prodEnv1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        prodEnv1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        prodEnv1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        def envConfigSet = new HashSet([prodEnv2, devEnv, prodEnv1])
        when(environmentConfigService.getEnvironments()).thenReturn(envConfigSet)

        getWithApiHeader(controller.controllerBasePath())

        def sortedEnvConfigList = [prodEnv1, prodEnv2]
        assertThatResponse().hasBodyWithJsonObject(sortedEnvConfigList, EnvironmentsRepresenter)
      }

      @Test
      void 'should return empty environments when there are no environments'() {
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

        when(environmentConfigService.getEnvironments()).thenReturn(new HashSet<>([]))

        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse().hasJsonBody(expectedResponse)
      }

      @Test
      void 'should sort set of environment config'() {
        EnvironmentConfig stageEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("stage"))
        EnvironmentConfig prodEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("prod"))
        EnvironmentConfig qa1Env = new BasicEnvironmentConfig(new CaseInsensitiveString("qa1"))
        EnvironmentConfig qa2Env = new BasicEnvironmentConfig(new CaseInsensitiveString("qa2"))

        def envConfigs = [qa1Env, stageEnv, prodEnv, qa2Env]

        def sortedEnvConfigList = controller.sortEnvConfigs(envConfigs as HashSet)
        def expectedSortedList = envConfigs as ArrayList
        expectedSortedList.sort { it.name() }

        assert expectedSortedList == sortedEnvConfigList
      }
    }
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
      void 'should return an environment config for a specified environment name'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        when(entityHashingService.hashForEntity(env1)).thenReturn("digest-hash")
        when(environmentConfigService.getEnvironmentConfig(eq("env1"))).thenReturn(env1)

        getWithApiHeader(controller.controllerPath("env1"))

        assertThatResponse()
          .isOk()
          .hasEtag('"digest-hash"')
          .hasBodyWithJsonObject(env1, EnvironmentRepresenter)
      }

      @Test
      void 'should return 404 when environment with specified name is not found'() {
        getWithApiHeader(controller.controllerPath("env1"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("env1"))
      }

      @Test
      void 'should render not modified when ETag matches'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addAgent("agent1")
        env1.addAgent("agent2")
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        when(entityHashingService.hashForEntity(env1)).thenReturn("digest-hash")
        when(environmentConfigService.getEnvironmentConfig(eq("env1"))).thenReturn(env1)

        getWithApiHeader(controller.controllerPath("env1"), ['if-none-match': 'digest-hash'])

        assertThatResponse()
          .isNotModified()
      }

      @Test
      void 'should return 200 when ETag doesn\'t matche'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addAgent("agent1")
        env1.addAgent("agent2")
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        when(entityHashingService.hashForEntity(env1)).thenReturn("digest-hash")
        when(environmentConfigService.getEnvironmentConfig(eq("env1"))).thenReturn(env1)

        getWithApiHeader(controller.controllerPath("env1"), ['if-none-match': 'digest-old-hash'])

        assertThatResponse()
          .isOk()
          .hasEtag('"digest-hash"')
          .hasBodyWithJsonObject(env1, EnvironmentRepresenter)
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
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        when(environmentConfigService.getEnvironmentConfig(anyString())).thenReturn(env1)
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
          .hasJsonMessage(controller.entityType.notFoundMessage("env1"))
      }

      @Test
      void 'should render not modified when ETag matches'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addAgent("agent1")
        env1.addAgent("agent2")
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        when(entityHashingService.hashForEntity(env1)).thenReturn("digest-hash")
        when(environmentConfigService.getEnvironmentConfig(eq("env1"))).thenReturn(env1)

        getWithApiHeader(controller.controllerPath("env1"), ['if-none-match': 'digest-hash'])

        assertThatResponse()
          .isNotModified()
      }

      @Test
      void 'should return 200 when ETag does not match'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addAgent("agent1")
        env1.addAgent("agent2")
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        when(entityHashingService.hashForEntity(env1)).thenReturn("digest-hash")
        when(environmentConfigService.getEnvironmentConfig(eq("env1"))).thenReturn(env1)

        getWithApiHeader(controller.controllerPath("env1"), ['if-none-match': 'digest-old-hash'])

        assertThatResponse()
          .isOk()
          .hasEtag('"digest-hash"')
          .hasBodyWithJsonObject(env1, EnvironmentRepresenter)
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
        existingConfig.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        existingConfig.addPipeline(new CaseInsensitiveString("Pipeline1"))

        def newConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env2"))
        newConfig.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        newConfig.addPipeline(new CaseInsensitiveString("Pipeline1"))

        when(entityHashingService.hashForEntity(existingConfig)).thenReturn("ffff")
        when(environmentConfigService.getEnvironmentConfig(eq("env1"))).thenReturn(existingConfig)

        def json = toObjectString({ EnvironmentRepresenter.toJSON(it, newConfig) })

        putWithApiHeader(controller.controllerPath("env1"), ['if-match': 'ffff'], json)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Renaming of environment is not supported by this API.")
      }

      @Test
      void 'should not error out on update if the environment name provided has is case different only'() {
        def existingConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        existingConfig.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        existingConfig.addPipeline(new CaseInsensitiveString("Pipeline1"))

        def newConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("Env1"))
        newConfig.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        newConfig.addPipeline(new CaseInsensitiveString("Pipeline1"))

        when(entityHashingService.hashForEntity(existingConfig)).thenReturn("ffff")
        when(environmentConfigService.getEnvironmentConfig(eq("env1"))).thenReturn(existingConfig)

        def json = toObjectString({ EnvironmentRepresenter.toJSON(it, newConfig) })

        putWithApiHeader(controller.controllerPath("env1"), ['if-match': 'ffff'], json)

        assertThatResponse()
          .isOk()
          .hasEtag('"ffff"')
          .hasBodyWithJsonObject(newConfig, EnvironmentRepresenter)
      }

      @Test
      void 'should not update if etag does not match'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))


        when(environmentConfigService.getEnvironmentConfig(eq("env1"))).thenReturn(env1)

        when(entityHashingService.hashForEntity(env1)).thenReturn("wrong-digest")

        def json = toObjectString({ EnvironmentRepresenter.toJSON(it, env1) })

        putWithApiHeader(controller.controllerPath("env1"), ['if-match': 'ffff'], json)

        assertThatResponse()
          .isPreconditionFailed()
          .hasJsonMessage("Someone has modified the configuration for environment 'env1'. Please update your copy of the config with the changes and try again.")
      }

      @Test
      void 'should update environment'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addEnvironmentVariable(new EnvironmentVariableConfig(new GoCipher(), "Secured", new GoCipher().encrypt("confidential")))
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        when(entityHashingService.hashForEntity(env1)).thenReturn("ffff")

        when(environmentConfigService.getEnvironmentConfig(eq("env1"))).thenReturn(env1)

        when(environmentConfigService.updateEnvironment(eq("env1"), eq(env1), eq(currentUsername()), anyString(), any(HttpLocalizedOperationResult))).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.setMessage("Updated environment '\" + oldEnvironmentConfigName + \"'.")
        })

        def json = toObjectString({ EnvironmentRepresenter.toJSON(it, env1) })

        putWithApiHeader(controller.controllerPath("env1"), ['if-match': 'ffff'], json)

        assertThatResponse()
          .isOk()
          .hasEtag('"ffff"')
          .hasBodyWithJsonObject(env1, EnvironmentRepresenter)
      }

      @Test
      void 'should error out if there are errors in parsing environment config'() {
        def json = [
          "name"                 : "env1",
          "pipelines"            : [
            [
              "name": "Pipeline2"
            ]
          ],
          "environment_variables": [
            [
              "secure"         : true,
              "name"           : "JAVA_HOME",
              "value"          : "/bin/java",
              "encrypted_value": new GoCipher().encrypt("some_encrypted_text")
            ]
          ]
        ]

        def expectedResponse = [
          "data"   : [
            "environment_variables": [
              [
                "encrypted_value": new GoCipher().encrypt("/bin/java"),
                "errors"         : [
                  "encrypted_value": ["You may only specify `value` or `encrypted_value`, not both!"],
                  "value"          : ["You may only specify `value` or `encrypted_value`, not both!"]
                ],
                "name"           : "JAVA_HOME",
                "secure"         : true
              ]
            ],
            "name"                 : "env1",
            "pipelines"            : [
              [
                "name": "Pipeline2"
              ]
            ]
          ],
          "message": "Error parsing environment config from the request"
        ]

        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addEnvironmentVariable(new EnvironmentVariableConfig(
          new GoCipher(), "Secured", new GoCipher().encrypt("confidential"))
        )
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        when(entityHashingService.hashForEntity(env1)).thenReturn("ffff")

        when(environmentConfigService.getEnvironmentConfig(eq("env1"))).thenReturn(env1)

        putWithApiHeader(controller.controllerPath("env1"), json)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonBody(expectedResponse)
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
          .hasJsonMessage(controller.entityType.notFoundMessage("env1"))
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
      BasicEnvironmentConfig oldConfig
      BasicEnvironmentConfig updatedConfig

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

        oldConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        oldConfig.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        oldConfig.addEnvironmentVariable("URL", "google.com")
        oldConfig.addPipeline(new CaseInsensitiveString("Pipeline1"))
        oldConfig.addPipeline(new CaseInsensitiveString("Pipeline2"))
        oldConfig.addPipeline(new CaseInsensitiveString("Pipeline3"))

        updatedConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        updatedConfig.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        updatedConfig.addPipeline(new CaseInsensitiveString("Pipeline1"))
        updatedConfig.addPipeline(new CaseInsensitiveString("Pipeline2"))
      }

      @Test
      void 'should update attributes of environment'() {
        when(entityHashingService.hashForEntity(updatedConfig)).thenReturn("digest-hash")
        when(environmentConfigService.getEnvironmentConfig(eq("env1"))).thenReturn(oldConfig)
        when(environmentConfigService.getEnvironmentConfig(eq("env1"))).thenReturn(updatedConfig)
        when(environmentConfigService.patchEnvironment(
          eq(oldConfig), anyList(), anyList(), anyList(), anyList(), eq(currentUsername()), any(HttpLocalizedOperationResult))
        ).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.setMessage("Updated environment '\" + oldEnvironmentConfigName + \"'.")
        })

        def patchRequestJson = [
          "pipelines"            : [
            "add"   : [
              "Pipeline1", "Pipeline2"
            ],
            "remove": [
              "Pipeline3"
            ]
          ],
          "environment_variables": [
            "add"   : [
              [
                "name" : "JAVA_HOME",
                "value": "/bin/java"
              ]
            ],
            "remove": [
              "URL"
            ]
          ]
        ]

        patchWithApiHeader(controller.controllerPath("env1"), patchRequestJson)

        assertThatResponse()
          .isOk()
          .hasEtag('"digest-hash"')
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
          .hasJsonMessage(controller.entityType.notFoundMessage("env1"))
      }

      @Test
      void 'should error out if there are errors in parsing environment variable to add in patch request'() {
        when(entityHashingService.hashForEntity(updatedConfig)).thenReturn("digest-hash")
        when(environmentConfigService.getEnvironmentConfig(eq("env1"))).thenReturn(oldConfig)
        when(environmentConfigService.getEnvironmentConfig(eq("env1"))).thenReturn(updatedConfig)
        when(environmentConfigService.patchEnvironment(
          eq(oldConfig), anyList(), anyList(), anyList(), anyList(), eq(currentUsername()), any(HttpLocalizedOperationResult))
        ).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.setMessage("Updated environment '\" + oldEnvironmentConfigName + \"'.")
        })

        def patchRequestJson = [
          "pipelines"            : [
            "add"   : [
              "Pipeline1", "Pipeline2"
            ],
            "remove": [
              "Pipeline3"
            ]
          ],
          "environment_variables": [
            "add"   : [[
                         "secure"         : true,
                         "name"           : "JAVA_HOME",
                         "value"          : "/bin/java",
                         "encrypted_value": new GoCipher().encrypt("some_encrypted_text")
                       ]],
            "remove": [
              "URL"
            ]
          ]
        ]

        def expectedJson = [
          "data"   : [
            "environment_variables": [
              [
                "encrypted_value": new GoCipher().encrypt("/bin/java"),
                "errors"         : [
                  "encrypted_value": [
                    "You may only specify `value` or `encrypted_value`, not both!"
                  ],
                  "value"          : [
                    "You may only specify `value` or `encrypted_value`, not both!"
                  ]
                ],
                "name"           : "JAVA_HOME",
                "secure"         : true
              ]
            ]
          ],
          "message": "Error parsing patch request"
        ]

        patchWithApiHeader(controller.controllerPath("env1"), patchRequestJson)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonBody(expectedJson)
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
        postWithApiHeader(controller.controllerPath(), [name: "env1"])
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
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addEnvironmentVariable(new EnvironmentVariableConfig(new GoCipher(), "Secured", new GoCipher().encrypt("confidential")))
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        def json = toObjectString({ EnvironmentRepresenter.toJSON(it, env1) })

        when(entityHashingService.hashForEntity(env1)).thenReturn("digest-hash")
        when(environmentConfigService.createEnvironment(eq(env1), eq(currentUsername()), any(HttpLocalizedOperationResult))).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.setMessage("Environment was created successfully")
        })

        postWithApiHeader(controller.controllerPath(), json)

        assertThatResponse()
          .isOk()
          .hasEtag('"digest-hash"')
          .hasJsonBody(json)
      }

      @Test
      void 'should error out if there are errors in parsing environment config'() {
        def json = [
          "name"                 : "env1",
          "pipelines"            : [
            [
              "name": "Pipeline2"
            ]
          ],
          "environment_variables": [
            [
              "secure"         : true,
              "name"           : "JAVA_HOME",
              "value"          : "/bin/java",
              "encrypted_value": new GoCipher().encrypt("some_encrypted_text")
            ]
          ]
        ]

        def expectedResponse = [
          "data"   : [
            "environment_variables": [
              [
                "encrypted_value": new GoCipher().encrypt("/bin/java"),
                "errors"         : [
                  "encrypted_value": ["You may only specify `value` or `encrypted_value`, not both!"],
                  "value"          : ["You may only specify `value` or `encrypted_value`, not both!"]
                ],
                "name"           : "JAVA_HOME",
                "secure"         : true
              ]
            ],
            "name"                 : "env1",
            "pipelines"            : [
              [
                "name": "Pipeline2"
              ]
            ]
          ],
          "message": "Error parsing environment config from the request"
        ]

        postWithApiHeader(controller.controllerPath(), json)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonBody(expectedResponse)
      }

      @Test
      void 'should error out when name parameter is missing'() {
        postWithApiHeader(controller.controllerPath(), [somethingRandom: "sjdiajdisajdi"])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Json `{\\\"somethingRandom\\\":\\\"sjdiajdisajdi\\\"}` does not contain property 'name'")
      }

      @Test
      void 'should error out if the environment already exists'() {
        when(environmentConfigService.getMergedEnvironmentforDisplay(anyString(), any(HttpLocalizedOperationResult.class)))
          .thenReturn(new ConfigElementForEdit<EnvironmentConfig>(new BasicEnvironmentConfig(), "digeststring"))

        postWithApiHeader(controller.controllerPath(), [name: "env1"])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Failed to add environment 'env1'. Another environment with the same name already exists.")
      }
    }
  }
}
