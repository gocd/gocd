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

package com.thoughtworks.go.server.newsecurity.matchers;

import org.springframework.security.web.util.RequestMatcher;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

public abstract class RequestHeaderMatcherFactory {
    public static RequestMatcher headerRequestMatcher(String expectedHeaderName, Pattern valueMatcherPattern) {
        return new RequestHeaderRegexMatcher(expectedHeaderName, valueMatcherPattern);
    }


    public static RequestMatcher headerRequestMatcher(String expectedHeaderName, String headerValue) {
        return new RequestHeaderStringMatcher(expectedHeaderName, headerValue);
    }

    @Deprecated
    // TODO: Use spring's impl after upgrade
    private static class RequestHeaderStringMatcher implements RequestMatcher {
        private final String expectedHeaderValue;
        private final String expectedHeaderName;

        private RequestHeaderStringMatcher(String expectedHeaderName, String expectedHeaderValue) {
            Assert.notNull(expectedHeaderName, "headerName cannot be null.");
            Assert.notNull(expectedHeaderValue, "valueMatcherPattern cannot be null.");

            this.expectedHeaderName = expectedHeaderName;
            this.expectedHeaderValue = expectedHeaderValue;
        }


        @Override
        public boolean matches(HttpServletRequest request) {
            String actualHeaderValue = request.getHeader(expectedHeaderName);
            if (expectedHeaderValue == null) {
                return actualHeaderValue != null;
            }

            return expectedHeaderValue.equals(actualHeaderValue);
        }
    }

    private static class RequestHeaderRegexMatcher implements RequestMatcher {
        private final String expectedHeaderName;
        private final Pattern valueMatcherPattern;

        private RequestHeaderRegexMatcher(String expectedHeaderName, Pattern valueMatcherPattern) {
            Assert.notNull(expectedHeaderName, "headerName cannot be null.");
            Assert.notNull(valueMatcherPattern, "valueMatcherPattern cannot be null.");

            this.expectedHeaderName = expectedHeaderName;
            this.valueMatcherPattern = valueMatcherPattern;
        }

        public boolean matches(HttpServletRequest request) {
            String actualHeaderValue = request.getHeader(expectedHeaderName);
            if (actualHeaderValue == null) {
                return false;
            }

            return valueMatcherPattern.matcher(actualHeaderValue).matches();
        }

    }
}