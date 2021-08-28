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
package com.thoughtworks.go.apiv2.securityauthconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv2.securityauthconfig.representers.SecurityAuthConfigRepresenter
import com.thoughtworks.go.apiv2.securityauthconfig.representers.SecurityAuthConfigsRepresenter
import com.thoughtworks.go.config.SecurityAuthConfig
import com.thoughtworks.go.config.SecurityAuthConfigs
import com.thoughtworks.go.config.exceptions.EntityType
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.SecurityAuthConfigService
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

import static com.thoughtworks.go.api.util.HaltApiMessages.etagDoesNotMatch
import static com.thoughtworks.go.api.util.HaltApiMessages.renameOfEntityIsNotSupportedMessage
import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create
import static com.thoughtworks.go.i18n.LocalizedMessage.resourceDeleteSuccessful
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.doAnswer
import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityAuthConfigControllerV2Test implements SecurityServiceTrait, ControllerTrait<SecurityAuthConfigControllerV2> {

  @Mock
  private SecurityAuthConfigService securityAuthConfigService

  @Mock
  private EntityHashingService entityHashingService


  @Override
  SecurityAuthConfigControllerV2 createControllerInstance() {
    new SecurityAuthConfigControllerV2(securityAuthConfigService, new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService)
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
      void 'should list all security auth configs'() {
        def securityAuthConfigs = new SecurityAuthConfigs(
          new SecurityAuthConfig("file", "cd.go.authorization.file", create("Path", false, "/var/lib/pass.prop")),
          new SecurityAuthConfig("ldap", "cd.go.authorization.ldap", create("Url", false, "ldap://example.com")),
          new SecurityAuthConfig("github", "cd.go.authorization.github", create("PersonalAccessToken", true, "some-token"))
        )

        when(securityAuthConfigService.getPluginProfiles()).thenReturn(securityAuthConfigs)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasEtag('"2ed2348f198e14381f2dd0e5a0e317f8a2287feb8807891a90ca9cd60248d45b"')
          .hasBodyWithJsonObject(SecurityAuthConfigsRepresenter, securityAuthConfigs)
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
        getWithApiHeader(controller.controllerPath("/file"))
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
      void 'should return security auth config of specified id'() {
        def securityAuthConfig = new SecurityAuthConfig("file", "cd.go.authorization.file", create("Path", false, "/var/lib/pass.prop"))

        when(entityHashingService.hashForEntity(securityAuthConfig)).thenReturn('digest')
        when(securityAuthConfigService.findProfile('file')).thenReturn(securityAuthConfig)

        getWithApiHeader(controller.controllerPath('/file'))

        assertThatResponse()
          .isOk()
          .hasEtag('"digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(SecurityAuthConfigRepresenter, securityAuthConfig)
      }

      @Test
      void 'should return 404 if security auth config with id does not exist'() {
        getWithApiHeader(controller.controllerPath('/file'))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("file"))
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 304 if security auth config is not modified'() {
        def securityAuthConfig = new SecurityAuthConfig("file", "cd.go.authorization.file", create("Path", false, "/var/lib/pass.prop"))

        when(entityHashingService.hashForEntity(securityAuthConfig)).thenReturn('digest')
        when(securityAuthConfigService.findProfile('file')).thenReturn(securityAuthConfig)

        getWithApiHeader(controller.controllerPath('/file'), ['if-none-match': '"digest"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 200 with security auth config if etag does not match'() {
        def securityAuthConfig = new SecurityAuthConfig("file", "cd.go.authorization.file", create("Path", false, "/var/lib/pass.prop"))

        when(entityHashingService.hashForEntity(securityAuthConfig)).thenReturn('digest-new')
        when(securityAuthConfigService.findProfile('file')).thenReturn(securityAuthConfig)

        getWithApiHeader(controller.controllerPath('/file'), ['if-none-match': '"digest"'])

        assertThatResponse()
          .isOk()
          .hasEtag('"digest-new"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(SecurityAuthConfigRepresenter, securityAuthConfig)
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
      void 'should create security auth config from given json payload'() {
        def jsonPayload = [
          id        : 'file',
          plugin_id : "cd.go.authorization.file",
          properties: [
            [
              "key"  : "Path",
              "value": "/var/lib/pass.prop"
            ]
          ]]

        when(entityHashingService.hashForEntity(Mockito.any() as SecurityAuthConfig)).thenReturn('some-digest')

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        assertThatResponse()
          .isOk()
          .hasEtag('"some-digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(SecurityAuthConfigRepresenter, new SecurityAuthConfig("file", "cd.go.authorization.file", create("Path", false, "/var/lib/pass.prop")))
      }

      @Test
      void 'should not create security auth config in case of validation error and return the same with errors'() {
        def jsonPayload = [
          id                               : 'file',
          plugin_id                        : "non-existent-plugin-id",
          "allow_only_known_users_to_login": false,
          properties                       : [
            [
              "key": "Path"
            ]
          ]]

        when(securityAuthConfigService.create(Mockito.any() as Username, Mockito.any() as SecurityAuthConfig, Mockito.any() as LocalizedOperationResult))
          .then({ InvocationOnMock invocation ->
          SecurityAuthConfig authConfig = invocation.getArguments()[1]
          authConfig.addError("plugin_id", "Plugin not installed.")
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.unprocessableEntity("validation failed")
        })

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        def expectedResponseBody = [
          message: "validation failed",
          data   : [
            id                               : "file",
            plugin_id                        : "non-existent-plugin-id",
            "allow_only_known_users_to_login": false,
            properties                       : [[key: "Path"]],
            errors                           : [plugin_id: ["Plugin not installed."]]
          ]
        ]

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonBody(new JsonBuilder(expectedResponseBody).toString())
      }

      @Test
      void 'should not create security auth config if one already exist with same id'() {
        def existingElasticProfile = new SecurityAuthConfig("file", 'cd.go.authorization.file')
        def jsonPayload = [
          id                               : 'file',
          plugin_id                        : "cd.go.authorization.file",
          "allow_only_known_users_to_login": false,
          properties                       : []
        ]

        when(entityHashingService.hashForEntity(Mockito.any() as SecurityAuthConfig)).thenReturn('some-digest')
        when(securityAuthConfigService.findProfile("file")).thenReturn(existingElasticProfile)

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        def expectedResponseBody = [
          message: "Failed to add security auth config 'file'. Another security auth config with the same name already exists.",
          data   : [
            id                               : "file",
            plugin_id                        : "cd.go.authorization.file",
            "allow_only_known_users_to_login": false,
            properties                       : [],
            errors                           : [id: ["Security auth config with id 'file' already exists!"]]
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
        putWithApiHeader(controller.controllerPath("/file"), '{}')
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
      void 'should update security auth config if etag matches'() {
        def existingAuthConfig = new SecurityAuthConfig("file", "cd.go.authorization.file", create("Path", false, "/var/lib/pass.prop"))
        def updatedProfile = new SecurityAuthConfig("file", "cd.go.authorization.file", create("Path", false, "/var/config/pass.prop"))
        def jsonPayload = [
          id                               : 'file',
          plugin_id                        : "cd.go.authorization.file",
          "allow_only_known_users_to_login": false,
          properties                       : [
            [
              "key"  : "Path",
              "value": "/var/config/pass.prop"
            ]
          ]]

        when(entityHashingService.hashForEntity(existingAuthConfig)).thenReturn('some-digest')
        when(entityHashingService.hashForEntity(updatedProfile)).thenReturn('new-digest')
        when(securityAuthConfigService.findProfile("file")).thenReturn(existingAuthConfig)

        putWithApiHeader(controller.controllerPath("/file"), ['if-match': 'some-digest'], jsonPayload)

        assertThatResponse()
          .isOk()
          .hasEtag('"new-digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(SecurityAuthConfigRepresenter, updatedProfile)
      }

      @Test
      void 'should update auth config even if id is not specified in payload'() {
        def existingAuthConfig = new SecurityAuthConfig("file", "cd.go.authorization.file", create("Path", false, "/var/lib/pass.prop"))
        def updatedProfile = new SecurityAuthConfig("file", "cd.go.authorization.file", create("Path", false, "/var/config/pass.prop"))
        def jsonPayload = [
          plugin_id : "cd.go.authorization.file",
          properties: [
            [
              "key"  : "Path",
              "value": "/var/config/pass.prop"
            ]
          ]]

        when(entityHashingService.hashForEntity(existingAuthConfig)).thenReturn('some-digest')
        when(entityHashingService.hashForEntity(updatedProfile)).thenReturn('new-digest')
        when(securityAuthConfigService.findProfile("file")).thenReturn(existingAuthConfig)

        putWithApiHeader(controller.controllerPath("/file"), ['if-match': 'some-digest'], jsonPayload)

        assertThatResponse()
          .isOk()
          .hasEtag('"new-digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(SecurityAuthConfigRepresenter, updatedProfile)
      }

      @Test
      void 'should not update security auth config if etag does not match'() {
        def existingAuthConfig = new SecurityAuthConfig("file", "cd.go.authorization.file", create("Path", false, "/var/lib/pass.prop"))
        def jsonPayload = [
          id        : 'file',
          plugin_id : "cd.go.authorization.file",
          properties: [
            [
              "key"  : "Path",
              "value": "/var/config/pass.prop"
            ]
          ]]

        when(entityHashingService.hashForEntity(existingAuthConfig)).thenReturn('some-digest')
        when(securityAuthConfigService.findProfile("file")).thenReturn(existingAuthConfig)

        putWithApiHeader(controller.controllerPath("/file"), ['if-match': 'wrong-digest'], jsonPayload)

        assertThatResponse()
          .isPreconditionFailed()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(etagDoesNotMatch(EntityType.SecurityAuthConfig.getEntityNameLowerCase(), "file"))
      }

      @Test
      void 'should return 404 if the profile does not exists'() {
        when(securityAuthConfigService.findProfile("non-existent-auth-config")).thenReturn(null)
        putWithApiHeader(controller.controllerPath("/non-existent-auth-config"), ['if-match': 'wrong-digest'], [:])

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(controller.entityType.notFoundMessage("non-existent-auth-config"))
      }

      @Test
      void 'should return 422 if attempted rename'() {
        def existingAuthConfig = new SecurityAuthConfig("file", "cd.go.authorization.file")
        def jsonPayload = [
          id        : 'new-file',
          plugin_id : "cd.go.authorization.file",
          properties: []
        ]

        when(entityHashingService.hashForEntity(existingAuthConfig)).thenReturn('some-digest')
        when(securityAuthConfigService.findProfile("file")).thenReturn(existingAuthConfig)

        putWithApiHeader(controller.controllerPath("/file"), ['if-match': 'some-digest'], jsonPayload)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(renameOfEntityIsNotSupportedMessage(controller.getEntityType().getEntityNameLowerCase()))
      }

      @Test
      void 'should return 422 for validation error'() {
        def existingAuthConfig = new SecurityAuthConfig("file", "cd.go.authorization.file", create("Path", false, "/var/lib/pass.prop"))
        def jsonPayload = [
          id                               : 'file',
          plugin_id                        : "cd.go.authorization.file",
          "allow_only_known_users_to_login": false,
          properties                       : []
        ]

        when(entityHashingService.hashForEntity(existingAuthConfig)).thenReturn('some-digest')
        when(securityAuthConfigService.findProfile("file")).thenReturn(existingAuthConfig)

        when(securityAuthConfigService.update(Mockito.any() as Username, Mockito.any() as String, Mockito.any() as SecurityAuthConfig, Mockito.any() as LocalizedOperationResult))
          .then({ InvocationOnMock invocation ->
          SecurityAuthConfig authConfig = invocation.getArguments()[2]
          authConfig.addError("plugin_id", "Plugin not installed.")
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.unprocessableEntity("validation failed")
        })

        putWithApiHeader(controller.controllerPath("/file"), ['if-match': 'some-digest'], jsonPayload)

        def expectedResponseBody = [
          message: "validation failed",
          data   : [
            id                               : "file",
            plugin_id                        : "cd.go.authorization.file",
            "allow_only_known_users_to_login": false,
            properties                       : [],
            errors                           : [plugin_id: ["Plugin not installed."]]
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
  class Destroy {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "deleteAuthConfig"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath("/file"))
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
      void 'should delete security auth config with given id'() {
        def authConfig = new SecurityAuthConfig("file", "cd.go.authorization.file")

        when(securityAuthConfigService.findProfile("file")).thenReturn(authConfig)
        when(securityAuthConfigService.delete(Mockito.any() as Username, Mockito.any() as SecurityAuthConfig, Mockito.any() as HttpLocalizedOperationResult)).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.arguments.last()
          result.setMessage(resourceDeleteSuccessful(EntityType.SecurityAuthConfig.getEntityNameLowerCase(), authConfig.getId()))
        })

        deleteWithApiHeader(controller.controllerPath('/file'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(resourceDeleteSuccessful(EntityType.SecurityAuthConfig.getEntityNameLowerCase(), authConfig.getId()))
      }

      @Test
      void 'should return 404 if security auth config with id does not exist'() {
        when(securityAuthConfigService.findProfile("file")).thenReturn(null)

        deleteWithApiHeader(controller.controllerPath('/file'))

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(controller.entityType.notFoundMessage("file"))
      }

      @Test
      void 'should return validation error on failure'() {
        def authConfig = new SecurityAuthConfig("file", "cd.go.authorization.file")

        when(securityAuthConfigService.findProfile('file')).thenReturn(authConfig)
        doAnswer({ InvocationOnMock invocation ->
          ((HttpLocalizedOperationResult) invocation.arguments.last()).unprocessableEntity("save failed")
        }).when(securityAuthConfigService).delete(any() as Username, eq(authConfig), any() as LocalizedOperationResult)

        deleteWithApiHeader(controller.controllerPath('/file'))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage('save failed')
      }
    }
  }
}
