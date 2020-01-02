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
package com.thoughtworks.go.spark

import com.thoughtworks.go.api.mocks.MockHttpServletResponseAssert
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.http.mocks.HttpRequestBuilder
import com.thoughtworks.go.http.mocks.MockHttpServletRequest
import com.thoughtworks.go.http.mocks.MockHttpServletResponse
import com.thoughtworks.go.http.mocks.MockHttpSession
import com.thoughtworks.go.server.cache.GoCache
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.newsecurity.models.AnonymousCredential
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils
import com.thoughtworks.go.spark.mocks.StubTemplateEngine
import com.thoughtworks.go.spark.mocks.TestApplication
import com.thoughtworks.go.spark.mocks.TestRequestContext
import com.thoughtworks.go.spark.mocks.TestSparkPreFilter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import spark.servlet.SparkFilter

import javax.servlet.Filter
import javax.servlet.FilterConfig
import javax.servlet.http.Part

import static org.mockito.Mockito.*

trait ControllerTrait<T extends SparkController> {

  private T _controller
  Filter prefilter
  MockHttpServletRequest request
  MockHttpServletResponse response
  RequestContext requestContext = new TestRequestContext()
  StubTemplateEngine templateEngine = new StubTemplateEngine()
  GoCache goCache = mock(GoCache.class)
  MockHttpSession session = new MockHttpSession()
  HttpRequestBuilder httpRequestBuilder = new HttpRequestBuilder().withSession(session)
  Part part = mock(Part)
  Collection<Part> parts = Collections.singletonList(part)

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

  void deleteWithApiHeader(String path, Object body) {
    deleteWithApiHeader(path, [:], body)
  }

  void deleteWithApiHeader(String path, Map headers, Object body) {
    sendRequest('delete', path, headers + ['accept': controller.mimeType], body)
  }

  void sendRequest(String httpVerb, String path, Map<String, String> headers, Object requestBody) {
    httpRequestBuilder.withPath(path).withMethod(httpVerb).withHeaders(headers)

    if (requestBody != null) {
      if (requestBody instanceof String) {
        httpRequestBuilder.withJsonBody((String) requestBody)
      } else {
        httpRequestBuilder.withJsonBody((Object) requestBody)
      }
    }
    AuthenticationToken<?> newToken
    if (currentUsername().isAnonymous()) {
      newToken = new AuthenticationToken<>(SessionUtils.getCurrentUser(), AnonymousCredential.INSTANCE, null, 0, null)
      httpRequestBuilder.withSessionAttr(SessionUtils.CURRENT_USER_ID, -1L)
    } else {
      newToken = new AuthenticationToken<>(SessionUtils.getCurrentUser(), new UsernamePassword(currentUsernameString(), "password"), "plugin1", System.currentTimeMillis(), "authConfigId")

      httpRequestBuilder.withSessionAttr(SessionUtils.CURRENT_USER_ID, currentUserLoginId())

    }
    httpRequestBuilder.withSessionAttr(SessionUtils.AUTHENTICATION_TOKEN, newToken)

    request = httpRequestBuilder.build()
    request.setParts(parts)

    response = new MockHttpServletResponse()

    getPrefilter().doFilter(request, response, null)
  }

  void mockMultipartContent(String name, String filename, String content) {
    when(part.getName()).thenReturn(name)
    when(part.getSubmittedFileName()).thenReturn(filename)
    when(part.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()))
  }

  private Filter getPrefilter() {
    if (prefilter == null) {
      def filterConfig = mock(FilterConfig.class)
      when(filterConfig.getInitParameter(SparkFilter.APPLICATION_CLASS_PARAM)).thenReturn(TestApplication.class.getName())
      prefilter = new TestSparkPreFilter(new TestApplication(controller))
      prefilter.init(filterConfig)
    }
    return prefilter
  }

  T getController() {
    if (_controller == null) {
      _controller = spy(createControllerInstance())
    }
    return _controller
  }

  MockHttpServletResponseAssert assertThatResponse() {
    MockHttpServletResponseAssert.assertThat(response)
  }

  abstract T createControllerInstance()

  @BeforeEach
  @AfterEach
  void clearSingletons() {
    ClearSingleton.clearSingletons()
  }

  @AfterEach
  void destroyPrefilter() {
    getPrefilter().destroy()
  }

  Username currentUsername() {
    return SessionUtils.currentUsername()
  }

  String currentUsernameString() {
    return currentUserLoginName().toString()
  }

  CaseInsensitiveString currentUserLoginName() {
    return currentUsername().getUsername()
  }

  Long currentUserLoginId() {
    if (currentUsername().isAnonymous()) {
      return -1L
    }

    currentUsername().hashCode()
  }
}
