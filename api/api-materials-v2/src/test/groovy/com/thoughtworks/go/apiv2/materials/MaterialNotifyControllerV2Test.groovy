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
package com.thoughtworks.go.apiv2.materials

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.server.materials.MaterialUpdateService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.verify
import static org.mockito.MockitoAnnotations.initMocks

class MaterialNotifyControllerV2Test implements SecurityServiceTrait, ControllerTrait<MaterialNotifyControllerV2> {

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Mock
  private MaterialUpdateService materialUpdateService

  @Override
  MaterialNotifyControllerV2 createControllerInstance() {
    return new MaterialNotifyControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), materialUpdateService)
  }

  @Nested
  class Svn {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "svnNotify"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath() + Routes.MaterialNotify.SVN, [])
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
      void 'should notify SVN material with repository_url'() {
        def payload = [repository_url: "ssh+svn://smiling-red-panda"]

        postWithApiHeader(Routes.MaterialNotify.BASE + Routes.MaterialNotify.SVN, payload)

        def expectedParams = [(MaterialUpdateService.TYPE): "svn", repository_url: "ssh+svn://smiling-red-panda"]
        verify(materialUpdateService).notifyMaterialsForUpdate(
          eq(currentUsername()), eq(expectedParams), any(HttpLocalizedOperationResult.class))
      }

      @Test
      void 'should notify SVN material with UUID'() {
        def payload = [uuid: "some-uuid-value"]

        postWithApiHeader(Routes.MaterialNotify.BASE + Routes.MaterialNotify.SVN, payload)

        def expectedParams = [(MaterialUpdateService.TYPE): "svn", uuid: "some-uuid-value"]
        verify(materialUpdateService).notifyMaterialsForUpdate(
          eq(currentUsername()), eq(expectedParams), any(HttpLocalizedOperationResult.class))
      }

      @Test
      void 'should return 422 when repository_url or uuid is not found for git'() {
        def payload = [foo: "bar"]

        postWithApiHeader(Routes.MaterialNotify.BASE + Routes.MaterialNotify.SVN, payload)

        assertThatResponse().isUnprocessableEntity()
      }
    }
  }

  @Nested
  class Git {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "gitNotify"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath() + Routes.MaterialNotify.GIT, [])
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
      void 'should notify Git material'() {
        def payload = [repository_url: "http://nonstop-moose/"]

        postWithApiHeader(Routes.MaterialNotify.BASE+ Routes.MaterialNotify.GIT, payload)

        def expectedParams = [(MaterialUpdateService.TYPE): "git", repository_url: "http://nonstop-moose/"]
        verify(materialUpdateService).notifyMaterialsForUpdate(
          eq(currentUsername()), eq(expectedParams), any(HttpLocalizedOperationResult.class))
      }

      @Test
      void 'should return 422 when repository_url is not found for git'() {
        def payload = [foo: "bar"]

        postWithApiHeader(Routes.MaterialNotify.BASE + Routes.MaterialNotify.GIT, payload)

        assertThatResponse().isUnprocessableEntity()
      }
    }
  }

  @Nested
  class Hg {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "hgNotify"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath() + Routes.MaterialNotify.HG, [])
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
      void 'should notify Hg material'() {
        def payload = [repository_url: "http://psychotic-sumatran-rhinoceros/"]

        postWithApiHeader(Routes.MaterialNotify.BASE + Routes.MaterialNotify.HG, payload)

        def expectedParams = [(MaterialUpdateService.TYPE): "hg", repository_url: "http://psychotic-sumatran-rhinoceros/"]
        verify(materialUpdateService).notifyMaterialsForUpdate(
          eq(currentUsername()), eq(expectedParams), any(HttpLocalizedOperationResult.class))
      }

      @Test
      void 'should return 422 when repository_url is not found for hg'() {
        def payload = [foo: "bar"]

        postWithApiHeader(Routes.MaterialNotify.BASE + Routes.MaterialNotify.HG, payload)

        assertThatResponse().isUnprocessableEntity()
      }
    }
  }

  @Nested
  class Scm {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "scmNotify"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath() + Routes.MaterialNotify.SCM, [])
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
      void 'should notify other SCM material'() {
        def payload = [scm_name: "pale-lionfish"]

        postWithApiHeader(Routes.MaterialNotify.BASE + Routes.MaterialNotify.SCM, payload)

        def expectedParams = [(MaterialUpdateService.TYPE): "scm", scm_name: "pale-lionfish"]
        verify(materialUpdateService).notifyMaterialsForUpdate(
          eq(currentUsername()), eq(expectedParams), any(HttpLocalizedOperationResult.class))
      }

      @Test
      void 'should return 422 when scm_name is not found for other scm'() {
        def payload = [foo: "bar"]

        postWithApiHeader(Routes.MaterialNotify.BASE + Routes.MaterialNotify.SCM, payload)

        assertThatResponse().isUnprocessableEntity()
      }
    }
  }
}
