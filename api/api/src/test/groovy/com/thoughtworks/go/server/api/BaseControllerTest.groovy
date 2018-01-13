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

package com.thoughtworks.go.server.api

import com.thoughtworks.go.api.mocks.MockHttpServletRequest
import com.thoughtworks.go.api.mocks.MockHttpServletResponse
import com.thoughtworks.go.api.mocks.MockHttpServletResponseAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import spark.HaltException
import spark.Request
import spark.RequestResponseFactory

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode

class BaseControllerTest {

  BaseController baseController

  @BeforeEach
  void setUp() {
    this.baseController = new BaseController(ApiVersion.v1) {

      @Override
      protected String controllerBasePath() {
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
        MockHttpServletResponseAssert.assertThat(response).isOk().as("${method} response should be ok")
      }
    }

    @Test
    void 'should not blow up if empty put/post/patch requests do not have a content type'() {
      ['put', 'post', 'patch'].each { method ->
        def response = new MockHttpServletResponse()
        assertThatCode({
          baseController.verifyContentType(
            RequestResponseFactory.create(HttpRequestBuilder."${method.toUpperCase()}"().build()) as Request,
            RequestResponseFactory.create(response)
          )
        }).as("${method} should not blow up with empty body").doesNotThrowAnyException()
        MockHttpServletResponseAssert.assertThat(response).isOk().as("${method} response should be ok with empty body")
      }
    }

    @Test
    void 'should not blow up if non-chunked put/post/patch requests has a body with json content type'() {
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
    void 'should blow up put/post/patch requests with a request body do not have a content type'() {
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
        }).isInstanceOf(HaltException)
          .hasFieldOrPropertyWithValue("statusCode", 415)
          .hasFieldOrPropertyWithValue("body", MessageJson.create("You must specify a 'Content-Type' of 'application/json'"))
          .as("${method} should not blow up")
      }
    }


    @Test
    void 'should blow up if chunked put/post/patch requests has a body with no content type'() {
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
        }).isInstanceOf(HaltException)
          .hasFieldOrPropertyWithValue("statusCode", 415)
          .hasFieldOrPropertyWithValue("body", MessageJson.create("You must specify a 'Content-Type' of 'application/json'"))
          .as("${method} should not blow up")
      }
    }


    @Test
    void 'should not blow up if chunked put/post/patch requests has a body with json content type'() {
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
        MockHttpServletResponseAssert.assertThat(response).isOk().as("${method} response should be ok with empty body")
      }
    }
  }

}
