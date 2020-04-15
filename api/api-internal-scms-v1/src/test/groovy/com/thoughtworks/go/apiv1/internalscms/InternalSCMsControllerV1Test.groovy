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

package com.thoughtworks.go.apiv1.internalscms

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.internalscms.representers.SCMRepresenter
import com.thoughtworks.go.apiv1.internalscms.representers.VerifyConnectionResultRepresenter
import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.domain.scm.SCM
import com.thoughtworks.go.domain.scm.SCMMother
import com.thoughtworks.go.plugin.api.response.Result
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.materials.PluggableScmService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.ArgumentMatchers.any
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalSCMsControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalSCMsControllerV1> {
  @Mock
  PluggableScmService pluggableScmService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalSCMsControllerV1 createControllerInstance() {
    new InternalSCMsControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), pluggableScmService)
  }

  @Nested
  class CheckConnection {

    @BeforeEach
    void setUp() {
      loginAsGroupAdmin()
    }

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "verifyConnection"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader("/api/admin/internal/scms/verify_connection", [:])
      }
    }

    @Test
    void 'should return 200 if check connection is ok'() {
      SCM scm = SCMMother.create("", "foobar", "plugin1", "v1.0", new Configuration(
        ConfigurationPropertyMother.create("key1", false, "value1")
      ))
      Result result = new Result().withSuccessMessages("message 1", "message 2")

      when(pluggableScmService.checkConnection(any(SCM))).thenReturn(result)

      def postJson = toObjectString({ SCMRepresenter.toJSON(it, scm) })

      postWithApiHeader("/api/admin/internal/scms/verify_connection", postJson)

      assertThatResponse()
        .isOk()
        .hasBodyWithJsonObject(VerifyConnectionResultRepresenter.class, scm, result)
    }
  }
}
