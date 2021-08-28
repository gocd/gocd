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
package com.thoughtworks.go.apiv1.backupconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.backupconfig.representers.BackupConfigRepresenter
import com.thoughtworks.go.config.BackupConfig
import com.thoughtworks.go.config.ServerConfig
import com.thoughtworks.go.config.exceptions.EntityType
import com.thoughtworks.go.config.update.DeleteBackupConfigCommand
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.ArgumentMatchers.isA
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class BackupConfigControllerV1Test implements SecurityServiceTrait, ControllerTrait<BackupConfigControllerV1> {
  @Override
  BackupConfigControllerV1 createControllerInstance() {
    new BackupConfigControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), goConfigService)
  }

  @Nested
  class Show {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

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
    class AsAuthorizedUser {
      private ServerConfig serverConfig

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
        serverConfig = new ServerConfig()
        when(goConfigService.serverConfig()).thenReturn(serverConfig)
      }

      @Test
      void 'should render an empty backup config when a backup config is not present'() {
        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(toObjectString({ BackupConfigRepresenter.toJSON(it, new BackupConfig()) }))
      }

      @Test
      void 'should render the backup config object in the config object'() {
        def backupConfig = new BackupConfig()
          .setSchedule("abc")
          .setPostBackupScript("/tmp/post-backup")
          .setEmailOnSuccess(true)
          .setEmailOnFailure(true)
        serverConfig.setBackupConfig(backupConfig)
        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(toObjectString({ BackupConfigRepresenter.toJSON(it, backupConfig) }))
      }
    }

  }

  @Nested
  class Delete {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "deleteBackupConfig"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath())
      }
    }

    @Nested
    class AsAuthorizedUser {
      private ServerConfig serverConfig

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
        serverConfig = new ServerConfig()
        when(goConfigService.serverConfig()).thenReturn(serverConfig)
      }

      @Test
      void 'should render a 404 when a backup config is not present'() {
        deleteWithApiHeader(controller.controllerBasePath())

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Backup config was not found!")
      }

      @Test
      void 'should render a success message after deleting the backup config'() {
        def backupConfig = new BackupConfig()
          .setSchedule("abc")
          .setPostBackupScript("/tmp/post-backup")
          .setEmailOnSuccess(true)
          .setEmailOnFailure(true)
        serverConfig.setBackupConfig(backupConfig)

        deleteWithApiHeader(controller.controllerBasePath())

        verify(goConfigService).updateConfig(isA(DeleteBackupConfigCommand.class), eq(currentUsername()))

        assertThatResponse()
          .isOk()
          .hasJsonMessage(EntityType.BackupConfig.deleteSuccessful())
      }
    }

  }
}
