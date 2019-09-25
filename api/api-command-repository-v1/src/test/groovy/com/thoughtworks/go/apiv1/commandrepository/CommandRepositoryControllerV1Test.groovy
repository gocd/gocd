/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.commandrepository

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.commandrepository.representer.CommandRepositoryLocationRepresenter
import com.thoughtworks.go.config.CruiseConfig
import com.thoughtworks.go.config.ServerConfig
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException
import com.thoughtworks.go.domain.ConfigErrors
import com.thoughtworks.go.server.service.GoConfigService
import com.thoughtworks.go.server.service.ServerConfigService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import net.sf.ehcache.config.ConfigError
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static java.util.Arrays.asList
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class CommandRepositoryControllerV1Test implements SecurityServiceTrait, ControllerTrait<CommandRepositoryControllerV1> {
  @Mock
  private ServerConfigService serverConfigService;

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  CommandRepositoryControllerV1 createControllerInstance() {
    new CommandRepositoryControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), serverConfigService)
  }

  @Nested
  class Index {

    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath())
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
      void 'should return command repository location'() {
        def commandRepoLocation = "command-repo-location"
        when(serverConfigService.getCommandRepositoryLocation()).thenReturn(commandRepoLocation)
        getWithApiHeader(controller.controllerPath())
        assertThatResponse()
          .isOk()
          .hasBodyWithJson(toObjectString({ CommandRepositoryLocationRepresenter.toJSON(it, commandRepoLocation) }))
      }

    }
  }

  @Nested
  class Update {
    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "update"
      }

      @Override
      void makeHttpCall() {
        putWithApiHeader(controller.controllerPath(), '{}')
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
      void 'should update command-repo location'() {
        def updatedLocation = "updated-location"

        putWithApiHeader(controller.controllerPath(), ["location": updatedLocation])

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(toObjectString({ CommandRepositoryLocationRepresenter.toJSON(it, updatedLocation) }))
      }

      @Test
      void 'should failed to update if command-repo location is invalid'() {
        def updatedLocation = ""
        def cruiseConfig = mock(CruiseConfig.class)
        def errors = new ConfigErrors()
        errors.add(ServerConfig.COMMAND_REPO_LOCATION, "Command Repository Location cannot be empty")
        when(cruiseConfig.getAllErrors()).thenReturn(asList(errors))
        when(serverConfigService.updateCommandRepoLocation(updatedLocation)).thenThrow(new GoConfigInvalidException(cruiseConfig, ""))

        putWithApiHeader(controller.controllerPath(), ["location": updatedLocation])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Command Repository Location cannot be empty")
      }
    }
  }
}
