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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import spark.RequestResponseFactory

import static org.assertj.core.api.AssertionsForClassTypes.assertThat

class ControllerMethodsTest {

  ControllerMethods controllerMethods

  @BeforeEach
  void setUp() {
    this.controllerMethods = new ControllerMethods() {
    }
  }

  @Nested
  class GetIfNoneMatch {

    @Test
    void 'should return etag from request'() {
      def req = HttpRequestBuilder.GET().withHeaders(['if-none-match': 'foo']).build()
      assertThat(controllerMethods.getIfNoneMatch(RequestResponseFactory.create(req))).isEqualTo('foo')
    }

    @Test
    void 'should return null when etag is not present in request'() {
      assertThat(controllerMethods.getIfNoneMatch(RequestResponseFactory.create(new MockHttpServletRequest()))).isNull()
    }

    @Test
    void 'should handle weak etag'() {
      def req = HttpRequestBuilder.GET().withHeaders(['if-none-match': '"foo--deflate"']).build()
      assertThat(controllerMethods.getIfNoneMatch(RequestResponseFactory.create(req))).isEqualTo('foo')

      req = HttpRequestBuilder.GET().withHeaders(['if-none-match': '"foo--gzip"']).build()
      assertThat(controllerMethods.getIfNoneMatch(RequestResponseFactory.create(req))).isEqualTo('foo')
    }

    @Test
    void 'should strip quotes around header value'() {
      def req = HttpRequestBuilder.GET().withHeaders(['if-none-match': '"foo"']).build()
      assertThat(controllerMethods.getIfNoneMatch(RequestResponseFactory.create(req))).isEqualTo('foo')
    }
  }

  @Nested
  class GetIfMatch {

    @Test
    void 'should return etag from request'() {
      def req = HttpRequestBuilder.GET().withHeaders(['if-match': 'foo']).build()
      assertThat(controllerMethods.getIfMatch(RequestResponseFactory.create(req))).isEqualTo('foo')
    }

    @Test
    void 'should return null when etag is not present in request'() {
      assertThat(controllerMethods.getIfMatch(RequestResponseFactory.create(new MockHttpServletRequest()))).isNull()
    }

    @Test
    void 'should handle weak etag'() {
      def req = HttpRequestBuilder.GET().withHeaders(['if-match': '"foo--deflate"']).build()
      assertThat(controllerMethods.getIfMatch(RequestResponseFactory.create(req))).isEqualTo('foo')

      req = HttpRequestBuilder.GET().withHeaders(['if-match': '"foo--gzip"']).build()
      assertThat(controllerMethods.getIfMatch(RequestResponseFactory.create(req))).isEqualTo('foo')
    }


    @Test
    void 'should strip quotes around header value'() {
      def req = HttpRequestBuilder.GET().withHeaders(['if-match': '"foo"']).build()
      assertThat(controllerMethods.getIfMatch(RequestResponseFactory.create(req))).isEqualTo('foo')
    }
  }
}
