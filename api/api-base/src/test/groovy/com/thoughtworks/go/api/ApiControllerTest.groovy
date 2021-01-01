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
package com.thoughtworks.go.api

import com.thoughtworks.go.api.mocks.MockHttpServletResponseAssert
import com.thoughtworks.go.api.util.HaltApiMessages
import com.thoughtworks.go.api.util.MessageJson
import com.thoughtworks.go.http.mocks.HttpRequestBuilder
import com.thoughtworks.go.http.mocks.MockHttpServletRequest
import com.thoughtworks.go.http.mocks.MockHttpServletResponse
import com.thoughtworks.go.spark.util.SecureRandom
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import spark.HaltException
import spark.Request
import spark.RequestResponseFactory

import static com.thoughtworks.go.api.util.HaltApiMessages.jsonContentTypeExpected
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode

class ApiControllerTest {

  ApiController baseController

  @BeforeEach
  void setUp() {
    this.baseController = new ApiController(ApiVersion.v1) {

      @Override
      String controllerBasePath() {
        return null
      }

      @Override
      void setupRoutes() {

      }
    }
  }

  @Nested
  class verifyContentType {

    @Test
    void 'should not blow up on get and head requests'() {
      ['get', 'head'].each { method ->
        def response = new MockHttpServletResponse()
        assertThatCode({
          baseController.verifyContentType(
            RequestResponseFactory.create(HttpRequestBuilder."${method.toUpperCase()}"().build()) as Request,
            RequestResponseFactory.create(response)
          )
        }).as("${method} should not blow up").doesNotThrowAnyException()
        MockHttpServletResponseAssert.assertThat(response)
          .as("${method} response should be ok")
          .isOk()
      }
    }

    @Test
    void 'should not blow up if empty put,post,patch requests do not have a content type and contain an confirm header'() {
      ['put', 'post', 'patch'].each { method ->
        def response = new MockHttpServletResponse()
        assertThatCode({
          baseController.verifyContentType(
            RequestResponseFactory.create(HttpRequestBuilder."${method.toUpperCase()}"().withHeaders(['X-GoCD-Confirm': SecureRandom.hex()]).build()) as Request,
            RequestResponseFactory.create(response)
          )
        })
          .as("${method} should not blow up with empty body and confirm header present")
          .doesNotThrowAnyException()
        MockHttpServletResponseAssert.assertThat(response).isOk().as("${method} response should be ok with empty body")
      }
    }

    @Test
    void 'should blow up if empty put,post,patch requests do not have a content type or a confirm header'() {
      ['put', 'post', 'patch'].each { method ->
        def response = new MockHttpServletResponse()
        assertThatCode({
          baseController.verifyContentType(
            RequestResponseFactory.create(HttpRequestBuilder."${method.toUpperCase()}"().build()) as Request,
            RequestResponseFactory.create(response)
          )
        })
          .as("${method} should blow up")
          .isInstanceOf(HaltException)
          .hasFieldOrPropertyWithValue("statusCode", 400)
          .hasFieldOrPropertyWithValue("body", MessageJson.create(HaltApiMessages.confirmHeaderMissing()))
      }
    }

    @Test
    void 'should not blow up if non-chunked put,post,patch requests has a body with json content type'() {
      ['put', 'post', 'patch'].each { method ->
        def response = new MockHttpServletResponse()
        assertThatCode({
          MockHttpServletRequest request = HttpRequestBuilder
            ."${method.toUpperCase()}"()
            .withBody("foo")
            .withHeaders(['content-type': 'application/json'])
            .build()

          baseController.verifyContentType(
            RequestResponseFactory.create(request),
            RequestResponseFactory.create(response)
          )
        }).as("${method} should not blow up with empty body").doesNotThrowAnyException()
        MockHttpServletResponseAssert.assertThat(response).isOk().as("${method} response should be ok with empty body")
      }
    }

    @Test
    void 'should blow up put,post,patch requests with a request body do not have a content type'() {
      ['put', 'post', 'patch'].each { method ->
        assertThatCode({
          MockHttpServletRequest request = HttpRequestBuilder.
          "${method.toUpperCase()}"()
            .withBody('foo')
            .build()

          baseController.verifyContentType(
            RequestResponseFactory.create(request),
            null
          )
        })
          .as("${method} should not blow up")
          .isInstanceOf(HaltException)
          .hasFieldOrPropertyWithValue("statusCode", 415)
          .hasFieldOrPropertyWithValue("body", MessageJson.create(jsonContentTypeExpected()))
      }
    }

    @Test
    void 'should blow up if chunked put,post,patch requests has a body with no content type'() {
      ['put', 'post', 'patch'].each { method ->
        def response = new MockHttpServletResponse()
        assertThatCode({
          MockHttpServletRequest request = HttpRequestBuilder
            ."${method.toUpperCase()}"()
            .withChunkedBody("foo")
            .build()

          baseController.verifyContentType(
            RequestResponseFactory.create(request),
            RequestResponseFactory.create(response)
          )
        })
          .as("${method} should blow up")
          .isInstanceOf(HaltException)
          .hasFieldOrPropertyWithValue("statusCode", 415)
          .hasFieldOrPropertyWithValue("body", MessageJson.create(jsonContentTypeExpected()))
      }
    }

    @Test
    void 'should not blow up if chunked put,post,patch requests has a body with json content type'() {
      ['put', 'post', 'patch'].each { method ->
        def response = new MockHttpServletResponse()
        assertThatCode({
          MockHttpServletRequest request = HttpRequestBuilder
            ."${method.toUpperCase()}"()
            .withChunkedBody("foo")
            .withHeaders(['content-type': 'application/json'])
            .build()

          baseController.verifyContentType(
            RequestResponseFactory.create(request),
            RequestResponseFactory.create(response)
          )
        }).as("${method} should not blow up with empty body").doesNotThrowAnyException()
        MockHttpServletResponseAssert.assertThat(response)
          .as("${method} response should be ok with empty body")
          .isOk()
      }
    }
  }

}
