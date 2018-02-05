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

import com.google.gson.*
import com.google.gson.internal.bind.util.ISO8601Utils
import com.thoughtworks.go.spark.mocks.MockHttpServletResponse
import com.thoughtworks.go.spark.mocks.TestRequestContext
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert
import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.ReflectionToStringBuilder
import org.apache.commons.lang.builder.ToStringStyle
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.internal.Failures
import org.assertj.core.util.Arrays
import org.assertj.core.util.Objects

import javax.activation.MimeType
import javax.activation.MimeTypeParseException
import javax.servlet.http.Cookie
import java.lang.reflect.Type

import static org.apache.commons.lang.StringUtils.isNotBlank
import static org.assertj.core.error.ShouldBeEqual.shouldBeEqual
import static org.assertj.core.error.ShouldBeNullOrEmpty.shouldBeNullOrEmpty

class MockHttpServletResponseAssert extends AbstractObjectAssert<MockHttpServletResponseAssert, MockHttpServletResponse> {
  private static final TimeZone UTC = TimeZone.getTimeZone("UTC")

  static private JsonSerializer<Date> dateSerializer = new JsonSerializer<Date>() {
    @Override
    JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
      return src == null ? JsonNull.INSTANCE : new JsonPrimitive(ISO8601Utils.format(src, false, UTC))
    }
  }

  private static final Gson GSON = new GsonBuilder()
    .serializeNulls()
    .setPrettyPrinting()
    .excludeFieldsWithoutExposeAnnotation()
    .disableHtmlEscaping()
    .registerTypeAdapter(Date.class, dateSerializer)
    .create()

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

  MockHttpServletResponseAssert hasJsonBodySerializedWith(Object expected, Class representer) throws UnsupportedEncodingException {
    JsonFluentAssert.assertThatJson(actual.getContentAsString()).isEqualTo(GSON.toJson(representer.toJSON(expected, new TestRequestContext())))
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
    return hasHeader("etag", expectedHeader)
  }

  MockHttpServletResponseAssert isOk() {
    return hasStatus(200)
  }

  MockHttpServletResponseAssert isPreconditionFailed() {
    return hasStatus(412)
  }

  MockHttpServletResponseAssert isNotModified() {
    return hasStatus(304).hasEtag(null).hasNoBody()
  }

  MockHttpServletResponseAssert hasNoBody() {
    if (!Arrays.isNullOrEmpty(actual.getContentAsByteArray())) {
      throw Failures.instance().failure(info, shouldBeNullOrEmpty(actual.getContentAsByteArray()))
    }
    return this
  }

  MockHttpServletResponseAssert isNotFound() {
    return hasStatus(404)
  }

  MockHttpServletResponseAssert isTooManyRequests() {
    return hasStatus(429)
  }

  MockHttpServletResponseAssert isUnprocessibleEntity() {
    return hasStatus(422)
  }

  MockHttpServletResponseAssert isBadRequest() {
    return hasStatus(400)
  }

  MockHttpServletResponseAssert isUnsupportedMediaType() {
    return hasStatus(415)

  }

  MockHttpServletResponseAssert hasCacheControl(String headerValue) {
    return hasHeader('Cache-Control', headerValue)
  }

  MockHttpServletResponseAssert hasHeader(String headerName, String expectedHeader) {
    String actualHeader = actual.getHeader(headerName)

    if (!Objects.areEqual(actualHeader, expectedHeader)) {
      failWithMessage("Expected header '%s: %s', but was '%s: %s'", headerName, expectedHeader, headerName, actualHeader)
    }
    return this
  }

  MockHttpServletResponseAssert hasBody(byte[] expected) {
    if (!Objects.areEqual(actual.getContentAsByteArray(), expected)) {
      this.as("body")
      throw Failures.instance().failure(info, shouldBeEqual(actual.getContentAsByteArray(), expected, info.representation()))
    }
    return this
  }

  MockHttpServletResponseAssert hasBody(String expected) {
    if (!Objects.areEqual(actual.getContentAsString(), expected)) {
      this.as("body")
      throw Failures.instance().failure(info, shouldBeEqual(actual.getContentAsString(), expected, info.representation()))
    }
    return this
  }

  MockHttpServletResponseAssert isConflict() {
    return hasStatus(409)
  }

  MockHttpServletResponseAssert isInternalServerError() {
    return hasStatus(500)
  }

  MockHttpServletResponseAssert hasCookie(String path, String name, String value, int maxAge, boolean secured, boolean httpOnly) {
    def actualCookie = actual.getCookie(name)

    Cookie expectedCookie = new Cookie(name, value)
    expectedCookie.domain = ""
    expectedCookie.path = path
    expectedCookie.maxAge = maxAge
    expectedCookie.secure = secured
    expectedCookie.httpOnly = httpOnly

    if (!EqualsBuilder.reflectionEquals(expectedCookie, actualCookie)) {
      this.as("cookie")

      throw Failures.instance().failure(info, shouldBeEqual(ReflectionToStringBuilder.toString(actualCookie, ToStringStyle.MULTI_LINE_STYLE), ReflectionToStringBuilder.toString(expectedCookie, ToStringStyle.MULTI_LINE_STYLE), info.representation()))
    }
    return this
  }
}
