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
package com.thoughtworks.go.api.cctray

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.mocks.MockHttpServletResponseAssert
import com.thoughtworks.go.server.service.CcTrayService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import java.util.function.Consumer

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class CctrayControllerTest implements SecurityServiceTrait, ControllerTrait<CctrayController> {
  @Mock
  private CcTrayService ccTrayService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  CctrayController createControllerInstance() {
    new CctrayController(securityService, ccTrayService)
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
        get("/cctray.xml")
      }

      @Override
      def assertRequestForbidden() {
        verify(controller, never())."${controllerMethodUnderTest}"(any(), any())

        ((MockHttpServletResponseAssert) assertThatResponse())
          .hasContentType(controller.mimeType)
          .hasStatus(403)
          .hasBody("<access-denied>\n" +
          "  <message>You are not authenticated!</message>\n" +
          "</access-denied>")
      }

    }

    @Nested
    class AsAuthorizedUser {
      @Test
      void 'should render XML returned by cctray service'() {
        enableSecurity()
        loginAsUser()
        when(ccTrayService.renderCCTrayXML(eq("http://test.host/go"), eq(currentUsernameString()), any() as Appendable, any() as Consumer<String>)).thenAnswer({ InvocationOnMock invocation ->
          Appendable appendable = invocation.getArgument(2)
          Consumer<String> etag = invocation.getArgument(3)
          etag.accept("some-etag")

          appendable.append("blah!")
        })

        get("/cctray.xml")

        assertThatResponse()
          .isOk()
          .hasEtag('"some-etag"')
          .hasContentType("application/xml")
          .hasBody("blah!")
      }
    }
  }
}
