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

package com.thoughtworks.go.api.mocks

import cd.go.jrepresenter.TestRequestContext
import com.thoughtworks.go.server.api.HaltMessages
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions

import javax.activation.MimeType
import javax.activation.MimeTypeParseException

import static org.apache.commons.lang.StringUtils.isNotBlank

class MockHttpServletResponseAssert extends AbstractObjectAssert<MockHttpServletResponseAssert, MockHttpServletResponse> {

  MockHttpServletResponseAssert(MockHttpServletResponse actual) {
    super(actual, MockHttpServletResponseAssert.class)
  }

  static MockHttpServletResponseAssert assertThat(MockHttpServletResponse actual) {
    return new MockHttpServletResponseAssert(actual)
  }

  MockHttpServletResponseAssert hasStatusWithMessage(int statusCode, String message) {
    return hasStatus(statusCode).hasJsonMessage(message)
  }

  MockHttpServletResponseAssert hasStatus(int statusCode) {
    if (actual.getStatus() != statusCode) {
      failWithMessage("Expected status code <%s> but was <%s>", statusCode, actual.getStatus())
    }
    return this
  }

  MockHttpServletResponseAssert hasContentType(String mimeType) {
    String contentType = actual.getHeader("content-type")
    try {
      if (!(isNotBlank(contentType) && new MimeType(contentType).match(mimeType))) {
        failWithMessage("Expected content type <%s> but was <%s>", mimeType, contentType)
      }
    } catch (MimeTypeParseException e) {
      failWithMessage("Actual content type <%s> could not be parsed", contentType)
    }
    return this
  }

  MockHttpServletResponseAssert hasJsonBody(Object expected) throws UnsupportedEncodingException {
    JsonFluentAssert.assertThatJson(actual.getContentAsString()).isEqualTo(expected)
    return this
  }

  MockHttpServletResponseAssert hasJsonBodySerializedWith(Object expected, Class mapper) throws UnsupportedEncodingException {
    JsonFluentAssert.assertThatJson(actual.getContentAsString()).isEqualTo(mapper.toJSON(expected, new TestRequestContext()))
    return this
  }


  MockHttpServletResponseAssert hasJsonAtrribute(String attribute, Object object) {
    JsonFluentAssert.assertThatJson(actual.getContentAsString()).node(attribute).isEqualTo(object)
    return this
  }


  MockHttpServletResponseAssert hasJsonMessage(String message) throws UnsupportedEncodingException {
    JsonFluentAssert.assertThatJson(actual.getContentAsString()).node("message").isEqualTo(message)
    return this
  }

  MockHttpServletResponseAssert hasEtag(String expectedHeader) {
    String actualHeader = actual.getHeader("etag")
    if (!expectedHeader.equals(actualHeader)) {
      failWithMessage("Expected etag <%s> but was <%s>", expectedHeader, actualHeader)
    }
    return this
  }

  MockHttpServletResponseAssert isOk() {
    return hasStatus(200)
  }

  MockHttpServletResponseAssert isNotModified() {
    return hasStatus(304).hasEtag(null).hasNoBody()
  }

  MockHttpServletResponseAssert hasNoBody() {
    Assertions.assertThat(actual.contentAsString).isEmpty()
    return this
  }

  MockHttpServletResponseAssert isNotFound() {
    return hasStatus(404).hasJsonMessage(HaltMessages.notFoundMessage())
  }

  MockHttpServletResponseAssert isUnprocessibleEntity() {
    return hasStatus(422)
  }

  MockHttpServletResponseAssert preConditionFailed() {
    return hasStatus(412)
  }

  MockHttpServletResponseAssert isBadRequest() {
    return hasStatus(400)
  }

  MockHttpServletResponseAssert isUnsupportedMediaType() {
    return hasStatus(415)

  }
}
