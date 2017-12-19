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

package com.thoughtworks.go.api

import cd.go.jrepresenter.RequestContext
import cd.go.jrepresenter.TestRequestContext
import com.thoughtworks.go.api.mocks.*
import com.thoughtworks.go.i18n.Localizer
import org.junit.jupiter.api.BeforeEach
import org.mockito.invocation.InvocationOnMock
import spark.servlet.SparkFilter

import javax.servlet.FilterConfig

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyVararg
import static org.mockito.Mockito.*

trait ControllerTrait<T extends SparkController> {

  FilterConfig filterConfig
  private T controller
  TestSparkPreFilter testSparkPreFilter
  MockHttpServletRequest request
  MockHttpServletResponse response
  RequestContext requestContext = new TestRequestContext();
  Localizer localizer = mock(Localizer.class)

  void get(String path) {
    sendRequest('get', path, [:], null)
  }

  void get(String path, Map headers) {
    sendRequest('get', path, headers, null)
  }

  void put(String path, Object body) {
    sendRequest('put', path, ['content-type': 'application/json'], body)
  }

  void post(String path, Object body) {
    sendRequest('post', path, ['content-type': 'application/json'], body)
  }

  void patch(String path, Object body) {
    sendRequest('patch', path, ['content-type': 'application/json'], body)
  }

  void delete(String path) {
    sendRequest('delete', path, [:], null)
  }

  void getWithApiHeader(String path) {
    getWithApiHeader(path, [:])
  }

  void getWithApiHeader(String path, Map headers) {
    sendRequest('get', path, headers + ['accept': controller.mimeType], null)
  }

  void putWithApiHeader(String path, Object body) {
    putWithApiHeader(path, [:], body)
  }

  void putWithApiHeader(String path, Map headers, Object body) {
    sendRequest('put', path, headers + ['accept': controller.mimeType, 'content-type': 'application/json'], body)
  }

  void postWithApiHeader(String path, Object body) {
    postWithApiHeader(path, [:], body)
  }

  void postWithApiHeader(String path, Map headers, Object body) {
    sendRequest('post', path, headers + ['accept': controller.mimeType, 'content-type': 'application/json'], body)
  }

  void patchWithApiHeader(String path, Object body) {
    patchWithApiHeader(path, [:], body)
  }

  void patchWithApiHeader(String path, Map headers, Object body) {
    sendRequest('patch', path, headers + ['accept': controller.mimeType, 'content-type': 'application/json'], body)
  }

  void deleteWithApiHeader(String path) {
    deleteWithApiHeader(path, [:])
  }

  void deleteWithApiHeader(String path, Map headers) {
    sendRequest('delete', path, headers + ['accept': controller.mimeType], null)
  }

  void sendRequest(String httpVerb, String path, Map<String, String> headers, Object requestBody) {
    filterConfig = mock(FilterConfig.class)

    when(filterConfig.getInitParameter(SparkFilter.APPLICATION_CLASS_PARAM)).thenReturn(TestApplication.class.getName())
    testSparkPreFilter = new TestSparkPreFilter(new TestApplication(controller))
    testSparkPreFilter.init(filterConfig)

    HttpRequestBuilder builder = HttpRequestBuilder."${httpVerb.toUpperCase()}"(path).withHeaders(headers)

    if (requestBody != null) {
      if (requestBody instanceof String) {
        builder.withJsonBody((String) requestBody)
      } else {
        builder.withJsonBody((Object) requestBody)
      }
    }

    request = builder.build()

    response = new MockHttpServletResponse()

    try {
      testSparkPreFilter.doFilter(request, response, null)
    } finally {
      testSparkPreFilter.destroy()
    }
  }

  T getController() {
    if (controller == null) {
      controller = spy(createControllerInstance())
    }
    return controller
  }

  MockHttpServletResponseAssert assertThatResponse() {
    MockHttpServletResponseAssert.assertThat(response)
  }

  abstract T createControllerInstance()

  @BeforeEach
  void setupLocalizer() {
    when(localizer.localize(any() as String, anyVararg())).then({ InvocationOnMock invocation ->
      return invocation.getArguments().first()
    })
  }
}
