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
package com.thoughtworks.go.apiv3.secretconfigs

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv3.secretconfigs.representers.SecretConfigRepresenter
import com.thoughtworks.go.apiv3.secretconfigs.representers.SecretConfigsRepresenter
import com.thoughtworks.go.config.SecretConfig
import com.thoughtworks.go.config.SecretConfigs
import com.thoughtworks.go.config.exceptions.EntityType
import com.thoughtworks.go.config.rules.Allow
import com.thoughtworks.go.config.rules.Deny
import com.thoughtworks.go.config.rules.Rules
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.i18n.LocalizedMessage
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.SecretConfigService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.server.service.result.LocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import groovy.json.JsonBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.apiv3.secretconfigs.representers.SecretConfigRepresenter.fromJSON
import static com.thoughtworks.go.apiv3.secretconfigs.representers.SecretConfigRepresenter.toJSON
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

@MockitoSettings(strictness = Strictness.LENIENT)
class SecretConfigsControllerV3Test implements SecurityServiceTrait, ControllerTrait<SecretConfigsControllerV3> {

  @Mock
  SecretConfigService secretConfigService

  @Mock
  EntityHashingService entityHashingService


  @Override
  SecretConfigsControllerV3 createControllerInstance() {
    new SecretConfigsControllerV3(new ApiAuthenticationHelper(securityService, goConfigService), secretConfigService, entityHashingService)
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
      void 'should list all secrets configs with etag header'() {
        def expectedConfigs = new SecretConfigs(new SecretConfig("ForDeploy", "file",
          ConfigurationPropertyMother.create("username", false, "Jane"),
          ConfigurationPropertyMother.create("password", true, "Doe")
        ))

        when(secretConfigService.getAllSecretConfigs()).thenReturn(expectedConfigs)
        when(entityHashingService.hashForEntity(expectedConfigs)).thenReturn("ffff")

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasEtag('"ffff"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(SecretConfigsRepresenter, expectedConfigs)
      }

      @Test
      void 'should return 304 if secret configs are not modified since last request'() {
        def expectedConfigs = new SecretConfigs(new SecretConfig("ForDeploy", "file",
          ConfigurationPropertyMother.create("username", false, "Jane"),
          ConfigurationPropertyMother.create("password", true, "Doe")
        ))

        when(secretConfigService.getAllSecretConfigs()).thenReturn(expectedConfigs)
        when(entityHashingService.hashForEntity(expectedConfigs)).thenReturn("ffff")

        getWithApiHeader(controller.controllerPath(), ['if-none-match': '"ffff"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
          .hasNoBody()
      }

      @Test
      void 'should return empty list if there are no secrets configs'() {
        def expectedConfigs = new SecretConfigs()
        when(secretConfigService.getAllSecretConfigs()).thenReturn(expectedConfigs)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(SecretConfigsRepresenter, expectedConfigs)
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
        getWithApiHeader(controller.controllerPath("/foo_secret_config"))
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
      void 'should return secret config if it exists'() {
        def expectedConfig = new SecretConfig("ForDeploy", "file",
          ConfigurationPropertyMother.create("username", false, "Jane"),
          ConfigurationPropertyMother.create("password", true, "Doe")
        )

        when(entityHashingService.hashForEntity(expectedConfig)).thenReturn('digest')
        when(secretConfigService.getAllSecretConfigs()).thenReturn(new SecretConfigs(expectedConfig))

        getWithApiHeader(controller.controllerPath("/ForDeploy"))

        assertThatResponse()
          .isOk()
          .hasEtag('"digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(SecretConfigRepresenter, expectedConfig)
      }

      @Test
      void 'should return 404 if secrets config does not exist'() {
        when(secretConfigService.getAllSecretConfigs()).thenReturn(new SecretConfigs())

        getWithApiHeader(controller.controllerPath("/non-existing-secret-config"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(EntityType.SecretConfig.notFoundMessage("non-existing-secret-config"))
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 304 if secret is not modified'() {
        def expectedConfig = new SecretConfig("ForDeploy", "file",
          ConfigurationPropertyMother.create("username", false, "Jane"),
          ConfigurationPropertyMother.create("password", true, "Doe")
        )

        when(entityHashingService.hashForEntity(expectedConfig)).thenReturn('digest')
        when(secretConfigService.getAllSecretConfigs()).thenReturn(new SecretConfigs(expectedConfig))

        getWithApiHeader(controller.controllerPath('/ForDeploy'), ['if-none-match': '"digest"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 200 with secret config if etag does not match'() {
        def expectedConfig = new SecretConfig("ForDeploy", "file",
          ConfigurationPropertyMother.create("username", false, "Jane"),
          ConfigurationPropertyMother.create("password", true, "Doe")
        )

        when(entityHashingService.hashForEntity(expectedConfig)).thenReturn('digest-new')
        when(secretConfigService.getAllSecretConfigs()).thenReturn(new SecretConfigs(expectedConfig))

        getWithApiHeader(controller.controllerPath('/ForDeploy'), ['if-none-match': '"digest"'])

        assertThatResponse()
          .isOk()
          .hasEtag('"digest-new"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(SecretConfigRepresenter, expectedConfig)
      }
    }
  }

  @Nested
  class Create {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "create"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), '{}')
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
      void 'should create secret config from given json payload'() {
        def jsonPayload = [
          "id"         : "secrets_id",
          "description": "This is used to lookup for secrets for the team X.",
          "plugin_id"  : "cd.go.secrets.file",
          "properties" : [
            [
              "key"  : "secrets_file_path",
              "value": "/home/secret/secret.dat"
            ],
            [
              "key"  : "cipher_file_path",
              "value": "/home/secret/secret-key.aes"
            ],
            [
              "key"            : "secret_password",
              "encrypted_value": "0a3ecba2e196f73d07b361398cc9d08b"
            ]
          ],
          "rules"      : [
            [
              "directive": "allow",
              "action"   : "refer",
              "type"     : "PipelineGroup",
              "resource" : "DeployPipelines"
            ],
            [
              "directive": "deny",
              "action"   : "refer",
              "type"     : "PipelineGroup",
              "resource" : "TestPipelines"
            ]
          ]
        ]

        def secretConfig = fromJSON(GsonTransformer.instance.jsonReaderFrom(jsonPayload))

        when(entityHashingService.hashForEntity(Mockito.any() as SecretConfig)).thenReturn('some-digest')
        when(secretConfigService.getAllSecretConfigs()).thenReturn(new SecretConfigs())

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        assertThatResponse()
          .isOk()
          .hasEtag('"some-digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(SecretConfigRepresenter, secretConfig)
      }


      @Test
      void 'should not create secret config if one already exist with same id'() {
        def expectedConfig = new SecretConfig("ForDeploy", "file",
          ConfigurationPropertyMother.create("username", false, "Jane"),
        )

        when(entityHashingService.hashForEntity(expectedConfig)).thenReturn('digest')
        when(secretConfigService.getAllSecretConfigs()).thenReturn(new SecretConfigs(expectedConfig))

        def jsonPayload = toObjectString({ toJSON(it, expectedConfig) })
        postWithApiHeader(controller.controllerPath(), jsonPayload)


        def expectedResponseBody = [
          message: "Failed to add secretConfig 'ForDeploy'. Another secretConfig with the same name already exists.",
          data   : [
            id        : "ForDeploy",
            plugin_id : "file",
            properties: [[key: "username", value: "Jane"]],
            errors    : [id: ["Secret Configuration ids should be unique. Secret Configuration with id 'ForDeploy' already exists."]]
          ]
        ]

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonBody(new JsonBuilder(expectedResponseBody).toString())
      }

      @Test
      void 'should return error response if there are errors in validation'() {
        def expectedConfig = new SecretConfig("ForDeploy", "file",
          ConfigurationPropertyMother.create("username", false, "Jane"),
        )

        when(entityHashingService.hashForEntity(expectedConfig)).thenReturn('digest')
        when(secretConfigService.getAllSecretConfigs()).thenReturn(new SecretConfigs())

        def jsonPayload = toObjectString({ toJSON(it, expectedConfig) })

        when(secretConfigService.create(Mockito.any() as Username, Mockito.any() as SecretConfig, Mockito.any() as LocalizedOperationResult))
          .then({ InvocationOnMock invocation ->
          SecretConfig secretConfig = invocation.getArguments()[1]
          secretConfig.addError("plugin_id", "Plugin not installed.")
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.unprocessableEntity("validation failed")
        })

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        def expectedResponseBody = [
          message: "validation failed",
          data   : [
            id        : "ForDeploy",
            plugin_id : "file",
            properties: [[key: "username", value: "Jane"]],
            errors    : [plugin_id: ["Plugin not installed."]]
          ]
        ]

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonBody(new JsonBuilder(expectedResponseBody).toString())
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
        putWithApiHeader(controller.controllerPath("/foo_secret_config"), '{}')
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
      void 'should update secret config'() {
        SecretConfig secretConfig = new SecretConfig("secrets_id", "plugin-id")
        secretConfig.setDescription("This is used to lookup for secrets for the team Y.")
        secretConfig.getConfiguration().add(ConfigurationPropertyMother.create("key", false, "secrets_file_path"))
        secretConfig.getConfiguration().add(ConfigurationPropertyMother.create("cipher_file_path", false, "/home/secret/secret-key.aes"))

        secretConfig.setRules(new Rules([
          new Allow("refer", "PipelineGroup", "DeployPipelines"),
          new Allow("view", "Environment", "DeployEnvironment"),
          new Deny("refer", "PipelineGroup", "TestPipelines"),
          new Deny("view", "Environment", "TestEnvironment")
        ]))

        def requestJson = [
          "id"         : "secrets_id",
          "description": "This is used to lookup for secrets for the team X.",
          "plugin_id"  : "cd.go.secrets.file",
          "properties" : [
            [
              "key"  : "secrets_file_path",
              "value": "/home/secret/secret.dat"
            ],
            [
              "key"  : "cipher_file_path",
              "value": "/home/secret/secret-key.aes"
            ],
            [
              "key"            : "secret_password",
              "encrypted_value": "0a3ecba2e196f73d07b361398cc9d08b"
            ]
          ],
          "rules"      :[
            [
              "directive": "allow",
              "action"   : "refer",
              "type"     : "PipelineGroup",
              "resource" : "DeployPipelines"
            ],
            [
              "directive": "deny",
              "action"   : "refer",
              "type"     : "PipelineGroup",
              "resource" : "TestPipelines"
            ]
          ]
        ]

        def newSecretConfig = fromJSON(GsonTransformer.instance.jsonReaderFrom(requestJson))

        when(entityHashingService.hashForEntity(secretConfig)).thenReturn('old-digest')
        when(entityHashingService.hashForEntity(newSecretConfig)).thenReturn('new-digest')
        when(secretConfigService.getAllSecretConfigs()).thenReturn(new SecretConfigs(secretConfig)).thenReturn(new SecretConfigs(newSecretConfig))
        when(secretConfigService.update(eq(currentUsername()), eq('some-digest'), eq(newSecretConfig), any(LocalizedOperationResult))).thenAnswer({
          HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) it.getArguments().last()
          result.setMessage("SecretConfig 'secrets_id' was updated successfully.")
        })

        putWithApiHeader(controller.controllerPath("/secrets_id"), ['if-match': 'old-digest'], requestJson)

        assertThatResponse()
          .isOk()
          .hasEtag('"new-digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(SecretConfigRepresenter, newSecretConfig)
      }

      @Test
      void shouldNotUpdateIfEtagsDontMatch() {
        SecretConfig secretConfig = new SecretConfig("secrets_id", "plugin-id")
        secretConfig.setDescription("This is used to lookup for secrets for the team Y.")

        def requestJson = [
          "id"         : "secrets_id",
          "description": "This is used to lookup for secrets for the team X.",
          "plugin_id"  : "cd.go.secrets.file"
        ]

        def newSecretConfig = fromJSON(GsonTransformer.instance.jsonReaderFrom(requestJson))

        when(entityHashingService.hashForEntity(secretConfig)).thenReturn('changed-digest')
        when(entityHashingService.hashForEntity(newSecretConfig)).thenReturn('new-digest')
        when(secretConfigService.getAllSecretConfigs()).thenReturn(new SecretConfigs(secretConfig)).thenReturn(new SecretConfigs(newSecretConfig))
        verify(secretConfigService, times(0)).update(eq(currentUsername()), eq('new-digest'), eq(newSecretConfig), any(LocalizedOperationResult))

        putWithApiHeader(controller.controllerPath("/secrets_id"), ['if-match': 'old-digest'], requestJson)

        assertThatResponse()
          .isPreconditionFailed()
          .hasJsonMessage("Someone has modified the entity. Please update your copy with the changes and try again.")
      }

      @Test
      void shouldNotUpdateIfSecretConfigRenameIsAttempted() {
        SecretConfig secretConfig = new SecretConfig("secrets_id", "plugin-id")
        secretConfig.setDescription("This is used to lookup for secrets for the team X.")

        def requestJson = [
          "id"         : "renamed_secret_id",
          "description": "This is used to lookup for secrets for the team X.",
          "plugin_id"  : "cd.go.secrets.file"
        ]

        def newSecretConfig = fromJSON(GsonTransformer.instance.jsonReaderFrom(requestJson))

        when(entityHashingService.hashForEntity(secretConfig)).thenReturn('old-digest')
        when(entityHashingService.hashForEntity(newSecretConfig)).thenReturn('new-digest')
        when(secretConfigService.getAllSecretConfigs()).thenReturn(new SecretConfigs(secretConfig)).thenReturn(new SecretConfigs(newSecretConfig))
        verify(secretConfigService, times(0)).update(eq(currentUsername()), eq('new-digest'), eq(newSecretConfig), any(LocalizedOperationResult))

        putWithApiHeader(controller.controllerPath("/secrets_id"), ['if-match': 'old-digest'], requestJson)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Renaming of secret config is not supported by this API.")
      }

      @Test
      void shouldReturnNotFoundIfSecretConfigDoesNotExist() {
        def requestJson = [
          "id"         : "secrets_id",
          "description": "This is used to lookup for secrets for the team X.",
          "plugin_id"  : "cd.go.secrets.file"
        ]

        def newSecretConfig = fromJSON(GsonTransformer.instance.jsonReaderFrom(requestJson))

        when(secretConfigService.getAllSecretConfigs()).thenReturn(new SecretConfigs()).thenReturn(new SecretConfigs(newSecretConfig))
        verify(secretConfigService, times(0)).update(eq(currentUsername()), eq('new-digest'), eq(newSecretConfig), any(LocalizedOperationResult))

        putWithApiHeader(controller.controllerPath("/secrets_id"), ['if-match': 'old-digest'], requestJson)

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Secret config with id 'secrets_id' was not found!")
      }

      @Test
      void shouldReturn422WhenMalformedJSONRequestIsReceived() {
        when(secretConfigService.getAllSecretConfigs()).thenReturn(
            new SecretConfigs(new SecretConfig("foo", "bar-plugin"))
        )

        putWithApiHeader(controller.controllerPath("/foo"), ['if-match': 'digest-does-not-matter'], [:])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Json `{}` does not contain property 'id'")
      }
    }
  }

  @Nested
  class Destroy {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "destroy"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath("/foo_secret_config"))
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
      void 'should delete secret config with given id'() {
        def secretConfig = new SecretConfig("ForDeploy", "file-plugin")

        when(secretConfigService.getAllSecretConfigs()).thenReturn(new SecretConfigs(secretConfig))
        when(secretConfigService.delete(Mockito.any() as Username, Mockito.any() as SecretConfig, Mockito.any() as HttpLocalizedOperationResult)).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.arguments.last()
          result.setMessage(LocalizedMessage.resourceDeleteSuccessful('secret config', secretConfig.getId()))
        })

        deleteWithApiHeader(controller.controllerPath('/ForDeploy'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(LocalizedMessage.resourceDeleteSuccessful('secret config', secretConfig.getId()))
      }

      @Test
      void 'should return 404 if secret config with id does not exist'() {
        when(secretConfigService.getAllSecretConfigs()).thenReturn(new SecretConfigs())

        deleteWithApiHeader(controller.controllerPath('/ForDeploy'))

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(controller.entityType.notFoundMessage("ForDeploy"))
      }

      @Test
      void 'should return validation error on failure'() {
        def secretConfig = new SecretConfig("ForDeploy", "cd.go.ForDeploy")

        when(secretConfigService.getAllSecretConfigs()).thenReturn(new SecretConfigs(secretConfig))
        doAnswer({ InvocationOnMock invocation ->
          ((HttpLocalizedOperationResult) invocation.arguments.last()).unprocessableEntity("save failed")
        }).when(secretConfigService).delete(any() as Username, eq(secretConfig), any() as LocalizedOperationResult)

        deleteWithApiHeader(controller.controllerPath('/ForDeploy'))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage('save failed')
      }
    }
  }

}
