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
package com.thoughtworks.go.apiv2.notificationfilter

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv2.notificationfilter.representers.NotificationFilterRepresenter
import com.thoughtworks.go.apiv2.notificationfilter.representers.NotificationFiltersRepresenter
import com.thoughtworks.go.config.exceptions.RecordNotFoundException
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException
import com.thoughtworks.go.domain.NotificationFilter
import com.thoughtworks.go.domain.StageEvent
import com.thoughtworks.go.domain.User
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.UserService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class NotificationFilterControllerV2Test implements SecurityServiceTrait, ControllerTrait<NotificationFilterControllerV2> {
  @Mock
  private UserService userService

  @Mock
  private EntityHashingService entityHashingService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  NotificationFilterControllerV2 createControllerInstance() {
    new NotificationFilterControllerV2(new ApiAuthenticationHelper(securityService, goConfigService),
      userService,
      goConfigService,
      entityHashingService
    )
  }

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath())
      }
    }

    @BeforeEach
    void setUp() {
      loginAsUser()
      when(goConfigService.isSmtpEnabled()).thenReturn(true)
    }

    @Test
    void 'should return list of filters for the user'() {
      def user = mock(User)
      def filterOne = new NotificationFilter("up42", "Stage_1", StageEvent.Breaks, true)
      filterOne.setId(200L)
      def filterTwo = new NotificationFilter("up43", "Stage_2", StageEvent.Fails, false)
      filterTwo.setId(201L)
      def notificationFilters = Arrays.asList(filterOne, filterTwo)
      when(userService.findUserByName(currentUsernameString())).thenReturn(user)
      when(user.getNotificationFilters()).thenReturn(notificationFilters)

      getWithApiHeader(controller.controllerBasePath())

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(NotificationFiltersRepresenter.class, notificationFilters)
    }

    @Test
    void 'should not error out when SMTP is not configured'() {
      def user = mock(User)
      when(goConfigService.isSmtpEnabled()).thenReturn(false)
      when(userService.findUserByName(currentUsernameString())).thenReturn(user)
      when(user.getNotificationFilters()).thenReturn([])

      getWithApiHeader(controller.controllerBasePath())

      assertThatResponse()
        .isOk()
    }
  }

  @Nested
  class Show {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "show"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath(100))
      }
    }

    @BeforeEach
    void setUp() {
      loginAsUser()
      when(goConfigService.isSmtpEnabled()).thenReturn(true)
    }

    @Test
    void 'should return a notification filter given id'() {
      def user = mock(User)
      def filterOne = new NotificationFilter("up42", "Stage_1", StageEvent.Breaks, true)
      filterOne.setId(200L)
      def filterTwo = new NotificationFilter("up43", "Stage_2", StageEvent.Fails, false)
      filterTwo.setId(201L)
      def notificationFilters = Arrays.asList(filterOne, filterTwo)
      when(userService.findUserByName(currentUsernameString())).thenReturn(user)
      when(user.getNotificationFilters()).thenReturn(notificationFilters)

      getWithApiHeader(controller.controllerPath(200))

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(NotificationFilterRepresenter.class, filterOne)
    }

    @Test
    void 'should throw record not found when notification with id does not exist'() {
      def user = mock(User)
      def filterOne = new NotificationFilter("up42", "Stage_1", StageEvent.Breaks, true)
      filterOne.setId(200L)
      def filterTwo = new NotificationFilter("up43", "Stage_2", StageEvent.Fails, false)
      filterTwo.setId(201L)
      def notificationFilters = Arrays.asList(filterOne, filterTwo)
      when(userService.findUserByName(currentUsernameString())).thenReturn(user)
      when(user.getNotificationFilters()).thenReturn(notificationFilters)

      getWithApiHeader(controller.controllerPath(202))

      assertThatResponse()
        .isNotFound()
        .hasJsonMessage("Notification filter with id '202' was not found!")
    }

    @Test
    void 'should not error out when SMTP is not configured'() {
      def user = mock(User)
      def filterOne = new NotificationFilter("up42", "Stage_1", StageEvent.Breaks, true)
      filterOne.setId(200L)
      def filterTwo = new NotificationFilter("up43", "Stage_2", StageEvent.Fails, false)
      filterTwo.setId(201L)
      def notificationFilters = Arrays.asList(filterOne, filterTwo)
      when(userService.findUserByName(currentUsernameString())).thenReturn(user)
      when(user.getNotificationFilters()).thenReturn(notificationFilters)
      when(goConfigService.isSmtpEnabled()).thenReturn(false)

      getWithApiHeader(controller.controllerPath(200))

      assertThatResponse()
        .isOk()
    }
  }

  @Nested
  class Create {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "createNotificationFilter"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerBasePath(), [:])
      }
    }

    @BeforeEach
    void setUp() {
      loginAsUser()
      when(goConfigService.isSmtpEnabled()).thenReturn(true)
    }

    @Test
    void 'should call UserService to create the notification filter for the user'() {
      def payload = [
        "pipeline"     : "up42",
        "stage"        : "unit-test",
        "event"        : "Breaks",
        "match_commits": true
      ]
      def fromPayload = new NotificationFilter("up42", "unit-test", StageEvent.Breaks, true)
      def withId = new NotificationFilter(fromPayload)
      withId.setId(100L)
      when(entityHashingService.hashForEntity(withId)).thenReturn("digest")
      doAnswer({ invocation ->
        invocation.getArgument(1).setId(100)
      }).when(userService).addNotificationFilter(currentUserLoginId(), fromPayload)

      postWithApiHeader(controller.controllerBasePath(), payload)

      verify(userService).addNotificationFilter(currentUserLoginId(), fromPayload)
      assertThatResponse()
        .isOk()
        .hasEtag('"digest"')
        .hasBodyWithJsonObject(NotificationFilterRepresenter, withId)
    }

    @Test
    void 'should error out when SMTP is not configured'() {
      when(goConfigService.isSmtpEnabled()).thenReturn(false)

      postWithApiHeader(controller.controllerBasePath(), [:])

      assertThatResponse()
        .isBadRequest()
        .hasJsonMessage("SMTP settings are currently not configured. Ask your administrator to configure SMTP settings.")
    }

    @Test
    void 'should error with unprocessable entity when has validation errors'() {
      def payload = [
        "pipeline"     : "up43",
        "stage"        : "unit-test",
        "event"        : "Breaks",
        "match_commits": true
      ]
      def user = new User("bob")
      doAnswer({ invocation ->
        NotificationFilter filter = invocation.getArgument(1)
        filter.addError("pipelineName", "Some error")
        throw new UnprocessableEntityException()
      }).when(userService).addNotificationFilter(eq(currentUserLoginId()), any(NotificationFilter))

      postWithApiHeader(controller.controllerBasePath(), payload)

      def expectedResponse = [
        "_links"       : [
          "doc" : [
            "href": apiDocsUrl('#notification-filters')
          ],
          "find": [
            "href": "http://test.host/go/api/notification_filters/:id"
          ]
        ],
        "pipeline"     : "up43",
        "stage"        : "unit-test",
        "event"        : "Breaks",
        "match_commits": true,
        "errors"       : [
          "pipeline_name": ["Some error"]
        ]
      ]

      assertThatResponse()
        .isUnprocessableEntity()
        .hasJsonBody(expectedResponse)
    }
  }

  @Nested
  class Update {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "updateFilter"
      }

      @Override
      void makeHttpCall() {
        patchWithApiHeader(controller.controllerPath(100L), [:])
      }
    }

    @BeforeEach
    void setUp() {
      loginAsUser()
      when(goConfigService.isSmtpEnabled()).thenReturn(true)
    }

    @Test
    void 'should handle record not found exception'() {
      def payload = [
        "pipeline"     : "up42",
        "stage"        : "unit-test",
        "event"        : "Breaks",
        "match_commits": true
      ]
      def user = new User("bob")
      user.addNotificationFilter(notificationFilter(100, "up42", "up42_stage", StageEvent.All))
      user.addNotificationFilter(notificationFilter(101, "up43", "up43_stage", StageEvent.Breaks))
      when(userService.updateNotificationFilter(eq(currentUserLoginId()), any(NotificationFilter)))
        .thenThrow(new RecordNotFoundException("Notification filter with id '102' was not found!"))

      patchWithApiHeader(controller.controllerPath(102L), payload)

      verify(userService, never()).saveOrUpdate(user)
      assertThatResponse()
        .isNotFound()
        .hasJsonMessage("Notification filter with id '102' was not found!")
    }

    @Test
    void 'should error with unprocessable entity when has validation errors'() {
      def payload = [
        "pipeline"     : "up43",
        "stage"        : "unit-test",
        "event"        : "Breaks",
        "match_commits": true
      ]
      def user = new User("bob")
      user.addNotificationFilter(notificationFilter(100, "up42", "unit-test", StageEvent.All))
      user.addNotificationFilter(notificationFilter(101, "up43", "unit-test", StageEvent.Breaks))
      doAnswer({ invocation ->
        NotificationFilter filter = invocation.getArgument(1)
        filter.addError("pipelineName", "Some error")
        throw new UnprocessableEntityException()
      }).when(userService).updateNotificationFilter(eq(currentUserLoginId()), any(NotificationFilter))

      patchWithApiHeader(controller.controllerPath(100L), payload)

      def expectedResponse = [
        "_links"       : [
          "self": [
            "href": "http://test.host/go/api/notification_filters/100"
          ],
          "doc" : [
            "href": apiDocsUrl('#notification-filters')
          ],
          "find": [
            "href": "http://test.host/go/api/notification_filters/:id"
          ]
        ],
        "id"           : 100,
        "pipeline"     : "up43",
        "stage"        : "unit-test",
        "event"        : "Breaks",
        "match_commits": true,
        "errors"       : [
          "pipeline_name": ["Some error"]
        ]
      ]

      assertThatResponse()
        .isUnprocessableEntity()
        .hasJsonBody(expectedResponse)
    }

    @Test
    void 'should update the notification filter and return the updated value'() {
      def payload = [
        "pipeline"     : "up42",
        "stage"        : "unit-test",
        "event"        : "Breaks",
        "match_commits": true
      ]
      def user = new User("bob")
      user.addNotificationFilter(notificationFilter(100, "up42", "up42_stage", StageEvent.All))
      user.addNotificationFilter(notificationFilter(101, "up43", "up43_stage", StageEvent.Breaks))
      def updatedFilterValue = notificationFilter(100, "up42", "unit-test", StageEvent.Breaks)
      when(entityHashingService.hashForEntity(updatedFilterValue)).thenReturn("Updated etag")

      patchWithApiHeader(controller.controllerPath(100L), payload)

      verify(userService).updateNotificationFilter(currentUserLoginId(), updatedFilterValue)
      assertThatResponse()
        .isOk()
        .hasEtag('"Updated etag"')
        .hasBodyWithJsonObject(NotificationFilterRepresenter, updatedFilterValue)
    }

    @Test
    void 'should error out when SMTP is not configured'() {
      when(goConfigService.isSmtpEnabled()).thenReturn(false)

      patchWithApiHeader(controller.controllerPath(100L), [:])

      assertThatResponse()
        .isBadRequest()
        .hasJsonMessage("SMTP settings are currently not configured. Ask your administrator to configure SMTP settings.")
    }
  }

  @Nested
  class Delete {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "deleteFilter"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath(100))
      }
    }

    @BeforeEach
    void setUp() {
      loginAsUser()
      when(goConfigService.isSmtpEnabled()).thenReturn(true)
    }

    @Test
    void 'should call UserService to delete the notification filter for user'() {
      deleteWithApiHeader(controller.controllerPath(100))

      verify(userService).removeNotificationFilter(currentUserLoginId(), 100)
      assertThatResponse()
        .isOk()
        .hasJsonMessage("Notification filter is successfully deleted!")
    }

    @Test
    void 'should not error out when SMTP is not configured'() {
      def user = mock(User)
      when(userService.findUserByName(currentUsernameString())).thenReturn(user)
      when(goConfigService.isSmtpEnabled()).thenReturn(false)

      deleteWithApiHeader(controller.controllerPath(100))

      assertThatResponse()
        .isOk()
    }
  }

  static def notificationFilter(long id, String pipeline, String stage, StageEvent event) {
    def filter = new NotificationFilter(pipeline, stage, event, true)
    filter.setId(id)
    return filter
  }
}
