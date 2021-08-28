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
package com.thoughtworks.go.apiv1.mailserver

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv1.mailserver.representers.MailServerRepresenter
import com.thoughtworks.go.config.MailHost
import com.thoughtworks.go.config.update.CreateOrUpdateUpdateMailHostCommand
import com.thoughtworks.go.config.update.DeleteMailHostCommand
import com.thoughtworks.go.server.service.ServerConfigService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.Routes
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
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class MailServerControllerV1Test implements SecurityServiceTrait, ControllerTrait<MailServerControllerV1> {

  @Mock
  ServerConfigService serverConfigService

  MailHost mailHost = new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com")


  @Override
  MailServerControllerV1 createControllerInstance() {
    new MailServerControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), goConfigService, serverConfigService)
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
      void 'should return mailhost configuration'() {
        when(goConfigService.getMailHost()).thenReturn(mailHost)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(MailServerRepresenter, mailHost)
      }

      @Test
      void 'should return 404 when MailHost is not configured'() {
        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("SMTP config was not found!")
          .hasContentType(controller.mimeType)
      }

    }
  }

  @Nested
  class Create {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'createOrUpdate'
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), [:])
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
      void 'should create mail host configuration from given json payload'() {
        def mailhost = new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com")

        def json = [
          hostname    : mailhost.hostName,
          port        : mailhost.port,
          username    : mailhost.username,
          password    : mailhost.password,
          tls         : mailhost.tls,
          sender_email: mailhost.from,
          admin_email : mailhost.adminMail
        ]

        when(goConfigService.getMailHost()).thenReturn(MailServerRepresenter.fromJSON(GsonTransformer.getInstance().jsonReaderFrom(json)))
        postWithApiHeader(controller.controllerPath(), json)

        verify(goConfigService).updateConfig(any(CreateOrUpdateUpdateMailHostCommand.class), eq(currentUsername()))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(MailServerRepresenter, mailHost)
      }
    }
  }

  @Nested
  class Update {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'createOrUpdate'
      }

      @Override
      void makeHttpCall() {
        putWithApiHeader(controller.controllerPath(), [:])
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
      void 'should update mail host configuration from given json payload'() {
        def mailhost = new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com")

        def json = [
          hostname    : mailhost.hostName,
          port        : mailhost.port,
          username    : mailhost.username,
          password    : mailhost.password,
          tls         : mailhost.tls,
          sender_email: mailhost.from,
          admin_email : mailhost.adminMail
        ]

        when(goConfigService.getMailHost()).thenReturn(MailServerRepresenter.fromJSON(GsonTransformer.getInstance().jsonReaderFrom(json)))
        putWithApiHeader(controller.controllerPath(), json)

        verify(goConfigService).updateConfig(any(CreateOrUpdateUpdateMailHostCommand.class), eq(currentUsername()))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(MailServerRepresenter, mailHost)
      }
    }
  }

  @Nested
  class Delete {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "deleteMailConfig"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerBasePath())
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
      void 'should delete smtp config'() {
        when(goConfigService.getMailHost()).thenReturn(new MailHost())
        deleteWithApiHeader(controller.controllerBasePath())

        verify(goConfigService).updateConfig(any(DeleteMailHostCommand.class), eq(currentUsername()))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("SMTP config was deleted successfully!")

      }
    }
  }

  @Nested
  class SendTestEmail {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return 'sendTestEmail'
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(Routes.MailServer.TEST_EMAIL), [:])
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
      void 'should send test email'() {
        def mailhost = new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com")

        def json = [
          hostname    : mailhost.hostName,
          port        : mailhost.port,
          username    : mailhost.username,
          password    : mailhost.password,
          tls         : mailhost.tls,
          sender_email: mailhost.from,
          admin_email : mailhost.adminMail
        ]

        when(serverConfigService.sendTestMail(eq(mailHost), any(HttpLocalizedOperationResult.class))).thenAnswer({ InvocationOnMock invocation ->
          def result = invocation.getArgument(1) as HttpLocalizedOperationResult
          result.setMessage("woohoo!")
        })
        postWithApiHeader(controller.controllerPath(Routes.MailServer.TEST_EMAIL), json)

        verify(serverConfigService).sendTestMail(eq(mailHost), any(HttpLocalizedOperationResult.class))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("woohoo!")
      }
    }
  }

}
