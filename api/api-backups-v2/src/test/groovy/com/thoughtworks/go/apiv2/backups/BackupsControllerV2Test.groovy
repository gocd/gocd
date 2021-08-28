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
package com.thoughtworks.go.apiv2.backups

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv2.backups.representers.BackupRepresenter
import com.thoughtworks.go.config.exceptions.EntityType
import com.thoughtworks.go.server.domain.BackupStatus
import com.thoughtworks.go.server.domain.ServerBackup
import com.thoughtworks.go.server.service.BackupService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.util.HaltApiMessages.confirmHeaderMissing
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

class BackupsControllerV2Test implements SecurityServiceTrait, ControllerTrait<BackupsControllerV2> {
  BackupService backupService = mock(BackupService.class)
  long BACKUP_ID = 12

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
        postWithApiHeader(controller.controllerBasePath(), ['X-GoCD-Confirm': 'true'], null)
      }
    }

    @Nested
    class AsAdmin {
      @Test
      void 'should schedule a backup creation'() {
        enableSecurity()
        loginAsAdmin()
        def backup = new ServerBackup("/foo/bar", new Date(), currentUserLoginName().toString(), BackupStatus.IN_PROGRESS, "", BACKUP_ID)

        doReturn(backup).when(backupService).scheduleBackup(eq(currentUsername()))

        postWithApiHeader(controller.controllerBasePath(), ['X-GoCD-Confirm': 'true'], null)

        verify(backupService).scheduleBackup(eq(currentUsername()))

        assertThatResponse()
          .isAccepted()
          .hasContentType(controller.mimeType)
          .hasHeader("Location", "/go/api/backups/12")
          .hasHeader("Retry-After", "5")
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
          .hasJsonMessage(confirmHeaderMissing())
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
        getWithApiHeader(controller.controllerPath(BACKUP_ID))
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
      void 'should get 404 when id does not exist'() {
        doReturn(Optional.empty()).when(backupService).getServerBackup(BACKUP_ID)

        getWithApiHeader(controller.controllerPath(BACKUP_ID))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(EntityType.Backup.notFoundMessage(BACKUP_ID))
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should get serverBackup json'() {
        def backup = new ServerBackup("/foo/bar", new Date(), currentUserLoginName().toString(), BackupStatus.IN_PROGRESS, "", BACKUP_ID)

        doReturn(Optional.of(backup)).when(backupService).getServerBackup(BACKUP_ID)

        getWithApiHeader(controller.controllerPath(BACKUP_ID))

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(BackupRepresenter.class, backup)
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should get running backup'() {
        def backup = new ServerBackup("/foo/bar", new Date(), currentUserLoginName().toString(), BackupStatus.IN_PROGRESS, "", BACKUP_ID)
        doReturn(Optional.of(backup)).when(backupService).runningBackup()

        getWithApiHeader(controller.controllerPath("running"))

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(BackupRepresenter.class, backup)
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return bad request when the id is not specified as a number'() {
        getWithApiHeader(controller.controllerPath("foobar"))

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The `id` parameter should be a number, or the keyword `running`")
          .hasContentType(controller.mimeType)
      }
    }
  }

  @Override
  BackupsControllerV2 createControllerInstance() {
    return new BackupsControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), backupService)
  }
}
