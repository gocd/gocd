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
package com.thoughtworks.go.apiv1.admin.backups

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.admin.backups.representers.BackupRepresenter
import com.thoughtworks.go.server.domain.BackupStatus
import com.thoughtworks.go.server.domain.ServerBackup
import com.thoughtworks.go.server.service.BackupService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.util.HaltApiMessages.deprecatedConfirmHeaderMissing
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

class BackupsControllerTest implements ControllerTrait<BackupsController>, SecurityServiceTrait {
  BackupService backupService = mock(BackupService.class)

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
        postWithApiHeader(controller.controllerBasePath(), [confirm: 'true'], null)
      }
    }

    @Nested
    class AsAdmin {
      @Test
      void 'should create a backup'() {
        enableSecurity()
        loginAsAdmin()
        def backup = new ServerBackup("/foo/bar", new Date(), currentUserLoginName().toString(), BackupStatus.COMPLETED, "Completed", 23)

        doReturn(backup).when(backupService).startBackup(eq(currentUsername()))

        postWithApiHeader(controller.controllerBasePath(), [confirm: 'true'], null)

        verify(backupService).startBackup(eq(currentUsername()))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(BackupRepresenter.class, backup)
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
          .hasJsonMessage(deprecatedConfirmHeaderMissing())
      }

      @Test
      void 'bails if confirm header is set to non true value'() {
        enableSecurity()
        loginAsAdmin()
        postWithApiHeader(controller.controllerBasePath(), [confirm: 'foo'], null)
        assertThatResponse()
          .isBadRequest()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(deprecatedConfirmHeaderMissing())
      }
    }
  }

  @Override
  BackupsController createControllerInstance() {
    return new BackupsController(new ApiAuthenticationHelper(securityService, goConfigService), backupService)
  }
}
