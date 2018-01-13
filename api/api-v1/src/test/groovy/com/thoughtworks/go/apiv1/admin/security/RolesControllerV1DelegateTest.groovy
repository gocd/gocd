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

package com.thoughtworks.go.apiv1.admin.security

import com.thoughtworks.go.api.ClearSingletonExtension
import com.thoughtworks.go.api.ControllerTrait
import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.config.*
import com.thoughtworks.go.i18n.LocalizedMessage
import com.thoughtworks.go.i18n.Localizer
import com.thoughtworks.go.server.api.HaltMessages
import com.thoughtworks.go.server.api.SecurityServiceTrait
import com.thoughtworks.go.server.api.spring.AuthenticationHelper
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.RoleConfigService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.util.SystemEnvironment
import gen.com.thoughtworks.go.config.representers.RoleMapper
import gen.com.thoughtworks.go.config.representers.RolesMapper
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.server.api.HaltMessages.entityAlreadyExistsMessage
import static com.thoughtworks.go.server.api.HaltMessages.etagDoesNotMatch
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

@ExtendWith(ClearSingletonExtension.class)
class RolesControllerV1DelegateTest implements SecurityServiceTrait, ControllerTrait<RolesControllerV1Delegate> {

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @BeforeAll
  static void setupRouting() {
    SystemEnvironment.GO_SPARK_ROUTER_ENABLED.defaultValue = true
  }

  @AfterAll
  static void disableRouting() {
    SystemEnvironment.GO_SPARK_ROUTER_ENABLED.defaultValue = false
  }

  @Mock
  private RoleConfigService roleConfigService
  @Mock
  private Localizer localizer
  @Mock
  private EntityHashingService entityHashingService

  @Override
  RolesControllerV1Delegate createControllerInstance() {
    new RolesControllerV1Delegate(roleConfigService, new AuthenticationHelper(securityService), entityHashingService, localizer)
  }

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait {

      @Test
      void 'should allow all with security disabled'() {
        disableSecurity()

        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void "should disallow anonymous users, with security enabled"() {
        enableSecurity()
        loginAsAnonymous()

        makeHttpCall()

        assertRequestNotAuthorized()
      }

      @Test
      void 'should disallow normal users, with security enabled'() {
        enableSecurity()
        loginAsUser()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

      @Test
      void 'should allow admin, with security enabled'() {
        enableSecurity()
        loginAsAdmin()

        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void 'should disallow pipeline group admin users, with security enabled'() {
        enableSecurity()
        loginAsGroupAdmin()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

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
          .hasJsonBodySerializedWith(expectedRoles, RolesMapper)
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
          .hasJsonBodySerializedWith(new RolesConfig([pluginRoleConfig]), RolesMapper)
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
    class Security implements SecurityTestTrait {
      @BeforeEach
      void setUp() {
        when(roleConfigService.findRole("foo")).thenReturn(new RoleConfig("foo"))
      }

      @Test
      void 'should allow all with security disabled'() {
        disableSecurity()

        makeHttpCall()
        assertRequestAuthorized()
      }

      def 'should disallow anonymous users, with security enabled'() {
        enableSecurity()
        loginAsAnonymous()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

      def 'should disallow normal users, with security enabled'() {
        enableSecurity()
        loginAsUser()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

      def 'should allow admin, with security enabled'() {
        enableSecurity()
        loginAsAdmin()

        makeHttpCall()
        assertRequestAuthorized()
      }

      def 'should disallow pipeline group admin users, with security enabled'() {
        enableSecurity()
        loginAsGroupAdmin()

        makeHttpCall()
        assertRequestNotAuthorized()
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
          .hasJsonBodySerializedWith(role, RoleMapper)
      }

      @Test
      void 'should return 404 if the security auth config does not exist'() {
        when(roleConfigService.findRole('non-existent-security-auth-config')).thenReturn(null)

        getWithApiHeader(controller.controllerPath('/non-existent-security-auth-config'))

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage('Either the resource you requested was not found, or you are not authorized to perform this action.')
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
          .hasJsonBodySerializedWith(role, RoleMapper)
      }
    }
  }

  @Nested
  class Create {

    @Nested
    class Security implements SecurityTestTrait {

      @Test
      void 'should allow all with security disabled'() {
        disableSecurity()

        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      def 'should disallow anonymous users, with security enabled'() {
        enableSecurity()
        loginAsAnonymous()

        makeHttpCall()

        assertRequestNotAuthorized()
      }

      @Test
      def 'should disallow normal users, with security enabled'() {
        enableSecurity()
        loginAsUser()

        makeHttpCall()

        assertRequestNotAuthorized()
      }

      @Test
      def 'should allow admin, with security enabled'() {
        enableSecurity()
        loginAsAdmin()

        makeHttpCall()

        assertRequestAuthorized()
      }

      @Test
      def 'should disallow pipeline group admin users, with security enabled'() {
        enableSecurity()
        loginAsGroupAdmin()

        makeHttpCall()

        assertRequestNotAuthorized()
      }

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

        postWithApiHeader(controller.controllerPath(), RoleMapper.toJSON(role, requestContext))

        assertThatResponse()
          .isOk()
          .hasEtag('"some-md5"')
          .hasContentType(controller.mimeType)
          .hasJsonBodySerializedWith(role, RoleMapper)
      }

      @Test
      void 'should fail to save if there are validation errors'() {
        PluginRoleConfig role = new PluginRoleConfig('blackbird', 'blackbird')

        when(roleConfigService.create(any(), any(), any())).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.unprocessableEntity(LocalizedMessage.string("ENTITY_CONFIG_VALIDATION_FAILED"))
        })

        when(localizer.localize(any() as String, anyVararg())).then({ InvocationOnMock invocation ->
          return invocation.getArguments().first()
        })

        postWithApiHeader(controller.controllerPath(), RoleMapper.toJSON(role, requestContext))

        assertThatResponse()
          .isUnprocessibleEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("ENTITY_CONFIG_VALIDATION_FAILED")
      }

      @Test
      void 'should check for existence of role with same name'() {
        PluginRoleConfig role = new PluginRoleConfig('blackbird', 'skunkworks')

        RoleConfig expectedRole = new RoleConfig('blackbird', new RoleUser('bob'), new RoleUser('alice'))
        expectedRole.addError('name', 'Role names should be unique. Role with the same name exists.')

        when(roleConfigService.findRole('blackbird')).thenReturn(role)
        postWithApiHeader(controller.controllerPath(), RoleMapper.toJSON(expectedRole, requestContext))

        verify(roleConfigService, never()).create(any(), any(), any())

        assertThatResponse()
          .isUnprocessibleEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(entityAlreadyExistsMessage("role", "blackbird"))
          .hasJsonAtrribute('data', RoleMapper.toJSON(expectedRole, requestContext))
      }
    }
  }

  @Nested
  class Update {
    @Nested
    class Security implements SecurityTestTrait {
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
        ], RoleMapper.toJSON(this.roleConfig, requestContext))
      }

      @Test
      void 'should allow all with security disabled'() {
        disableSecurity()
        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void 'should disallow anonymous users, with security enabled'() {
        enableSecurity()
        loginAsAnonymous()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

      @Test
      void 'should disallow normal users, with security enabled'() {
        enableSecurity()
        loginAsUser()
        makeHttpCall()
        assertRequestNotAuthorized()
      }

      @Test
      void 'should allow admin, with security enabled'() {
        enableSecurity()
        loginAsAdmin()
        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void 'should disallow pipeline group admin users, with security enabled'() {
        enableSecurity()
        loginAsGroupAdmin()
        makeHttpCall()
        assertRequestNotAuthorized()
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
          name: 'blackbird',
          type: 'plugin'
        ]

        putWithApiHeader(controller.controllerPath('/foo'), headers, body)

        assertThatResponse()
          .isUnprocessibleEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(HaltMessages.renameOfEntityIsNotSupportedMessage("roles"))
      }

      @Test
      void 'should fail update if etag does not match'() {
        Role role = new PluginRoleConfig('blackbird', 'ldap')

        when(roleConfigService.findRole('blackbird')).thenReturn(role)
        when(entityHashingService.md5ForEntity(role)).thenReturn('cached-md5')

        putWithApiHeader(controller.controllerPath('/blackbird'), ['if-match': 'some-string'], RoleMapper.toJSON(role, requestContext))

        assertThatResponse()
          .preConditionFailed()
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

        putWithApiHeader(controller.controllerPath('/blackbird'), ['if-match': 'cached-md5'], RoleMapper.toJSON((Role) newRole, requestContext))

        assertThatResponse()
          .isOk()
          .hasEtag('"new-md5"')
          .hasContentType(controller.mimeType)
          .hasJsonBodySerializedWith(newRole, RoleMapper)
      }
    }
  }

  @Nested
  class Destroy {
    @Nested
    class Security implements SecurityTestTrait {
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

      @Test
      void 'should allow all with security disabled'() {
        disableSecurity()
        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void 'should disallow anonymous users, with security enabled'() {
        enableSecurity()
        loginAsAnonymous()

        makeHttpCall()
        assertRequestNotAuthorized()
      }

      @Test
      void 'should disallow normal users, with security enabled'() {
        enableSecurity()
        loginAsUser()
        makeHttpCall()
        assertRequestNotAuthorized()
      }

      @Test
      void 'should allow admin, with security enabled'() {
        enableSecurity()
        loginAsAdmin()
        makeHttpCall()
        assertRequestAuthorized()
      }

      @Test
      void 'should disallow pipeline group admin users, with security enabled'() {
        enableSecurity()
        loginAsGroupAdmin()
        makeHttpCall()
        assertRequestNotAuthorized()
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
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should render the success message on deleting a role'() {
        PluginRoleConfig role = new PluginRoleConfig('blackbird', 'ldap')
        when(roleConfigService.findRole('blackbird')).thenReturn(role)

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.arguments.last()
          result.setMessage(LocalizedMessage.string("RESOURCE_DELETE_SUCCESSFUL", 'role', role.getName()))
        }).when(roleConfigService).delete(any(), eq(role), any())

        when(localizer.localize(any() as String, anyVararg())).then({ InvocationOnMock invocation ->
          return invocation.getArguments().first()
        })

        deleteWithApiHeader(controller.controllerPath('/blackbird'))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage('RESOURCE_DELETE_SUCCESSFUL')
      }

      @Test
      void 'should render the validation errors on failure to delete'() {
        PluginRoleConfig role = new PluginRoleConfig('blackbird', 'ldap')

        when(roleConfigService.findRole('blackbird')).thenReturn(role)

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.arguments.last()
          result.unprocessableEntity(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", 'validation error'))
        }).when(roleConfigService).delete(any(), eq(role), any())

        when(localizer.localize(any() as String, anyVararg())).then({ InvocationOnMock invocation ->
          return invocation.getArguments().first()
        })

        deleteWithApiHeader(controller.controllerPath('/blackbird'))

        assertThatResponse()
          .isUnprocessibleEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage('SAVE_FAILED_WITH_REASON')
      }
    }
  }
}