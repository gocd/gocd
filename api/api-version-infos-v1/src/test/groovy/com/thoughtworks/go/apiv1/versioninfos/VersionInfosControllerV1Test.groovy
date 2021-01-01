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

package com.thoughtworks.go.apiv1.versioninfos

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.versioninfos.representers.VersionInfoRepresenter
import com.thoughtworks.go.domain.GoVersion
import com.thoughtworks.go.domain.VersionInfo
import com.thoughtworks.go.server.service.VersionInfoService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.util.SystemEnvironment
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class VersionInfosControllerV1Test implements SecurityServiceTrait, ControllerTrait<VersionInfosControllerV1> {
  @Mock
  private VersionInfoService versionInfoService
  @Mock
  private SystemEnvironment systemEnvironment

  @BeforeEach
  void setUp() {
    initMocks(this)
    when(systemEnvironment.getUpdateServerUrl()).thenReturn("https://update.example.com/some/path")
  }

  @Override
  VersionInfosControllerV1 createControllerInstance() {
    new VersionInfosControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), versionInfoService, systemEnvironment)
  }

  @Nested
  class Stale {

    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "stale"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath() + "/stale")
      }
    }

    @Test
    void 'should render stale version info'() {
      def info = new VersionInfo("go_server", new GoVersion('1.2.3-1'), new GoVersion('5.6.7-1'), null)

      when(systemEnvironment.getUpdateServerUrl()).thenReturn("https://update.example.com/some/path")
      when(versionInfoService.getStaleVersionInfo()).thenReturn(info)

      getWithApiHeader(controller.controllerBasePath() + "/stale")

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(VersionInfoRepresenter.class, info, systemEnvironment)
    }

    @Test
    void 'should return empty response when there are no stale version infos'() {
      when(versionInfoService.getStaleVersionInfo()).thenReturn(null)

      getWithApiHeader(controller.controllerBasePath() + "/stale")

      assertThatResponse()
        .isOk()
        .hasBody("{ }")
    }
  }

  @Nested
  class LatestVersion {

    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "latestVersion"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath() + "/latest_version")
      }
    }

    @Test
    void 'should render latest version'() {
      when(versionInfoService.getGoUpdate()).thenReturn("2.4.6.2")

      getWithApiHeader(controller.controllerBasePath() + "/latest_version")

      assertThatResponse()
        .isOk()
        .hasJsonBody([latest_version: "2.4.6.2"])
    }

    @Test
    void 'should return empty response when there is no info available'() {
      getWithApiHeader(controller.controllerBasePath() + "/latest_version")

      assertThatResponse()
        .isOk()
        .hasBody("{ }")
    }
  }

  @Nested
  class PatchGoServer {

    @BeforeEach
    void setUp() {
      loginAsUser()
    }

    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "updateGoServerInfo"
      }

      @Override
      void makeHttpCall() {
        patchWithApiHeader(controller.controllerBasePath() + "/go_server", [:])
      }
    }
  }
}
