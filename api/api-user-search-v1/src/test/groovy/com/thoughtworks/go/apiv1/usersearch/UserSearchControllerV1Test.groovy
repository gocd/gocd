/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.apiv1.usersearch

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthorizationHelper
import com.thoughtworks.go.apiv1.usersearch.representers.UserSearchResultsRepresenter
import com.thoughtworks.go.domain.User
import com.thoughtworks.go.server.security.UserSearchService
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

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class UserSearchControllerV1Test implements SecurityServiceTrait, ControllerTrait<UserSearchControllerV1> {

  @Mock
  UserSearchService userSearchService


  @Override
  UserSearchControllerV1 createControllerInstance() {
    new UserSearchControllerV1(new ApiAuthorizationHelper(securityService, goConfigService), userSearchService)
  }

  @Nested
  class Show {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Delegate SecurityServiceTrait s = UserSearchControllerV1Test.this
      @Delegate ControllerTrait<UserSearchControllerV1> c = UserSearchControllerV1Test.this

      @Override
      String getControllerMethodUnderTest() {
        return "show"
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
        loginAsAdmin()
      }

      @Test
      void 'should return blank array if no users found for query string'() {
        def searchTerm = 'blah blah'

        when(userSearchService.search(eq(searchTerm), any())).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(1)
          result.setMessage("No results found.")
          return []
        })

        getWithApiHeader(controller.controllerPath([q: searchTerm]))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJson(toObjectString({ UserSearchResultsRepresenter.toJSON(it, searchTerm, []) }))
      }

      @Test
      void 'should return a list of users returned by user search service'() {
        def searchTerm = 'blah blah'
        def user = new User("bob", searchTerm, "bob@example.com")
        def expectedUsers = [
          user
        ]
        when(userSearchService.search(eq(searchTerm), any())).thenReturn(expectedUsers)

        getWithApiHeader(controller.controllerPath([q: searchTerm]))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJson(toObjectString({ UserSearchResultsRepresenter.toJSON(it, searchTerm, expectedUsers) }))
      }

      @Test
      void "should render error if search operation fails"(){
        def searchTerm = 'blah blah'

        when(userSearchService.search(eq(searchTerm), any())).then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.getArgument(1)
          result.badRequest("boom!")
          return []
        })

        getWithApiHeader(controller.controllerPath([q: searchTerm]))

        assertThatResponse()
          .isBadRequest()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("boom!")
      }
    }
  }
}
