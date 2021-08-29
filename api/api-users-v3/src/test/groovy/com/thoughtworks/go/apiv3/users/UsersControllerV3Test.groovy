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
package com.thoughtworks.go.apiv3.users

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv3.users.model.UserToRepresent
import com.thoughtworks.go.apiv3.users.representers.UserRepresenter
import com.thoughtworks.go.apiv3.users.representers.UsersRepresenter
import com.thoughtworks.go.config.RolesConfig
import com.thoughtworks.go.domain.NullUser
import com.thoughtworks.go.domain.User
import com.thoughtworks.go.helper.UsersMother
import com.thoughtworks.go.i18n.LocalizedMessage
import com.thoughtworks.go.server.service.RoleConfigService
import com.thoughtworks.go.server.service.UserService
import com.thoughtworks.go.server.service.result.BulkUpdateUsersOperationResult
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.doAnswer
import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class UsersControllerV3Test implements SecurityServiceTrait, ControllerTrait<UsersControllerV3> {
  @Mock
  UserService userService

  @Mock
  RoleConfigService roleConfigService


  @Override
  UsersControllerV3 createControllerInstance() {
    new UsersControllerV3(new ApiAuthenticationHelper(securityService, goConfigService), userService, securityService, roleConfigService)
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
      void 'should list all users'() {
        def bobUser = UsersMother.withName("bob")
        when(userService.allUsers()).thenReturn([bobUser])

        getWithApiHeader(controller.controllerPath())

        def expectedUser = UserToRepresent.from(bobUser, securityService.isUserAdmin(bobUser.getUsername()), new RolesConfig())
        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(UsersRepresenter, [expectedUser])
      }
    }
  }

  @Nested
  class Show {
    def username = 'bob'

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "show"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(username))
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
      void 'should get one user'() {
        def bobUser = UsersMother.withName("bob")
        when(userService.findUserByName(username)).thenReturn(bobUser)

        getWithApiHeader(controller.controllerPath(username))

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(UserRepresenter, UserToRepresent.from(bobUser, false, new RolesConfig()))
      }
    }
  }

  @Nested
  class Create {
    def username = 'bob'

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "create"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), [:])
      }
    }

    @Nested
    class AsAdmin {
      @Test
      void 'should create a new user'() {
        enableSecurity()
        loginAsAdmin()

        def jsonPayload = [
          login_name: username
        ]

        def expectedUser = new User(username)
        expectedUser.setDisplayName(username)

        when(userService.findUserByName(username))
          .thenReturn(new NullUser())
          .thenReturn(expectedUser)

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(UserRepresenter, UserToRepresent.from(expectedUser, false, new RolesConfig()))
      }
    }

    @Test
    void 'should not create a user if one already exists'() {
      enableSecurity()
      loginAsAdmin()

      def jsonPayload = [
        login_name: username
      ]

      when(userService.findUserByName(username)).thenReturn(new User(username))

      postWithApiHeader(controller.controllerPath(), jsonPayload)

      assertThatResponse()
        .isUnprocessableEntity()
        .hasJsonMessage("Failed to add User 'bob'. Another User with the same name already exists.")
    }
  }

  @Nested
  class Patch {
    def username = 'bob'

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "patchUser"
      }

      @Override
      void makeHttpCall() {
        patchWithApiHeader(controller.controllerPath(username), [:])
      }
    }

    @Nested
    class AsAdmin {
      @Test
      void 'should update the existing user'() {
        enableSecurity()
        loginAsAdmin()

        def jsonPayload = [
          email: "new-email"
        ]

        when(userService.findUserByName(username)).thenReturn(new User(username))
        patchWithApiHeader(controller.controllerPath(username), jsonPayload)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(UserRepresenter, UserToRepresent.from(new User(username), false, new RolesConfig()))
      }
    }

    @Test
    void 'should not update login name of the user'() {
      enableSecurity()
      loginAsAdmin()

      def jsonPayload = [
        login_name: "new-username"
      ]

      when(userService.findUserByName(username)).thenReturn(new User(username))
      patchWithApiHeader(controller.controllerPath(username), jsonPayload)

      assertThatResponse()
        .isUnprocessableEntity()
        .hasJsonMessage("Renaming of User is not supported by this API.")
    }

    @Test
    void 'should fail updating non existing user'() {
      enableSecurity()
      loginAsAdmin()

      def jsonPayload = [
        login_name: "new-username"
      ]

      when(userService.findUserByName(username)).thenReturn(new NullUser())
      patchWithApiHeader(controller.controllerPath(username), jsonPayload)

      assertThatResponse()
        .isNotFound()
    }

  }

  @Nested
  class Delete {
    def username = 'bob'

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "deleteUser"
      }

      @Override
      void makeHttpCall() {
        delete(controller.controllerPath(username))
      }
    }

    @Nested
    class AsAdmin {
      @Test
      void 'should delete the existing user'() {
        enableSecurity()
        loginAsAdmin()

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
          result.setMessage("The user '" + username + "' was deleted successfully.")
        }).when(userService).deleteUser(eq(username), eq(currentUsernameString()), any(HttpLocalizedOperationResult.class))

        delete(controller.controllerPath(username))

        assertThatResponse()
          .isOk()
          .hasJsonMessage("The user 'bob' was deleted successfully.")
      }

      @Test
      void 'should render error while deleting the enabled user'() {
        enableSecurity()
        loginAsAdmin()

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
          result.badRequest("User '" + username + "' is not disabled.")
        }).when(userService).deleteUser(eq(username), eq(currentUsernameString()), any(HttpLocalizedOperationResult.class))

        delete(controller.controllerPath(username))

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("User 'bob' is not disabled.")
      }
    }
  }

  @Nested
  class BulkDelete {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "bulkDelete"
      }

      @Override
      void makeHttpCall() {
        delete(controller.controllerPath())
      }
    }

    @Nested
    class AsAdmin {
      def usersToDelete = ['John', 'Bob']

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
       }

      @Test
      void 'should delete a list of users'() {
        def requestBody = [
          users: usersToDelete
        ]

        doAnswer({ InvocationOnMock invocation ->
          BulkUpdateUsersOperationResult result = (BulkUpdateUsersOperationResult) invocation.arguments.last()
          result.setMessage(LocalizedMessage.resourcesDeleteSuccessful("Users", usersToDelete))
        }).when(userService).deleteUsers(eq(usersToDelete), eq(currentUsernameString()), any(BulkUpdateUsersOperationResult.class))

        deleteWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isOk()
          .hasJsonMessage("Users 'John, Bob' were deleted successfully.")
      }

      @Test
      void 'should render error while deleting the enabled user'() {
        def requestBody = [
          users: usersToDelete
        ]

        doAnswer({ InvocationOnMock invocation ->
          BulkUpdateUsersOperationResult result = (BulkUpdateUsersOperationResult) invocation.arguments.last()
          result.unprocessableEntity("Deletion failed because some users were either enabled or do not exist.")
          result.addNonExistentUserName(usersToDelete[0])
          result.addNonExistentUserName(usersToDelete[1])
        }).when(userService).deleteUsers(eq(usersToDelete), eq(currentUsernameString()), any(BulkUpdateUsersOperationResult.class))

        deleteWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Deletion failed because some users were either enabled or do not exist.")
          .hasJsonAttribute("non_existent_users", usersToDelete)
      }
    }
  }

  @Nested
  class BulkEnableDisableUsers {
    def bulkEnableDisableUsersPath = '/operations/state'

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "bulkUpdateUsersState"
      }

      @Override
      void makeHttpCall() {
        patchWithApiHeader(controller.controllerPath(bulkEnableDisableUsersPath), [:])
      }
    }

    @Nested
    class AsAdmin {
      def usersToEnable = ['John', 'Bob']

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should enable a list of users'() {

        def requestBody = [
          users     : usersToEnable,
          operations: [
            enable: true
          ]
        ]

        doAnswer({ InvocationOnMock invocation ->
          BulkUpdateUsersOperationResult result = (BulkUpdateUsersOperationResult) invocation.arguments.last()
          result.setMessage("Users '${usersToEnable.join(', ')}' were enabled successfully.")
        }).when(userService).bulkEnableDisableUsers(eq(usersToEnable), eq(true), any(BulkUpdateUsersOperationResult.class))

        patchWithApiHeader(controller.controllerPath(bulkEnableDisableUsersPath), requestBody)

        assertThatResponse()
          .isOk()
          .hasJsonMessage("Users 'John, Bob' were enabled successfully.")
      }

      @Test
      void 'should fail to enable non existing user'() {
        def requestBody = [
          users     : usersToEnable,
          operations: [
            enable: true
          ]
        ]

        doAnswer({ InvocationOnMock invocation ->
          BulkUpdateUsersOperationResult result = (BulkUpdateUsersOperationResult) invocation.arguments.last()
          result.unprocessableEntity("Update failed because some users do not exist.")
          result.addNonExistentUserName('John')
        }).when(userService).bulkEnableDisableUsers(eq(usersToEnable), eq(true), any(BulkUpdateUsersOperationResult.class))

        patchWithApiHeader(controller.controllerPath(bulkEnableDisableUsersPath), requestBody)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Update failed because some users do not exist.")
          .hasJsonAttribute("non_existent_users", ['John'])
      }
    }
  }
}
