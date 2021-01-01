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

package com.thoughtworks.go.http.mocks;

import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.error.ShouldContainCharSequence;
import org.assertj.core.internal.Failures;
import org.assertj.core.util.Objects;
import org.hamcrest.text.MatchesPattern;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;

import javax.servlet.http.Cookie;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import static javax.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.error.ShouldBeEqual.shouldBeEqual;
import static org.assertj.core.error.ShouldBeNullOrEmpty.shouldBeNullOrEmpty;

public class MockHttpServletResponseAssert<SELF extends MockHttpServletResponseAssert<SELF>> extends AbstractObjectAssert<SELF, MockHttpServletResponse> {

    protected MockHttpServletResponseAssert(MockHttpServletResponse actual, Class<?> selfType) {
        super(actual, selfType);
    }

    public static MockHttpServletResponseAssert assertThat(MockHttpServletResponse actual) {
        return new MockHttpServletResponseAssert(actual, MockHttpServletResponseAssert.class);
    }

    public SELF hasStatusWithMessage(int statusCode, String message) throws UnsupportedEncodingException {
        return hasStatus(statusCode).hasJsonMessage(message);
    }

    public SELF hasContentType(String mimeType) {
        String contentType = actual.getHeader("content-type");
        try {
            if (!(isNotBlank(contentType) && MimeType.valueOf(contentType).isCompatibleWith(MimeType.valueOf(mimeType)))) {
                failWithMessage("Expected content type <%s> but was <%s>", mimeType, contentType);
            }
        } catch (InvalidMimeTypeException e) {
            failWithMessage("Actual content type <%s> could not be parsed", contentType);
        }
        return myself;
    }

    public SELF hasCacheControl(String headerValue) {
        return hasHeader("Cache-Control", headerValue);
    }

    public SELF hasEtag(String expectedHeader) {
        return hasHeader("etag", expectedHeader);
    }

    public SELF hasHeader(String headerName, String expectedHeader) {
        String actualHeader = actual.getHeader(headerName);

        if (!Objects.areEqual(actualHeader, expectedHeader)) {
            failWithMessage("Expected header '%s: %s', but was '%s: %s'", headerName, expectedHeader, headerName, actualHeader);
        }
        return myself;
    }

    public SELF hasCookie(String path, String name, String value, int maxAge, boolean secured, boolean httpOnly) {
        Cookie actualCookie = actual.getCookie(name);

        Cookie expectedCookie = new Cookie(name, value);
        expectedCookie.setDomain("");
        expectedCookie.setPath(path);
        expectedCookie.setMaxAge(maxAge);
        expectedCookie.setSecure(secured);
        expectedCookie.setHttpOnly(httpOnly);

        if (!EqualsBuilder.reflectionEquals(expectedCookie, actualCookie)) {
            this.as("cookie");

            throw Failures.instance().failure(info, shouldBeEqual(ReflectionToStringBuilder.toString(actualCookie, ToStringStyle.MULTI_LINE_STYLE), ReflectionToStringBuilder.toString(expectedCookie, ToStringStyle.MULTI_LINE_STYLE), info.representation()));
        }
        return myself;
    }

    public SELF hasJsonBody(Object expected) throws UnsupportedEncodingException {
        JsonFluentAssert.assertThatJson(actual.getContentAsString()).isEqualTo(expected);
        return myself;
    }

    public SELF hasNoBody() {
        byte[] contentAsByteArray = actual.getContentAsByteArray();
        if (!(contentAsByteArray == null || contentAsByteArray.length == 0)) {
            throw Failures.instance().failure(info, shouldBeNullOrEmpty(contentAsByteArray));
        }
        return myself;
    }

    public SELF hasBody(byte[] expected) {
        if (!Objects.areEqual(actual.getContentAsByteArray(), expected)) {
            this.as("body");
            throw Failures.instance().failure(info, shouldBeEqual(actual.getContentAsByteArray(), expected, info.representation()));
        }
        return myself;
    }

    public SELF hasBody(String expected) throws UnsupportedEncodingException {
        if (!Objects.areEqual(actual.getContentAsString(), expected)) {
            this.as("body");
            throw Failures.instance().failure(info, shouldBeEqual(actual.getContentAsString(), expected, info.representation()));
        }
        return myself;
    }

    public SELF hasBodyContaining(String expectedSubstring) throws UnsupportedEncodingException {
        if (!actual.getContentAsString().contains(expectedSubstring)) {
            this.as("body containing");
            throw Failures.instance().failure(info, ShouldContainCharSequence.shouldContain(actual.getContentAsString(), expectedSubstring));
        }
        return myself;
    }

    public SELF hasJsonAttribute(String attribute, Object object) throws UnsupportedEncodingException {
        JsonFluentAssert.assertThatJson(actual.getContentAsString()).node(attribute).isEqualTo(object);
        return myself;
    }

    public SELF hasJsonMessage(String message) throws UnsupportedEncodingException {
        JsonFluentAssert.assertThatJson(actual.getContentAsString()).node("message").isEqualTo(message);
        return myself;
    }

    public SELF hasJsonMessage(Pattern regex) throws UnsupportedEncodingException {
        JsonFluentAssert.assertThatJson(actual.getContentAsString()).node("message").matches(new MatchesPattern(regex));
        return myself;
    }

    public SELF hasStatus(int statusCode) {
        if (actual.getStatus() != statusCode) {
            failWithMessage("Expected status code <%s> but was <%s>", statusCode, actual.getStatus());
        }
        return myself;
    }

    public SELF isOk() {
        return hasStatus(200);
    }

    public SELF isEntityTooLarge() {
        return hasStatus(SC_REQUEST_ENTITY_TOO_LARGE);
    }

    public SELF redirectsTo(String url) {
        if (!(actual.containsHeader("location") && actual.getHeader("location").equals(url))) {
            failWithMessage("Expected `Location` header to be set to `%s` but was `%s`", url, actual.getHeader("location"));
        }
        return hasStatus(302);
    }

    public SELF hasNoRedirectUrlSet() {
        if (actual.getRedirectedUrl() != null) {
            failWithMessage("Expected redirect url to not be set, but was `%s`", actual.getRedirectedUrl());
        }
        return myself;
    }

    public SELF doesNotContainHeader(String header) {
        if (actual.containsHeader(header)) {
            failWithMessage("Expected response to not contain header `%s: %s`", header, actual.getHeaders(header));
        }
        return myself;
    }

    public SELF containsHeader(String header) {
        if (!actual.containsHeader(header)) {
            failWithMessage("Expected response to contain header `%s`", header);
        }
        return myself;
    }

    public SELF hasNoContent() {
        return hasStatus(204);
    }

    public SELF isPreconditionFailed() {
        return hasStatus(412);
    }

    public SELF isNotModified() {
        return hasStatus(304).hasEtag(null).hasNoBody();
    }

    public SELF isNotFound() {
        return hasStatus(404);
    }

    public SELF isTooManyRequests() {
        return hasStatus(429);
    }

    public SELF isUnprocessableEntity() {
        return hasStatus(422);
    }

    public SELF isBadRequest() {
        return hasStatus(400);
    }

    public SELF isUnsupportedMediaType() {
        return hasStatus(415);
    }

    public SELF isCreated() {
        return hasStatus(201);
    }

    public SELF isConflict() {
        return hasStatus(409);
    }

    public SELF isInternalServerError() {
        return hasStatus(500);
    }

    public SELF isAccepted() {
        return hasStatus(202);
    }

    public SELF isFailedDependency() {
        return hasStatus(424);
    }

    public SELF isInsufficientStorage() {
        return hasStatus(507);
    }

    public SELF isForbidden() {
        return hasStatus(403);
    }

    public SELF isUnauthorized() {
        return hasStatus(401);
    }

    public SELF doesNotRedirect() {
        if (actual.getStatus() >= 300 && actual.getStatus() <= 399) {
            failWithMessage("Unexpected redirect status code <%s>", actual.getStatus());
        }
        return doesNotContainHeader("location");
    }
}
