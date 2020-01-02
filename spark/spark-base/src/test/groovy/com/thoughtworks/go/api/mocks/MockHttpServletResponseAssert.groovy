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
package com.thoughtworks.go.api.mocks

import com.thoughtworks.go.http.mocks.MockHttpServletResponse
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert

import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString

class MockHttpServletResponseAssert extends com.thoughtworks.go.http.mocks.MockHttpServletResponseAssert<MockHttpServletResponseAssert> {

  private MockHttpServletResponseAssert(MockHttpServletResponse actual, Class<?> selfType) {
    super(actual, selfType)
  }

  static MockHttpServletResponseAssert assertThat(MockHttpServletResponse actual) {
    return new MockHttpServletResponseAssert(actual, MockHttpServletResponseAssert.class)
  }

  MockHttpServletResponseAssert hasBodyWithJson(String expectedJson) {
    JsonFluentAssert.assertThatJson(actual.getContentAsString()).isEqualTo(expectedJson)
    return this
  }

  /**
   * Use {@link #hasBodyWithJsonObject(java.lang.Class, java.lang.Object [])} instead
   */
  @Deprecated
  MockHttpServletResponseAssert hasBodyWithJsonObject(Object expected, Class representer) throws UnsupportedEncodingException {
    return hasBodyWithJsonObject(representer, expected)
  }

  MockHttpServletResponseAssert hasBodyWithJsonObject(Class representer, Object... representerArgs) throws UnsupportedEncodingException {
    def expectedJson = toObjectString({ representer.toJSON(it, *representerArgs) })

    JsonFluentAssert.assertThatJson(actual.getContentAsString()).isEqualTo(expectedJson)
    return this
  }

  /**
   * Use {@link #hasBodyWithJsonArray(java.lang.Class, java.lang.Object [])} instead
   */
  @Deprecated
  MockHttpServletResponseAssert hasBodyWithJsonArray(Object expected, Class representer) throws UnsupportedEncodingException {
    return hasBodyWithJsonArray(representer, expected)
  }

  MockHttpServletResponseAssert hasBodyWithJsonArray(Class representer, Object... representerArgs) throws UnsupportedEncodingException {
    def expectedJson = toArrayString({ representer.toJSON(it, *representerArgs) })

    JsonFluentAssert.assertThatJson(actual.getContentAsString()).isEqualTo(expectedJson)
    return this
  }

}
