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

package com.thoughtworks.go.apiv1.admin.backups

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.admin.backups.representers.BackupRepresenter
import com.thoughtworks.go.i18n.LocalizedMessage
import com.thoughtworks.go.server.domain.ServerBackup
import com.thoughtworks.go.server.service.BackupService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.invocation.InvocationOnMock

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

class BackupsControllerDelegateTest implements ControllerTrait<BackupsControllerDelegate>, SecurityServiceTrait {
  BackupService backupService = mock(BackupService.class)

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
        return 'create'
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerBasePath(), [confirm: true], null)
      }
    }

    @Nested
    class AsAdmin {
      @Test
      void 'should create a backup'() {
        enableSecurity()
        loginAsAdmin()
        def backup = new ServerBackup("/foo/bar", new Date(), currentUserLoginName().toString())

        doAnswer({ InvocationOnMock invocationOnMock ->
          HttpLocalizedOperationResult result = invocationOnMock.arguments.last() as HttpLocalizedOperationResult
          result.setMessage(LocalizedMessage.string("SUCCESS"))
          return backup
        }).when(backupService).startBackup(eq(currentUsername()), any() as HttpLocalizedOperationResult)

        postWithApiHeader(controller.controllerBasePath(), [confirm: true], null)

        verify(backupService).startBackup(eq(currentUsername()), any() as HttpLocalizedOperationResult)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBodySerializedWith(backup, BackupRepresenter.class)
      }
    }

    @Nested
    class CORS {
      @Test
      void 'bails if confirm header is missing'() {
        enableSecurity()
        loginAsAdmin()
        postWithApiHeader(controller.controllerBasePath(), null)
        assertThatResponse()
          .isBadRequest()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Missing required header 'Confirm' with value 'true'")
      }

      @Test
      void 'bails if confirm header is set to non true value'() {
        enableSecurity()
        loginAsAdmin()
        postWithApiHeader(controller.controllerBasePath(), [confirm: 'foo'])
        assertThatResponse()
          .isBadRequest()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Missing required header 'Confirm' with value 'true'")
      }
    }
  }

  @Override
  BackupsControllerDelegate createControllerInstance() {
    return new BackupsControllerDelegate(new ApiAuthenticationHelper(securityService, goConfigService), backupService, localizer)
  }
}
