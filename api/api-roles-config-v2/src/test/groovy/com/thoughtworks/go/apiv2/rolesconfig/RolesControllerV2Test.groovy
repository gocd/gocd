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
package com.thoughtworks.go.apiv2.rolesconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.api.util.HaltApiMessages
import com.thoughtworks.go.apiv2.rolesconfig.representers.RoleRepresenter
import com.thoughtworks.go.apiv2.rolesconfig.representers.RolesRepresenter
import com.thoughtworks.go.config.*
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.RoleConfigService
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
import static com.thoughtworks.go.api.base.JsonUtils.toObjectWithoutLinks
import static com.thoughtworks.go.api.util.HaltApiMessages.entityAlreadyExistsMessage
import static com.thoughtworks.go.api.util.HaltApiMessages.etagDoesNotMatch
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class RolesControllerV2Test implements SecurityServiceTrait, ControllerTrait<RolesControllerV2> {

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Mock
  private RoleConfigService roleConfigService

  @Mock
  private EntityHashingService entityHashingService

  @Override
  RolesControllerV2 createControllerInstance() {
    new RolesControllerV2(roleConfigService, new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService)
  }

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
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
        def expectedRoles = new RolesConfig([new PluginRoleConfig('foo', 'ldap')])
        when(entityHashingService.md5ForEntity(expectedRoles)).thenReturn("some-etag")
        when(roleConfigService.getRoles()).thenReturn(expectedRoles)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasEtag('"some-etag"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(expectedRoles, RolesRepresenter)
      }

      @Test
      void 'should list roles by type'() {
        def pluginRoleConfig = new PluginRoleConfig('foo', 'ldap')
        def gocdRoleConfig = new RoleConfig('bar')
        def expectedRoles = new RolesConfig([pluginRoleConfig, gocdRoleConfig])
        when(entityHashingService.md5ForEntity(expectedRoles)).thenReturn("some-etag")
        when(roleConfigService.getRoles()).thenReturn(expectedRoles)

        getWithApiHeader(controller.controllerPath(type: 'plugin'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(new RolesConfig([pluginRoleConfig]), RolesRepresenter)
      }

      @Test
      void 'should error out if listing roles by a wrong type'() {
        when(roleConfigService.getRoles()).thenReturn(new RolesConfig())

        getWithApiHeader(controller.controllerPath(type: 'bad-role-type'))

        assertThatResponse()
          .isBadRequest()
          .hasEtag(null)
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Bad role type `bad-role-type`. Valid values are gocd, plugin")
      }

      @Test
      void 'should render 304 if etag matches'() {
        def expectedRoles = new RolesConfig([new PluginRoleConfig('foo', 'ldap')])

        when(entityHashingService.md5ForEntity(expectedRoles)).thenReturn("some-etag")
        when(roleConfigService.getRoles()).thenReturn(expectedRoles)

        getWithApiHeader(controller.controllerPath(), ['if-none-match': '"some-etag"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }
    }
  }

  @Nested
  class Show {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @BeforeEach
      void setUp() {
        when(roleConfigService.findRole("foo")).thenReturn(new RoleConfig("foo"))
      }

      @Override
      String getControllerMethodUnderTest() {
        return "show"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath('/foo'))
      }
    }

    @Nested
    class AsAdmin {
      HttpLocalizedOperationResult result

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
        this.result = new HttpLocalizedOperationResult()
      }

      @Test
      void 'should render the security auth config of specified name'() {
        def role = new PluginRoleConfig('blackbird', 'ldap')
        when(entityHashingService.md5ForEntity(role)).thenReturn('md5')
        when(roleConfigService.findRole('blackbird')).thenReturn(role)

        getWithApiHeader(controller.controllerPath('/blackbird'))

        assertThatResponse()
          .isOk()
          .hasEtag('"md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(role, RoleRepresenter)
      }

      @Test
      void 'should return 404 if the security auth config does not exist'() {
        when(roleConfigService.findRole('non-existent-security-auth-config')).thenReturn(null)

        getWithApiHeader(controller.controllerPath('/non-existent-security-auth-config'))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage('non-existent-security-auth-config'))
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should render 304 if etag matches'() {
        def role = new PluginRoleConfig('blackbird', 'ldap')
        when(entityHashingService.md5ForEntity(role)).thenReturn('md5')
        when(roleConfigService.findRole('blackbird')).thenReturn(role)
        getWithApiHeader(controller.controllerPath('/blackbird'), ['if-none-match': '"md5"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should render 200 if etag does not match'() {
        def role = new PluginRoleConfig('blackbird', 'ldap')
        when(entityHashingService.md5ForEntity(role)).thenReturn('md5')
        when(roleConfigService.findRole('blackbird')).thenReturn(role)
        getWithApiHeader(controller.controllerPath('/blackbird'), ['if-none-match': '"junk"'])

        assertThatResponse()
          .isOk()
          .hasEtag('"md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(role, RoleRepresenter)
      }
    }
  }

  @Nested
  class Create {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() { return "create" }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), "{}")
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
      void 'should deserialize auth config from given parameters'() {
        PluginRoleConfig role = new PluginRoleConfig('blackbird', 'blackbird')
        when(entityHashingService.md5ForEntity(role)).thenReturn('some-md5')
        when(roleConfigService.findRole('blackbird')).thenReturn(null)
        doNothing().when(roleConfigService).create(any(), any(), any())


        postWithApiHeader(controller.controllerPath(), toObjectString({ RoleRepresenter.toJSON(it, role) }))

        assertThatResponse()
          .isOk()
          .hasEtag('"some-md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(role, RoleRepresenter)
      }

      @Test
      void 'should fail to save if there are validation errors'() {
        PluginRoleConfig role = new PluginRoleConfig('blackbird', 'blackbird')

        when(roleConfigService.create(any(), any(), any())).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.unprocessableEntity("validation failed")
        })

        postWithApiHeader(controller.controllerPath(), toObjectString({ RoleRepresenter.toJSON(it, role) }))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("validation failed")
      }

      @Test
      void 'should check for existence of role with same name'() {
        PluginRoleConfig role = new PluginRoleConfig('blackbird', 'skunkworks')

        RoleConfig expectedRole = new RoleConfig('blackbird', new RoleUser('bob'), new RoleUser('alice'))
        expectedRole.addError('name', 'Role names should be unique. Role with the same name exists.')

        when(roleConfigService.findRole('blackbird')).thenReturn(role)
        postWithApiHeader(controller.controllerPath(), toObjectString({ RoleRepresenter.toJSON(it, expectedRole) }))

        verify(roleConfigService, never()).create(any(), any(), any())

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(entityAlreadyExistsMessage("role", "blackbird"))
          .hasJsonAttribute('data', toObjectWithoutLinks({ RoleRepresenter.toJSON(it, expectedRole) }))
      }
    }
  }

  @Nested
  class Update {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      PluginRoleConfig roleConfig = new PluginRoleConfig('blackbird', 'skunkworks')

      @BeforeEach
      void setUp() {
        when(roleConfigService.findRole(roleConfig.name.toString())).thenReturn(roleConfig)
        when(entityHashingService.md5ForEntity(roleConfig)).thenReturn('cached-md5')
      }

      @Override
      String getControllerMethodUnderTest() {
        return "update"
      }

      @Override
      void makeHttpCall() {
        sendRequest('put', controller.controllerPath('/blackbird'), [
          'accept'      : controller.mimeType,
          'If-Match'    : 'cached-md5',
          'content-type': 'application/json'
        ], toObjectString({ RoleRepresenter.toJSON(it, this.roleConfig) }))
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
      void 'should not allow rename of auth config id'() {
        Role role = new PluginRoleConfig('foo', 'ldap')

        when(roleConfigService.findRole('foo')).thenReturn(role)
        when(entityHashingService.md5ForEntity(role)).thenReturn("cached-md5")

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'cached-md5',
          'content-type': 'application/json'
        ]
        def body = [
          name      : 'blackbird',
          type      : 'plugin',
          attributes: [:]
        ]

        putWithApiHeader(controller.controllerPath('/foo'), headers, body)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(HaltApiMessages.renameOfEntityIsNotSupportedMessage("roles"))
      }

      @Test
      void 'should fail update if etag does not match'() {
        Role role = new PluginRoleConfig('blackbird', 'ldap')

        when(roleConfigService.findRole('blackbird')).thenReturn(role)
        when(entityHashingService.md5ForEntity(role)).thenReturn('cached-md5')

        putWithApiHeader(controller.controllerPath('/blackbird'), ['if-match': 'some-string'], toObjectString({
          RoleRepresenter.toJSON(it, role)
        }))

        assertThatResponse().isPreconditionFailed()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(etagDoesNotMatch("role", "blackbird"))
      }

      @Test
      void 'should proceed with update if etag matches'() {
        Role role = new PluginRoleConfig('blackbird', 'ldap')
        Role newRole = new PluginRoleConfig('blackbird', 'blackbird')

        when(roleConfigService.findRole('blackbird')).thenReturn(role)
        when(entityHashingService.md5ForEntity(role)).thenReturn('cached-md5')
        when(entityHashingService.md5ForEntity(newRole)).thenReturn('new-md5')

        putWithApiHeader(controller.controllerPath('/blackbird'), ['if-match': 'cached-md5'], toObjectString({
          RoleRepresenter.toJSON(it, newRole)
        }))

        assertThatResponse()
          .isOk()
          .hasEtag('"new-md5"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(newRole, RoleRepresenter)
      }
    }
  }

  @Nested
  class BulkUpdate {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      RoleConfig roleConfig = new RoleConfig('role', new RoleUser('user'))

      @BeforeEach
      void setUp() {
        when(roleConfigService.findRole(roleConfig.name.toString())).thenReturn(roleConfig)
      }

      @Override
      String getControllerMethodUnderTest() {
        return "bulkUpdate"
      }

      @Override
      void makeHttpCall() {
        patchWithApiHeader(controller.controllerPath(), [:])
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
      void 'should bulk update GoCD Roles'() {
        Role role = new RoleConfig('foo', new RoleUser("user"))

        when(roleConfigService.getRoles()).thenReturn(new RolesConfig(role))

        def headers = [
          'accept'      : controller.mimeType,
          'content-type': 'application/json'
        ]
        def body = [
          "operations": [
            [
              "role" : "foo",
              "users": [
                "add"   : [
                  "user1",
                  "user2"
                ],
                "remove": [
                  "user"
                ]
              ]
            ]
          ]
        ]

        patchWithApiHeader(controller.controllerPath(), headers, body)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(new RolesConfig(role), RolesRepresenter.class)
      }
    }
  }

  @Nested
  class Destroy {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      PluginRoleConfig roleConfig = new PluginRoleConfig('blackbird', 'skunkworks')

      @BeforeEach
      void setUp() {
        when(roleConfigService.findRole('blackbird')).thenReturn(roleConfig)
      }

      @Override
      String getControllerMethodUnderTest() {
        return "destroy"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath('/blackbird'))
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
      void 'should raise an error if role is not found'() {
        when(roleConfigService.findRole('blackbird')).thenReturn(null)
        deleteWithApiHeader(controller.controllerPath('/blackbird'))
        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage('blackbird'))
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should render the success message on deleting a role'() {
        PluginRoleConfig role = new PluginRoleConfig('blackbird', 'ldap')
        when(roleConfigService.findRole('blackbird')).thenReturn(role)

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.arguments.last()
          result.setMessage("something bad happened")
        }).when(roleConfigService).delete(any(), eq(role), any())

        deleteWithApiHeader(controller.controllerPath('/blackbird'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage('something bad happened')
      }

      @Test
      void 'should render the validation errors on failure to delete'() {
        PluginRoleConfig role = new PluginRoleConfig('blackbird', 'ldap')

        when(roleConfigService.findRole('blackbird')).thenReturn(role)

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.arguments.last()
          result.unprocessableEntity("some error happened!")
        }).when(roleConfigService).delete(any(), eq(role), any())

        deleteWithApiHeader(controller.controllerPath('/blackbird'))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("some error happened!")
      }
    }
  }
}
