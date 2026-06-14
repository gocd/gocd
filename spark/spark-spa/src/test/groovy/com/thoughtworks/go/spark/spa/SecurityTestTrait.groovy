/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.spark.spa

import com.thoughtworks.go.api.mocks.MockHttpServletResponseAssert
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.SecurityTestTraitBasics
import groovy.transform.SelfType
import org.junit.jupiter.api.BeforeEach
import org.mockito.invocation.InvocationOnMock
import spark.ModelAndView
import spark.Response

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.*

@SelfType([SecurityServiceTrait, ControllerTrait])
trait SecurityTestTrait implements SecurityTestTraitBasics {

  def reachedControllerMessage

  @BeforeEach
  void stubControllerAction() {
    this.reachedControllerMessage = "REACHED_CONTROLLER_${UUID.randomUUID()}".toString()
    doAnswer({ InvocationOnMock invocation ->
      Response res = invocation.getArgument(1)
      res.status(99999)
      return new ModelAndView([message: reachedControllerMessage], null)
    }).when(controller)."${controllerMethodUnderTest}"(any(), any())
  }

  abstract String getControllerMethodUnderTest()


  @Override
  def assertRequestAllowed() {
    ((MockHttpServletResponseAssert) assertThatResponse())
      .hasStatus(99999)
      .hasContentType("text/html")
      .hasBody("rendered template with locals {\"message\":\"${reachedControllerMessage}\"}")

    verify(controller)."${controllerMethodUnderTest}"(any(), any())
  }

  @Override
  def assertRequestForbidden() {
    ((MockHttpServletResponseAssert) assertThatResponse())
      .hasContentType("text/html")
      .hasStatus(403)

    verify(controller, never())."${controllerMethodUnderTest}"(any(), any())
  }
}
